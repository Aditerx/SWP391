"""Test migrations18/19/20 only in a newly created disposable loopback database. No .env or cloud."""
import argparse
import json
from pathlib import Path
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
args = p.parse_args()
if not re.fullmatch(r'scms_phases_[a-z0-9_]+', args.database):
    p.error('A new scms_phases_* database is required; existing databases are never reset')
container = 'scms-phases-20261027'
inspection = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]
root = Path(__file__).resolve().parents[1]


def sql(statement, database=None, error=None):
    result = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-At', '-v', 'ON_ERROR_STOP=1',
                             '-U', 'postgres', '-d', database or args.database],
                            input=statement, text=True, encoding='utf-8', capture_output=True)
    if error:
        assert result.returncode != 0 and error in result.stderr, result.stderr[-1000:]
    else:
        assert result.returncode == 0, result.stderr[-1000:]
    return result.stdout.strip()


def migration(name, prefix='', error=None):
    script = (root / 'database/migrations' / name).read_text(encoding='utf-8')
    if prefix:
        script = script.replace('BEGIN;', 'BEGIN;\n' + prefix, 1)
    return sql(script, error=error)


sql('CREATE DATABASE ' + args.database, 'postgres')
sql("""CREATE TABLE centers(center_id INT PRIMARY KEY);
INSERT INTO centers VALUES(1);
CREATE TABLE rooms(room_id INT PRIMARY KEY,room_code TEXT,room_name TEXT,center_id INT,facility_type TEXT);
CREATE TABLE subjects(subject_id INT PRIMARY KEY,slug TEXT,outdoor BOOLEAN);
INSERT INTO subjects VALUES(1,'cau-long',false);
CREATE TABLE classes(class_id INT PRIMARY KEY,center_id INT,subject_id INT,tuition_fee NUMERIC,status TEXT);
INSERT INTO classes VALUES(1,1,1,900000,'Closed'),(2,1,1,950000,'Open');
CREATE TABLE sessions(session_id INT PRIMARY KEY,class_id INT,room_id INT,status TEXT,session_date DATE,start_time TIME,end_time TIME);
CREATE TABLE system_logs(log_id SERIAL PRIMARY KEY,user_id INT,action TEXT,target_entity TEXT,target_id INT,detail TEXT,timestamp TIMESTAMP);
INSERT INTO system_logs(action,detail,timestamp) VALUES('LEGACY','Preserve this history','2020-01-01');
CREATE TABLE invoices(invoice_id INT,amount NUMERIC,payment_status TEXT);
INSERT INTO invoices VALUES(1,900000,'Paid');
CREATE TABLE member_packages(subscription_id INT,purchase_amount NUMERIC);
INSERT INTO member_packages VALUES(1,3000000);
CREATE TABLE scms_data_migration_audit(migration_key VARCHAR(100) PRIMARY KEY,captured_at TIMESTAMP,before_data TEXT,after_data TEXT);
WITH catalog AS (SELECT * FROM (VALUES
 ('TEN',2,'Outdoor'),('FOOT',2,'Outdoor'),('ARC',2,'Outdoor'),('BR',2,'Indoor'),('BC',2,'Indoor'),
 ('PICK',2,'Indoor'),('BAD',2,'Indoor'),('TT',4,'Indoor'),('SWIM',2,'Outdoor')) c(code,n,kind)),
 rows AS (SELECT row_number() OVER(ORDER BY code,k)::int id,code,k,kind FROM catalog CROSS JOIN LATERAL generate_series(1,n) k)
INSERT INTO rooms SELECT id,'C01-'||code||'-'||lpad(k::text,2,'0'),'Approved '||code||' '||k,1,kind FROM rows;
INSERT INTO scms_data_migration_audit SELECT '20261102_room_catalog_v1','2026-11-02','{}',jsonb_build_object('rooms',jsonb_agg(to_jsonb(r)))::text FROM rooms r;
INSERT INTO sessions SELECT 1,1,room_id,'Completed','2020-01-01','06:00','07:30' FROM rooms WHERE room_code='C01-BR-01';
UPDATE rooms SET room_name='Changed display name',room_code='C01-EDITED-01' WHERE room_code='C01-BAD-01';""")


def stable():
    return sql("""SELECT md5(string_agg((to_jsonb(c)-'pricing_draft')::text,',' ORDER BY class_id)) FROM classes c;
    SELECT md5(string_agg(to_jsonb(s)::text,',' ORDER BY session_id)) FROM sessions s;
    SELECT md5(string_agg(to_jsonb(i)::text,',' ORDER BY invoice_id)) FROM invoices i;
    SELECT md5(string_agg(to_jsonb(p)::text,',' ORDER BY subscription_id)) FROM member_packages p;""")


before = stable()
old_log = sql("SELECT (to_jsonb(l)-ARRAY['audit_category','center_id','scope_source'])::text FROM system_logs l WHERE log_id=1")
migration('18_audit_scope_postgres.sql')
migration('18_audit_scope_postgres.sql')
assert old_log == sql("SELECT (to_jsonb(l)-ARRAY['audit_category','center_id','scope_source'])::text FROM system_logs l WHERE log_id=1")
assert sql("SELECT count(*) FROM system_logs WHERE log_id=1 AND audit_category IS NULL AND center_id IS NULL AND scope_source IS NULL") == '1'
migration('18_audit_scope_postgres.sql', 'ALTER TABLE system_logs ALTER COLUMN scope_source TYPE VARCHAR(41);', error='Incompatible system_logs provenance')
assert sql("SELECT character_maximum_length FROM information_schema.columns WHERE table_name='system_logs' AND column_name='scope_source'") == '40'

migration('19_room_activity_postgres.sql', """INSERT INTO sessions SELECT 2,1,room_id,'Scheduled','2099-01-01','06:00','07:30'
 FROM rooms WHERE room_code='C01-BR-01';""", error='ROOM_ACTIVITY_SCHEDULE_CONFLICT')
assert sql("SELECT count(*) FROM information_schema.columns WHERE table_name='rooms' AND column_name='activity_type'") == '0'
assert before == stable(), 'Failed room migration did not fully roll back'
migration('19_room_activity_postgres.sql')
assert sql("SELECT activity_type FROM rooms WHERE room_code='C01-EDITED-01'") == 'BADMINTON'
assert sql('SELECT count(*) FROM rooms WHERE activity_type IS NOT NULL') == '20'
sql("UPDATE rooms SET activity_type='PICKLEBALL' WHERE room_code='C01-EDITED-01'")
migration('19_room_activity_postgres.sql')
assert sql("SELECT activity_type FROM rooms WHERE room_code='C01-EDITED-01'") == 'PICKLEBALL'
migration('19_room_activity_postgres.sql', 'ALTER TABLE rooms ALTER COLUMN activity_type TYPE TEXT;', error='Incompatible rooms.activity_type')

migration('20_class_publication_price_postgres.sql')
assert sql('SELECT count(*) FROM classes WHERE pricing_draft=false') == '2'
assert before == stable(), 'Upgrade modified class prices, sessions or money'
sql("INSERT INTO classes VALUES(3,1,1,975000,'Closed',true)")
migration('20_class_publication_price_postgres.sql')
assert sql('SELECT pricing_draft FROM classes WHERE class_id=3') == 't'
migration('20_class_publication_price_postgres.sql', 'ALTER TABLE classes ALTER COLUMN pricing_draft DROP NOT NULL;', error='Incompatible classes.pricing_draft')
print(json.dumps({'status': 'PASS', 'environment': 'disposable loopback PostgreSQL', 'database': args.database,
                  'verified': ['legacy audit preserved', 'incompatible schemas roll back', 'room snapshot identity survives renaming',
                               'wrong future room aborts migration', 'historical sessions and money unchanged', 'replay preserves authorized edits and drafts']}))
