"""Verify migration21 on NEW disposable local databases; never loads .env or connects to cloud."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--database',required=True)
p.add_argument('--output',required=True)
a=p.parse_args()
if not re.fullmatch(r'scms_phases_normal_migration_[a-z0-9_]+',a.database):
    p.error('A NEW scms_phases_normal_migration_* DB is required')
root=Path(__file__).resolve().parents[1]
container='scms-phases-20261027'
inspection=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':'55440'}]
script=(root/'database/migrations/21_normal_lifecycle_postgres.sql').read_text(encoding='utf-8')
checks=[]
def sql(statement,database=None,error=None):
    r=subprocess.run(['docker','exec','-i',container,'psql','-X','-At','-v','ON_ERROR_STOP=1','-U','postgres','-d',database or a.database],input=statement,text=True,encoding='utf-8',capture_output=True)
    if error:
        assert r.returncode!=0 and error in r.stderr,r.stderr[-1400:]
    else:
        assert r.returncode==0,r.stderr[-1400:]
    return r.stdout.strip()
sql('CREATE DATABASE '+a.database,'postgres')
# Minimal OLD baseline exposes the migration's compatibility behavior independently of the new generator.
sql('''CREATE TABLE users(user_id INT PRIMARY KEY); INSERT INTO users VALUES(391);
CREATE TABLE classes(class_id INT PRIMARY KEY,max_capacity INT,status TEXT,tuition_fee NUMERIC);
INSERT INTO classes VALUES(1,4,'Open',900000),(2,4,'Closed',950000);
CREATE TABLE sessions(session_id INT PRIMARY KEY,class_id INT REFERENCES classes(class_id));
INSERT INTO sessions VALUES(11,1);
CREATE TABLE invoices(invoice_id INT PRIMARY KEY,class_id INT,amount NUMERIC,payment_status TEXT);
INSERT INTO invoices VALUES(5,1,900000,'Paid');''')
before=sql("SELECT md5(row_to_json(i)::text) FROM invoices i; SELECT md5(row_to_json(s)::text) FROM sessions s;")
sql(script)
assert sql('SELECT count(*) FROM classes WHERE min_capacity IS NULL AND opening_confirmed_at IS NULL AND opening_confirmed_by IS NULL')=='2'
checks.append('legacy minimum/confirmation remain unknown; no fabricated opening')
sql("UPDATE classes SET min_capacity=3,opening_confirmed_by=391,opening_confirmed_at='2026-11-10 08:00',opening_reason='Reviewed locally',postponement_count=1,lifecycle_version=7 WHERE class_id=1")
metadata=sql('SELECT md5(row_to_json(c)::text) FROM classes c ORDER BY class_id')
sql(script);sql(script)
assert metadata==sql('SELECT md5(row_to_json(c)::text) FROM classes c ORDER BY class_id')
assert before==sql("SELECT md5(row_to_json(i)::text) FROM invoices i; SELECT md5(row_to_json(s)::text) FROM sessions s;")
checks.append('replay preserves class decisions, Paid receipts and sessions')
sql('BEGIN; UPDATE classes SET postponement_count=2 WHERE class_id=1; COMMIT;',error='ck_normal_lifecycle_postponement')
sql("BEGIN; UPDATE classes SET opening_confirmed_by=999 WHERE class_id=1; COMMIT;",error='foreign key')
assert metadata==sql('SELECT md5(row_to_json(c)::text) FROM classes c ORDER BY class_id')
checks.append('postponement and actor constraints reject invalid data without changing history')
bad='BEGIN; ALTER TABLE classes DROP CONSTRAINT ck_normal_lifecycle_capacity; ALTER TABLE classes ALTER COLUMN min_capacity TYPE TEXT;\n'+script.replace('BEGIN;','',1)
sql(bad,error='Incompatible Normal lifecycle columns')
assert metadata==sql('SELECT md5(row_to_json(c)::text) FROM classes c ORDER BY class_id')
sql('BEGIN; ALTER TABLE classes ALTER COLUMN lifecycle_version DROP NOT NULL;\n'+script.replace('BEGIN;','',1),error='Incompatible Normal lifecycle defaults/nullability')
assert metadata==sql('SELECT md5(row_to_json(c)::text) FROM classes c ORDER BY class_id')
checks.append('incompatible column/default/nullability preflight rolls back all preceding DDL')
result={'status':'PASS','database':a.database,'localOnly':True,'envLoaded':False,'cloudMutation':False,'migrationSha256':hashlib.sha256(script.encode()).hexdigest(),'checks':checks}
output=(root/a.output).resolve();assert output.is_relative_to(root)
output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'status':'PASS','checks':len(checks),'cloudMutation':False}))
