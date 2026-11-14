"""Check the Advanced private-class upgrade on a NEW disposable loopback database only."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--database',required=True)
p.add_argument('--legacy-schema',required=True,help='Preserved pre25 bundle under this workspace')
p.add_argument('--container',default='scms-phases-20261027')
p.add_argument('--port',type=int,choices=(55440,54391),default=55440)
p.add_argument('--output',required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[1]
assert re.fullmatch(r'scms_schema_adv_class_[a-z0-9_]+',a.database)
assert re.fullmatch(r'scms-(?:phases-20261027|bundle-[a-z0-9-]+)',a.container)
config=json.loads(subprocess.check_output(['docker','inspect',a.container],text=True))[0]
assert config['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':str(a.port)}]
legacy=(root/a.legacy_schema).resolve();output=(root/a.output).resolve()
assert legacy.is_relative_to(root) and output.is_relative_to(root)
old=legacy.read_text(encoding='utf-8');assert '25_advanced_registration_classes_postgres.sql' not in old
migration=(root/'database/migrations/25_advanced_registration_classes_postgres.sql').read_text(encoding='utf-8')
proof={'status':'RUNNING','database':a.database,'container':a.container,'localOnly':True,'cloudExecuted':False,
       'legacySchemaSha256':hashlib.sha256(legacy.read_bytes()).hexdigest(),'migrationSha256':hashlib.sha256(migration.encode()).hexdigest(),'checks':[]}
def sql(statement,db=None,error=None):
 result=subprocess.run(['docker','exec','-i',a.container,'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','postgres','-d',db or a.database],
                       input=statement,text=True,encoding='utf-8',capture_output=True)
 if error:assert result.returncode and error in result.stderr,result.stderr[-1200:]
 else:assert result.returncode==0,result.stderr[-1200:]
 return result.stdout.strip()
def check(name,query=None,expected=None):
 if query:assert sql(query)==expected,name
 proof['checks'].append({'case':name,'result':'PASS'})
sql('CREATE DATABASE '+a.database,'postgres');sql(old)
snapshot="\n".join("SELECT md5(string_agg((to_jsonb(t)-'"+excluded+"')::text,',' ORDER BY "+key+")) FROM "+table+" t;"
                   for table,key,excluded in [('sessions','session_id','class_id'),('advanced_registrations','id','class_id'),
                       ('invoices','invoice_id','unused'),('member_packages','subscription_id','unused'),('notifications','notification_id','unused'),('system_logs','log_id','unused')])
before=sql(snapshot)
sql((root/'database/cleanup_unused_advanced_templates_postgres.sql').read_text(encoding='utf-8'),error='SCMS_ADVANCED_CLEANUP_REQUIRES_MIGRATION25')
assert sql(snapshot)==before;check('cleanup_before_model_upgrade_is_blocked_without_changes')
inside=migration.replace('BEGIN;','',1)
sql("BEGIN; ALTER TABLE advanced_registrations ADD COLUMN class_id TEXT;"+inside,error='SCMS_ADVANCED_CLASS_INCOMPATIBLE')
check('wrong_column_type_rolls_back','SELECT count(*) FROM information_schema.columns WHERE table_name=\'advanced_registrations\' AND column_name=\'class_id\'','0')
sql("""BEGIN; INSERT INTO private_session_proposals(subscription_id,proposed_by,coach_id,room_id,class_id,subject_id,session_date,start_time,end_time,request_key,request_hash,created_at,respond_by)
 SELECT s.subscription_id,s.coach_id,s.coach_id,s.room_id,s.class_id,c.subject_id,s.session_date,s.start_time,s.end_time,'migration-pending',repeat('a',64),
 clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',s.session_date+s.start_time-interval '12hours'
 FROM sessions s JOIN classes c ON c.class_id=s.class_id WHERE s.subscription_id IS NOT NULL LIMIT 1;
 """+inside,error='SCMS_ADVANCED_CLASS_PENDING_PROPOSAL')
check('explicit_pending_offer_requires_reissue_and_rolls_back','SELECT count(*) FROM private_session_proposals','0')
sql("""BEGIN; INSERT INTO training_plans(coach_id,class_id,title,content)
 SELECT s.coach_id,s.class_id,'Ambiguous legacy class plan','Local migration guard' FROM sessions s WHERE s.subscription_id IS NOT NULL LIMIT 1;
 """+inside,error='SCMS_ADVANCED_CLASS_AMBIGUOUS')
assert sql(snapshot)==before;check('ambiguous_legacy_class_plan_rolls_back_without_guessing')
sql(migration)
assert sql(snapshot)==before;check('money_credit_original_sessions_and_audit_preserved_except_exact_class_links')
check('one_class_for_each_accepted_or_invoiced_registration',"SELECT count(*)||','||count(DISTINCT class_id) FROM advanced_registrations WHERE status IN ('Accepted','Invoiced')",'8,8')
check('requested_rejected_have_no_class',"SELECT count(*) FROM advanced_registrations WHERE status IN ('Requested','Rejected') AND class_id IS NOT NULL",'0')
check('all_subscription_sessions_use_their_own_class',"SELECT count(*) FROM sessions s JOIN advanced_registrations r ON r.subscription_id=s.subscription_id WHERE s.class_id<>r.class_id",'0')
all_rows="SELECT md5(string_agg(to_jsonb(c)::text,',' ORDER BY class_id)) FROM classes c;"+snapshot
replay_before=sql(all_rows);sql(migration);sql(migration);assert sql(all_rows)==replay_before
check('replay_does_not_duplicate_classes_or_change_history')
sql("BEGIN; ALTER TABLE advanced_registrations DROP CONSTRAINT ck_advanced_registration_class_state; ALTER TABLE advanced_registrations ADD CONSTRAINT ck_advanced_registration_class_state CHECK(true);"+inside,error='SCMS_ADVANCED_CLASS_INCOMPATIBLE')
assert sql(all_rows)==replay_before;check('wrong_existing_check_definition_rolls_back')
sql("UPDATE advanced_registrations SET status='Requested' WHERE class_id IS NOT NULL",error='ck_advanced_registration_class_state')
sql("UPDATE classes SET max_capacity=2 WHERE class_id=(SELECT min(class_id) FROM advanced_registrations)",error='SCMS_ADVANCED_CLASS_IMMUTABLE')
sql("UPDATE advanced_registrations SET member_id=(SELECT max(member_id) FROM advanced_registrations) WHERE id=(SELECT min(id) FROM advanced_registrations WHERE class_id IS NOT NULL)",error='SCMS_ADVANCED_CLASS_IMMUTABLE')
check('approved_identity_and_state_cannot_be_overwritten')
sql("""UPDATE sessions SET class_id=(SELECT max(class_id) FROM advanced_registrations)
 WHERE session_id=(SELECT min(session_id) FROM sessions WHERE subscription_id IS NOT NULL)""",error='SCMS_ADVANCED_SESSION_OWNER')
check('wrong_private_class_session_rejected')
sql("""BEGIN; INSERT INTO classes(class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id)
 SELECT 'Unowned Advanced',subject_id,NULL,1,advanced_price,'Open','Advanced',12,1 FROM subjects LIMIT 1; COMMIT;""",error='SCMS_ADVANCED_CLASS_UNOWNED')
sql("DELETE FROM advanced_registrations WHERE id=(SELECT min(id) FROM advanced_registrations WHERE status='Accepted')",error='SCMS_ADVANCED_CLASS_UNOWNED')
check('new_orphan_private_class_and_deleting_accepted_owner_rejected_at_commit')
legacy_id=sql("SELECT min(c.class_id) FROM classes c LEFT JOIN advanced_registrations r ON r.class_id=c.class_id WHERE c.class_type='Advanced' AND r.id IS NULL")
sql('CREATE TABLE migration_external_class_ref(id INT PRIMARY KEY,class_id INT REFERENCES classes(class_id) ON DELETE CASCADE); INSERT INTO migration_external_class_ref VALUES(391,'+legacy_id+');')
sql((root/'database/cleanup_unused_advanced_templates_postgres.sql').read_text(encoding='utf-8'))
check('cleanup_keeps_external_cascade_child','SELECT count(*) FROM migration_external_class_ref','1')
check('cleanup_keeps_parent_referenced_by_external_cascade','SELECT count(*) FROM classes WHERE class_id='+legacy_id,'1')
sql('DROP TABLE migration_external_class_ref RESTRICT;')
sql((root/'database/cleanup_unused_advanced_templates_postgres.sql').read_text(encoding='utf-8'))
check('legacy_templates_cleaned_only_after_links_moved',"SELECT count(*)||','||count(*) FILTER(WHERE coach_id IS NULL) FROM classes WHERE class_type='Advanced'",'8,0')
assert sql(snapshot)==before;check('cleanup_keeps_all_original_business_rows')
proof['status']='PASS';output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(proof,indent=2),encoding='utf-8')
print(json.dumps({'status':'PASS','checks':len(proof['checks']),'database':a.database,'cloudExecuted':False}))
