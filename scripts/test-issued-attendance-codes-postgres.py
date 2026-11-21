"""Validate migration26 on disposable loopback PostgreSQL only; never loads .env.

Clone a consumed local fixture to prove history is preserved. Error details may
contain private row values, so only expected error identifiers are recorded.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess

ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--database',required=True)
p.add_argument('--template',required=True)
p.add_argument('--output',required=True)
a=p.parse_args()
for value in (a.database,a.template):
    if not re.fullmatch(r'scms_phases_[a-z0-9_]+',value): p.error('Disposable scms_phases_* database only')
container='scms-phases-20261027'
inspection=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp']==[{'HostIp':'127.0.0.1','HostPort':'55440'}]
out=(ROOT/a.output).resolve()
assert out.is_relative_to((ROOT/'artifacts/runs').resolve()) and not out.exists()
cases=[]
def sql(body,db=None,error=None):
    r=subprocess.run(['docker','exec','-i',container,'psql','-X','-q','-A','-t','-v','ON_ERROR_STOP=1','-U','postgres','-d',db or a.database],input=body,text=True,encoding='utf-8',capture_output=True)
    if error:
        assert r.returncode and error in r.stderr,'Expected migration/constraint guard did not fire'
        return ''
    assert r.returncode==0,'Local SQL failed; private row details were intentionally withheld'
    return r.stdout.strip()
def check(name,condition=True):
    assert condition,name
    cases.append({'case':name,'status':'PASS'})
sql('CREATE DATABASE '+a.database+' TEMPLATE '+a.template+';','postgres')
migration=(ROOT/'database/migrations/26_issued_attendance_codes_postgres.sql').read_text(encoding='utf-8')
body=re.sub(r'^BEGIN;\s*','',migration,flags=re.M) # Retain the surrounding test transaction for negative guards.
body=re.sub(r'^COMMIT;\s*','',body,flags=re.M)
new=['checkin_code_nonce','checkin_code_issued_at','checkin_code_expires_at','checkin_code_used_at']
def snapshot():
    result={}
    for table in ('users','members','invoices','member_packages','attendances','sessions','advanced_registrations','private_session_proposals'):
        omitted="-ARRAY["+','.join("'"+x+"'" for x in new)+"]" if table=='members' else ''
        expression='(to_jsonb(t)'+omitted+')::text'
        result[table]=sql('SELECT count(*)::text||\':\'||md5(coalesce(string_agg('+expression+",E'\\n' ORDER BY "+expression+"),'')) FROM public."+table+' t;')
    result['membersRls']=sql("SELECT relrowsecurity::text||':'||relforcerowsecurity::text FROM pg_class WHERE oid='public.members'::regclass;")
    return result
before=snapshot()
sql(migration);check('first upgrade preserves Member/secret/history/invoice/credit/session rows and RLS',before==snapshot())
check('legacy Members receive no fabricated issuance',sql('SELECT count(*) FROM members WHERE '+' OR '.join(x+' IS NOT NULL' for x in new)+';')=='0')
sql(migration);check('replay preserves all existing state',before==snapshot())
valid="checkin_code_nonce=70001,checkin_code_issued_at=TIMESTAMP '2026-11-19 20:40:17',checkin_code_expires_at=TIMESTAMP '2026-11-19 20:45:17'"
selector='user_id=(SELECT min(user_id) FROM members WHERE checkin_secret IS NOT NULL)'
assert int(sql('SELECT count(*) FROM members WHERE checkin_secret IS NOT NULL;'))>0,'Use a local QR fixture with an existing secret'
sql('BEGIN; UPDATE members SET '+valid+' WHERE '+selector+'; ROLLBACK;');check('valid 300-second issuance is accepted')
sql('BEGIN; UPDATE members SET '+valid.replace('20:45:17','20:45:16')+' WHERE '+selector+';',error='ck_members_issued_checkin_code');check('shortened/malformed TTL is rejected without writes')
sql('BEGIN; UPDATE members SET '+valid+",checkin_code_used_at=TIMESTAMP '2026-11-19 20:45:17' WHERE "+selector+';',error='ck_members_issued_checkin_code');check('use at the expiry boundary is rejected without writes')
check('failed data mutations preserve the complete original rows',before==snapshot())

shape=a.database+'_shape'
sql('CREATE DATABASE '+shape+';','postgres')
sql('CREATE TABLE members(user_id INTEGER PRIMARY KEY,checkin_secret VARCHAR(64),checkin_last_step BIGINT);',shape)
sql(migration,shape)
for name,alter in [
    ('wrong nonce type','ALTER TABLE members ALTER COLUMN checkin_code_nonce TYPE TEXT USING checkin_code_nonce::text;'),
    ('unexpected nonce default','ALTER TABLE members ALTER COLUMN checkin_code_nonce SET DEFAULT 7;'),
    ('unexpected NOT NULL','ALTER TABLE members ALTER COLUMN checkin_code_nonce SET NOT NULL;'),
    ('colliding constraint definition','ALTER TABLE members DROP CONSTRAINT ck_members_issued_checkin_code; ALTER TABLE members ADD CONSTRAINT ck_members_issued_checkin_code CHECK(true);')]:
    sql('BEGIN;'+alter+body,shape,error='SCMS_ISSUED_CODES_SCHEMA')
    sql(migration,shape)
    check(name+' is rejected and rollback leaves replay compatible')
result={'status':'PASS_LOCAL_POSTGRES_MIGRATION26','database':a.database,'shapeDatabase':shape,
        'environment':'Docker loopback 127.0.0.1:55440; no .env/cloud/provider',
        'beforeAfterOriginalDataHashes':before,'cases':cases,'secretsNoncesCodesExported':False}
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'status':result['status'],'cases':len(cases)}))
