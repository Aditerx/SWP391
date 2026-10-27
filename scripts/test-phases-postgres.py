"""Provision and verify only disposable local PostgreSQL. Never loads .env.
Usage: python scripts/test-phases-postgres.py --database scms_phases_p1
Container: scms-phases-20261027, port 127.0.0.1:55440.
"""
from pathlib import Path
import subprocess, argparse, re, json

root = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
p.add_argument('--minimal-fixture',action='store_true',help='DDL, roles are created by HTTP test fixtures; no demo Paid/Active rows')
args = p.parse_args()
if not re.fullmatch(r'scms_phases_[a-z0-9_]+', args.database):
    p.error('Only a new scms_phases_* local database is allowed')
container = 'scms-phases-20261027'
inspection = json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
bindings = inspection['HostConfig']['PortBindings']['5432/tcp']
assert bindings == [{'HostIp':'127.0.0.1','HostPort':'55440'}], 'Wrong disposable database target'

def sql(text, database=None, success=True, expected_error=None):
    r = subprocess.run(['docker','exec','-i',container,'psql','-X','-v','ON_ERROR_STOP=1','-U','postgres','-d',database or args.database],
                       input=text,text=True,encoding='utf-8',capture_output=True)
    if (r.returncode == 0) != success:
        raise RuntimeError(r.stdout+r.stderr)
    if expected_error and expected_error not in r.stderr:
        raise AssertionError(r.stderr)
    return r.stdout

sql('CREATE DATABASE '+args.database+';', 'postgres')
schema=(root/'database/schema_postgres.sql').read_text(encoding='utf-8')
if args.minimal_fixture:
    catalog_start=schema.index('INSERT INTO subjects ')
    catalog=schema[catalog_start:schema.index(';',catalog_start)+1]
    schema=schema[:schema.index('INSERT INTO roles ')]+catalog
sql(schema)
def snapshots():
    return sql("""SELECT md5(string_agg((to_jsonb(i)-ARRAY['reconciliation_required','cash_note','cash_confirmation_reversed','resolution_status'])::text,',' ORDER BY invoice_id)) FROM invoices i;
    SELECT md5(string_agg((to_jsonb(c)-'eligibility_subscription_id')::text,',' ORDER BY enrollment_id)) FROM class_enrollments c;
    SELECT md5(string_agg(json_build_array(subscription_id,member_id,package_id,start_date,end_date,remaining_credit,validity_days,status)::text,',' ORDER BY subscription_id)) FROM member_packages;""")

before=snapshots()
for filename in ['06_schedule_safety_postgres.sql','07_session_subscription_postgres.sql',
                 '08_advanced_entitlements_postgres.sql','09_approved_catalog_postgres.sql',
                 '10_session_incidents_postgres.sql','11_holiday_history_postgres.sql','12_payment_resolutions_postgres.sql']:
    migration=(root/'database/migrations'/filename).read_text(encoding='utf-8')
    sql(migration)
    sql(migration)
assert before == snapshots(), 'Upgrade modified existing invoices or enrollments'
assert '9' in sql('SELECT count(*) FROM subjects WHERE normal_price>0 AND advanced_price>0;')
# A catalog migration rerun must not overwrite a subsequent authorized price update.
sql("UPDATE subjects SET advanced_price=advanced_price+1 WHERE slug='cau-long';")
sql((root/'database/migrations/09_approved_catalog_postgres.sql').read_text(encoding='utf-8'))
assert '3000001' in sql("SELECT advanced_price FROM subjects WHERE slug='cau-long';")
catalog_db = args.database+'_catalog'
sql('CREATE DATABASE '+catalog_db+';', 'postgres')
sql("""CREATE TABLE subjects(subject_id INT PRIMARY KEY,subject_name TEXT,normal_price NUMERIC,advanced_price NUMERIC,
        status TEXT DEFAULT 'Active',slug TEXT UNIQUE,outdoor BOOLEAN);
        CREATE TABLE catalog_refs(id INT PRIMARY KEY,subject_id INT REFERENCES subjects(subject_id));
        INSERT INTO subjects(subject_id,subject_name,normal_price,advanced_price) VALUES
        (1,'Cầu lông',0,0),(2,'Pickleball',0,0),(3,'Bóng bàn',0,0),(4,'Bắn cung',0,0),(5,'Bóng rổ',0,0),
        (6,'Bóng chuyền',0,0),(7,'Bắn súng',0,0),(8,'Bóng đá',0,0),(9,'Quần vợt',0,0);
        INSERT INTO subjects(subject_id,slug,subject_name,normal_price,advanced_price) VALUES
        (19,'quan-vot-tennis','Quần vợt (Tennis)',1500000,10000000),(20,'boi-loi','Bơi lội',1200000,9000000),
        (21,'pickleball','Pickleball',1100000,8500000),(22,'ban-cung','Bắn cung',1400000,9500000),
        (23,'bong-ro','Bóng rổ',1000000,8000000),(24,'bong-da','Bóng đá',900000,8000000),
        (25,'bong-chuyen','Bóng chuyền',900000,7000000),(26,'cau-long','Cầu lông',900000,7500000),
        (27,'bong-ban','Bóng bàn',800000,6500000);""",catalog_db)
catalog_migration=(root/'database/migrations/09_approved_catalog_postgres.sql').read_text(encoding='utf-8')
sql('BEGIN; INSERT INTO catalog_refs VALUES(1,1);'+catalog_migration.replace('BEGIN;','',1),catalog_db,
    success=False,expected_error='D02 duplicate subject has new references')
assert '18' in sql('SELECT count(*) FROM subjects;',catalog_db), 'Guard failure did not preserve catalog'
sql(catalog_migration,catalog_db)
assert '9' in sql('SELECT count(*) FROM subjects WHERE subject_id BETWEEN 19 AND 27;',catalog_db)
sql(catalog_migration,catalog_db)
print(json.dumps({'database':args.database,'localOnly':True,'migrationReplay':True,'invoiceEnrollmentSnapshotsPreserved':True,
                  'catalog':9,'priceReplayPreserved':True,'exactSnapshotCleanupAndFkGuard':True},indent=2))
