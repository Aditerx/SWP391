"""Owner02/11 migration proof on fresh disposable local PostgreSQL only; never reads .env."""
from pathlib import Path
import argparse, datetime, hashlib, json, re, subprocess

root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--prefix',required=True)
p.add_argument('--output',required=True)
p.add_argument('--root-bundles',action='store_true',help='Exercise the exact two root SQL files, including order guard and diagnostic wrappers')
a=p.parse_args()
if not re.fullmatch(r'scms_phases_room_[a-z0-9_]+',a.prefix): p.error('Disposable scms_phases_room_* prefix required')
container='scms-phases-20261027'
info=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
assert info['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':'55440'}]

def sql(db,content,error=None,error_detail=None):
 r=subprocess.run(['docker','exec','-i',container,'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','postgres','-d',db],input=content,text=True,encoding='utf-8',capture_output=True)
 if error:
  assert r.returncode and error in r.stderr, r.stderr[-1600:]
  if error_detail: assert error_detail in r.stderr, r.stderr[-1600:]
 elif r.returncode: raise RuntimeError(r.stderr[-2400:])
 return r.stderr if error else r.stdout.strip()

def create(suffix):
 db=a.prefix+'_'+suffix
 sql('postgres','CREATE DATABASE '+db+';')
 return db

def snap(db,tables):
 def expression(table):
  return "(to_jsonb(t)-ARRAY['absence_notice_id','superseded_at'])" if table=='attendances' else 'to_jsonb(t)'
 return {table:sql(db,f"SELECT md5(coalesce(string_agg({expression(table)}::text,',' ORDER BY {expression(table)}::text),'')) FROM {table} t;") for table in tables}

m13=(root/'database/migrations/13_room_catalog_postgres.sql').read_text(encoding='utf-8')
unapproved_capacity_guard=m13  # Generic catalog migration must never resize classes.
m14=(root/'database/migrations/14_absence_attendance_postgres.sql').read_text(encoding='utf-8')
source_paths=['schema_postgres.sql','database/schema_postgres.sql','database/migrations/13_room_catalog_postgres.sql','database/migrations/14_absence_attendance_postgres.sql']
if a.root_bundles:
 for name,canonical in [('0211-1.sql',m13),('0212-2.sql',m14)]:
  assert canonical.replace('BEGIN;\n','',1).rsplit('COMMIT;',1)[0].rstrip() in (root/name).read_text(encoding='utf-8')
 m13=(root/'0211-1.sql').read_text(encoding='utf-8');m14=(root/'0212-2.sql').read_text(encoding='utf-8')
 source_paths+=['0211-1.sql','0212-2.sql']
hashes={name:hashlib.sha256((root/name).read_bytes()).hexdigest() for name in source_paths}
checks={}
fresh=create('fresh')
root_schema=(root/'schema_postgres.sql').read_text(encoding='utf-8')
sql(fresh,root_schema)
assert sql(fresh,"SELECT count(*) FROM rooms WHERE status='Available' AND room_code LIKE 'C01-%';")=='20'
before=snap(fresh,['users','rooms','invoices','attendances'])
sql(fresh,root_schema); assert before==snap(fresh,before)
sql(fresh,"UPDATE rooms SET capacity=3,status='Maintenance' WHERE room_code='C01-TEN-02';")
changed=snap(fresh,['rooms']);sql(fresh,root_schema);assert changed==snap(fresh,changed)
sql(fresh,"INSERT INTO rooms(room_name,capacity,center_id) VALUES('Old writer without code',4,1);")
assert sql(fresh,"SELECT room_code LIKE 'C01-LEGACY-%' FROM rooms WHERE room_name='Old writer without code';")=='t'
sql(fresh,"INSERT INTO rooms(room_code,room_name,capacity,center_id) VALUES('C02-QA-01','Wrong center',4,1);",error='chk_room_code')
sql(fresh,"INSERT INTO rooms(room_code,room_name,capacity,center_id) VALUES('C01-TEN-01','Duplicate',4,1);",error='uq_rooms_code')
checks['rootFreshReplayAndOldWriter']='PASS'

demo=create('demo')
sql(demo,(root/'database/schema_postgres.sql').read_text(encoding='utf-8'))
assert sql(demo,'SELECT count(*) FROM rooms;')=='20'
assert sql(demo,"SELECT count(*) FROM sessions s JOIN rooms r USING(room_id) JOIN classes c USING(class_id) WHERE c.max_capacity>r.capacity;")=='0'
assert sql(demo,"SELECT count(*) FROM sessions s JOIN classes c USING(class_id) JOIN subjects su USING(subject_id) JOIN rooms r USING(room_id) WHERE su.slug='bong-chuyen' AND r.room_code NOT LIKE 'C01-BC-%';")=='0'
checks['rebuiltTwentyRoomsCapacityAndVolleyball']='PASS'
sql(demo,(root/'database/schema_postgres.sql').read_text(encoding='utf-8'))
assert sql(demo,'SELECT count(*) FROM rooms;')=='20'
assert sql(demo,"SELECT count(*) FROM pg_constraint WHERE conrelid='attendances'::regclass AND conname='fk_attendance_absence_notice';")=='1'
checks['rebuiltReplayForeignKeySafety']='PASS'

upgrade=create('upgrade')
baseline='ec2325df07df053257df09698857b27e36489820'
old=subprocess.check_output(['git','show',baseline+':database/schema_postgres.sql'],cwd=root).decode('utf-8')
assert '20261102_room_catalog_v1' not in old, 'Baseline must precede the room migration'
sql(upgrade,old)
# Owner's actual inventory has the old eight plus room9 Tennis02.
assert sql(upgrade,"INSERT INTO rooms(room_name,location,capacity,status,facility_type,center_id) VALUES('Sân Tennis 02','Khu A – Ngoài trời',4,'Available','Outdoor',1) RETURNING room_id;").splitlines()[0]=='9'
# Two physical tennis courts may legitimately host different Coaches concurrently.
sql(upgrade,"""WITH course AS (
 INSERT INTO classes(class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id)
 VALUES('QA Tennis02 parallel course',1,13,4,2500000,'Ongoing','Normal',12,1) RETURNING class_id
)
INSERT INTO sessions(class_id,room_id,coach_id,session_date,start_time,end_time,status)
SELECT course.class_id,9,13,day::date,TIME '06:00',TIME '07:30','Scheduled' FROM course
CROSS JOIN LATERAL (
 SELECT day FROM generate_series(
  (SELECT min(session_date) FROM sessions WHERE class_id=10 AND status='Scheduled' AND session_date>(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date),
  (clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+100,INTERVAL '1 day') day
 WHERE extract(isodow FROM day) IN(1,3,5) AND NOT EXISTS(SELECT 1 FROM holidays h WHERE h.holiday_date=day::date AND h.is_active)
 ORDER BY day LIMIT 12
) calendar;
""")
assert sql(upgrade,"SELECT count(*) FROM sessions WHERE room_id=9;")=='12'
protected=['invoices','member_packages','class_enrollments','attendances','training_results']
before=snap(upgrade,protected+['rooms','sessions'])
if a.root_bundles:
 sql(upgrade,m14,error='RUN_0211_1_FIRST');assert before==snap(upgrade,before)
 checks['rootFileOrderGuard']='PASS'
failure=sql(upgrade,unapproved_capacity_guard,error='ROOM_CATALOG_SCHEDULE_REVIEW',error_detail='CLASS_CAPACITY_EXCEEDS_ROOM')
diagnostics=json.loads(next(line.split('DETAIL:',1)[1].strip() for line in failure.splitlines() if line.startswith('DETAIL:')))
assert diagnostics and all({'session_id','class_id','max_capacity','target_capacity','held_seats','reasons'}<=set(item) for item in diagnostics)
assert any(item['max_capacity']>item['target_capacity'] and item['held_seats']<=item['target_capacity'] for item in diagnostics)
checks['scheduleReviewIdentifiesClassCapacityVersusActualSeats']='PASS'
assert before==snap(upgrade,before), 'Failed migration must roll back all schema/data edits'
assert sql(upgrade,"SELECT count(*) FROM information_schema.columns WHERE table_name='rooms' AND column_name='room_code';")=='0'
checks['capacityGuardAtomicRollback']='PASS'
# Fixture-only reconciliation, not part of the production migration. Do not shrink
# live classes automatically: Owner must resolve actual registrations and schedules.
sql(upgrade,"""UPDATE classes c SET max_capacity=least(c.max_capacity,v.capacity)
 FROM subjects su JOIN (VALUES('quan-vot-tennis',4),('boi-loi',10),('pickleball',4),('ban-cung',5),('bong-ro',8),('bong-chuyen',8),('bong-da',15),('cau-long',4),('bong-ban',2)) v(slug,capacity) ON v.slug=su.slug
 WHERE c.subject_id=su.subject_id AND c.center_id=1;
""")
sql(upgrade,"UPDATE classes SET max_capacity=NULL WHERE class_name='QA Tennis02 parallel course';")
failure=sql(upgrade,m13,error='ROOM_CATALOG_SCHEDULE_REVIEW',error_detail='MISSING_CLASS_CAPACITY')
missing=json.loads(next(line.split('DETAIL:',1)[1].strip() for line in failure.splitlines() if line.startswith('DETAIL:')))
assert any(item['max_capacity'] is None and 'MISSING_CLASS_CAPACITY' in item['reasons'] for item in missing)
sql(upgrade,"UPDATE classes SET max_capacity=4 WHERE class_name='QA Tennis02 parallel course';")
checks['scheduleReviewIdentifiesMissingCapacity']='PASS'
sql(upgrade,"INSERT INTO rooms(room_name,capacity,center_id) VALUES('Unreviewed custom room',4,1);")
sql(upgrade,m13,error='ROOM_CATALOG_CUSTOM_ROOM',error_detail='Unreviewed custom room')
sql(upgrade,"DELETE FROM rooms WHERE room_name='Unreviewed custom room';")
checks['customRoomGuard']='PASS'
sql(upgrade,"""CREATE TABLE qa_room_restore AS SELECT session_id,session_date,start_time,end_time FROM sessions
 WHERE class_id=10 AND status='Scheduled' AND session_date>(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date ORDER BY session_date LIMIT 2;
 UPDATE sessions s SET session_date=a.session_date,start_time=a.start_time,end_time=a.end_time
 FROM qa_room_restore a WHERE a.session_id=(SELECT min(session_id) FROM qa_room_restore) AND s.session_id=(SELECT max(session_id) FROM qa_room_restore);""")
sql(upgrade,m13,error='ROOM_CATALOG_OVERLAP')
sql(upgrade,"UPDATE sessions s SET session_date=b.session_date,start_time=b.start_time,end_time=b.end_time FROM qa_room_restore b WHERE s.session_id=b.session_id;")
checks['targetOverlapGuard']='PASS'
before=snap(upgrade,protected)
past=sql(upgrade,"SELECT md5(string_agg(to_jsonb(s)::text,',' ORDER BY session_id)) FROM sessions s WHERE status<>'Scheduled' OR session_date<(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;")
sql(upgrade,m13);sql(upgrade,m14)
assert before==snap(upgrade,protected)
assert past==sql(upgrade,"SELECT md5(string_agg(to_jsonb(s)::text,',' ORDER BY session_id)) FROM sessions s WHERE status<>'Scheduled' OR session_date<(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;")
assert sql(upgrade,"SELECT count(*) FROM rooms WHERE status='Available';")=='20'
assert sql(upgrade,"SELECT count(*) FROM rooms WHERE room_code LIKE 'C01-LEGACY-%' AND status='Closed';")=='9'
assert sql(upgrade,"SELECT count(*) FROM sessions s JOIN classes c USING(class_id) JOIN rooms r USING(room_id) WHERE c.class_name='QA Tennis02 parallel course' AND r.room_code='C01-TEN-02';")=='12'
assert sql(upgrade,"SELECT count(*) FROM rooms WHERE room_id=9 AND room_name='Sân Tennis 02' AND status='Closed';")=='1'
checks['ownerNineRoomInventoryAndTennis02Mapping']='PASS'
assert sql(upgrade,"SELECT count(*) FROM sessions s JOIN rooms r USING(room_id) WHERE s.status='Scheduled' AND s.session_date>=(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AND r.status<>'Available';")=='0'
after=snap(upgrade,protected+['rooms','sessions','scms_data_migration_audit'])
sql(upgrade,m13);sql(upgrade,m14);assert after==snap(upgrade,after)
checks['upgradePreservesHistoryAndReplay']='PASS'
sql(upgrade,"""INSERT INTO attendances(session_id,member_id,recorded_by,state,absence_notice_id,superseded_at)
 SELECT session_id,member_id,member_id,'Absent',id,clock_timestamp() FROM session_absence_notices LIMIT 1;""")
# Existing current attendance + archived version may coexist; another current row may not.
sql(upgrade,"""INSERT INTO attendances(session_id,member_id,state) SELECT session_id,member_id,'Absent' FROM session_absence_notices LIMIT 1;""",error='ux_attendance_session_member')
checks['supersededAttendanceUniqueness']='PASS'
sql(upgrade,"INSERT INTO attendances(member_id,state,absence_notice_id) SELECT min(user_id),'Absent',2147483647 FROM members;",error='fk_attendance_absence_notice')
checks['noticeForeignKeyGuard']='PASS'
assert hashes=={name:hashlib.sha256((root/name).read_bytes()).hexdigest() for name in source_paths}, 'Sources changed during migration proof; rerun'
report=dict(checkedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(),localOnly=True,rootBundles=a.root_bundles,databases=[fresh,demo,upgrade],checks=checks,sha256=hashes,scheduleDiagnosticSample=diagnostics[0])
out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(localOnly=True,checks=checks,output=str(out)),indent=2))
