"""Verify the explicitly approved seven-class update and exact root bundles locally only."""
from pathlib import Path
import argparse, datetime, hashlib, json, re, subprocess

root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--prefix',required=True);p.add_argument('--output',required=True)
a=p.parse_args()
if not re.fullmatch(r'scms_phases_owner_caps_[a-z0-9_]+',a.prefix):p.error('Fresh local scms_phases_owner_caps_* prefix required')
container='scms-phases-20261027'
info=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
assert info['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':'55440'}]
sources=['0211-1.sql','0212-2.sql','database/migrations/13_owner_class_capacity_20261102_postgres.sql',
 'database/migrations/13_room_catalog_postgres.sql','database/migrations/14_absence_attendance_postgres.sql',
 'schema_postgres.sql','database/schema_postgres.sql','scripts/test-owner-class-capacity-postgres.py']
hashes={n:hashlib.sha256((root/n).read_bytes()).hexdigest() for n in sources}
one=(root/'0211-1.sql').read_text(encoding='utf-8');two=(root/'0212-2.sql').read_text(encoding='utf-8')
for n in sources[2:4]:assert (root/n).read_text(encoding='utf-8').replace('BEGIN;\n','',1).rsplit('COMMIT;',1)[0].rstrip() in one
assert one.count('\nBEGIN;\n')==one.count('\nCOMMIT;\n')==1
baseline='ec2325df07df053257df09698857b27e36489820'
approved={11:(15,10),12:(12,4),13:(10,5),14:(15,8),16:(15,8),17:(15,4),18:(15,2)}
marker='20261102_owner_normal_class_capacity_v1'
checks={}

def sql(db,text,error=None,detail=None):
 r=subprocess.run(['docker','exec','-i',container,'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','postgres','-d',db],input=text,text=True,encoding='utf-8',capture_output=True)
 if error:
  assert r.returncode and error in r.stderr,r.stderr[-2000:]
  if detail:assert detail in r.stderr,r.stderr[-2000:]
 elif r.returncode:raise RuntimeError(r.stderr[-2400:])
 return r.stderr if error else r.stdout.strip()

def snapshot(db,tables,exclude_max=False):
 parts=[]
 for t in tables:
  expr="(to_jsonb(t)-ARRAY['absence_notice_id','superseded_at'])" if t=='attendances' else "(to_jsonb(t)-'max_capacity')" if t=='classes' and exclude_max else 'to_jsonb(t)'
  parts.append(f"'{t}',(SELECT md5(coalesce(string_agg({expr}::text,',' ORDER BY {expr}::text),'')) FROM {t} t)")
 return json.loads(sql(db,'SELECT jsonb_build_object('+','.join(parts)+')::text;'))

def caps(db):return {int(x.split('|')[0]):int(x.split('|')[1]) for x in sql(db,'SELECT class_id,max_capacity FROM classes ORDER BY class_id;').splitlines()}

base=a.prefix+'_base'
sql('postgres','CREATE DATABASE '+base+';')
sql(base,subprocess.check_output(['git','show',baseline+':database/schema_postgres.sql'],cwd=root).decode('utf-8'))
sql(base,"INSERT INTO rooms(room_name,capacity,status,facility_type,center_id) VALUES('Sân Tennis 02',4,'Available','Outdoor',1);")
assert all(caps(base)[i]==v[0] for i,v in approved.items())
protected=['invoices','member_packages','class_enrollments','attendances','training_results']
all_tables=protected+['classes','rooms','sessions','scms_data_migration_audit']

def clone(suffix):
 db=a.prefix+'_'+suffix;sql('postgres','CREATE DATABASE '+db+' TEMPLATE '+base+';');return db

bad=clone('guards')
before=snapshot(bad,all_tables)
sql(bad,two,error='RUN_0211_1_FIRST');assert before==snapshot(bad,all_tables)
checks['file2OrderGuard']='PASS'
for label,prepare,restore,reason in [
 ('wrongSubject',"UPDATE classes SET subject_id=1 WHERE class_id=11;","UPDATE classes SET subject_id=2 WHERE class_id=11;",'WRONG_SUBJECT'),
 ('staleLimit',"UPDATE classes SET max_capacity=12 WHERE class_id=11;","UPDATE classes SET max_capacity=15 WHERE class_id=11;",'CLASS_CAPACITY_CHANGED_SINCE_APPROVAL'),
 ('wrongType',"UPDATE classes SET class_type='Advanced',max_capacity=1 WHERE class_id=11;","UPDATE classes SET class_type='Normal',max_capacity=15 WHERE class_id=11;",'WRONG_CLASS_TYPE')]:
 sql(bad,prepare);before=snapshot(bad,all_tables)
 sql(bad,one,error='OWNER_CLASS_CAPACITY_REVIEW',detail=reason)
 assert before==snapshot(bad,all_tables),label
 sql(bad,restore);checks[label+'AbortsWholeFile']='PASS'
sql(bad,"INSERT INTO centers(center_id,center_name) VALUES(2,'Owner-capacity scope test'); UPDATE classes SET center_id=2 WHERE class_id=11;")
before=snapshot(bad,all_tables);sql(bad,one,error='OWNER_CLASS_CAPACITY_REVIEW',detail='WRONG_CENTER')
assert before==snapshot(bad,all_tables)
sql(bad,'UPDATE classes SET center_id=1 WHERE class_id=11;');checks['wrongCenterAbortsWholeFile']='PASS'

held=clone('held')
sql(held,"""INSERT INTO class_enrollments(member_id,class_id,status)
 SELECT m.user_id,18,'Pending' FROM members m WHERE NOT EXISTS(
 SELECT 1 FROM class_enrollments e WHERE e.class_id=18 AND e.member_id=m.user_id) ORDER BY m.user_id LIMIT 2;""")
assert sql(held,"SELECT count(*) FROM class_enrollments WHERE class_id=18 AND status IN('Registered','Pending');")=='3'
before=snapshot(held,all_tables);sql(held,one,error='OWNER_CLASS_CAPACITY_REVIEW',detail='HELD_SEATS_EXCEED_APPROVED_CAPACITY')
assert before==snapshot(held,all_tables);checks['excessHeldSeatsNotDroppedOrResized']='PASS'

late=clone('late')
sql(late,"INSERT INTO rooms(room_name,capacity,status,facility_type,center_id) VALUES('QA unreviewed extra resource',4,'Available','Indoor',1);")
before=snapshot(late,all_tables)
sql(late,one,error='ROOM_CATALOG_CUSTOM_ROOM',detail='QA unreviewed extra resource')
assert before==snapshot(late,all_tables)
assert sql(late,f"SELECT count(*) FROM scms_data_migration_audit WHERE migration_key='{marker}';")=='0'
checks['laterRoomFailureRollsBackAllSevenLimitsAndAudit']='PASS'

success=clone('success')
before=snapshot(success,protected+['classes'],exclude_max=True);old_caps=caps(success)
past_query="SELECT md5(string_agg(to_jsonb(s)::text,',' ORDER BY session_id)) FROM sessions s WHERE status<>'Scheduled' OR session_date<(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;"
past=sql(success,past_query)
sql(success,one);sql(success,two)
assert before==snapshot(success,protected+['classes'],exclude_max=True)
actual=caps(success)
assert all(actual[i]==v[1] for i,v in approved.items())
assert all(actual[i]==v for i,v in old_caps.items() if i not in approved)
assert past==sql(success,past_query)
assert sql(success,"SELECT count(*) FROM rooms WHERE status='Available';")=='20'
assert sql(success,"SELECT count(*) FROM rooms WHERE status='Closed' AND room_code LIKE 'C01-LEGACY-%';")=='9'
assert sql(success,"SELECT count(*) FROM sessions s JOIN classes c USING(class_id) JOIN rooms r USING(room_id) WHERE s.status='Scheduled' AND NOT s.center_invalidated AND c.max_capacity>r.capacity;")=='0'
checks['exactSevenLimitsAndUnrelatedClassFieldsPreserved']='PASS'
checks['moneyRightsEnrollmentsAttendanceAndPastSessionsPreserved']='PASS'
audit=json.loads(sql(success,f"SELECT jsonb_build_object('before',before_data::jsonb,'after',after_data::jsonb)::text FROM scms_data_migration_audit WHERE migration_key='{marker}';"))
assert {r['class_id']:r['max_capacity'] for r in audit['before']['classes']}=={i:v[0] for i,v in approved.items()}
assert {r['class_id']:r['max_capacity'] for r in audit['after']['classes']}=={i:v[1] for i,v in approved.items()}
assert audit['before']['held_seats']==audit['after']['held_seats']
checks['approvalBeforeAfterAudit']='PASS'
checks['bothRootFilesCommitAndPostflight']='PASS'
sql(success,"UPDATE classes SET max_capacity=9 WHERE class_id=11;")
before=snapshot(success,all_tables);sql(success,one);sql(success,two)
assert before==snapshot(success,all_tables), 'Replay must preserve later authorized metadata changes'
checks['replayDoesNotResetLaterCapacityOrDuplicateAudit']='PASS'

partial=clone('partial')
sql(partial,'UPDATE classes SET max_capacity=10 WHERE class_id=11;')
sql(partial,one)
assert all(caps(partial)[i]==v[1] for i,v in approved.items())
checks['alreadyAlignedSubsetAccepted']='PASS'
assert hashes=={n:hashlib.sha256((root/n).read_bytes()).hexdigest() for n in sources},'Sources changed during proof'
report={'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'fixtureBaseline':baseline,'localOnly':True,
 'environment':'PostgreSQL17 127.0.0.1:55440, disposable clones of reviewed nine-room fixture',
 'databases':[base,bad,held,late,success,partial],'checks':checks,'sha256':hashes,
 'approvedLimits':[{'class_id':i,'old':v[0],'new':v[1]} for i,v in approved.items()],
 'auditSample':audit,'cloudAccess':False,'backendTestsExecuted':False}
out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'localOnly':True,'checks':checks,'output':str(out)},indent=2))
