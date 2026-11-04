"""Verify migration15 on disposable local PostgreSQL only. Never loads .env or cloud URLs."""
from pathlib import Path
import argparse
import datetime as dt
import hashlib
import json
import re
import subprocess

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output', default='artifacts/runs/querydr/migration.json')
args = parser.parse_args()
container = 'scms-phases-20261027'
info = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
assert info['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]
prefix = 'scms_querydr_' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d%H%M%S')
assert re.fullmatch(r'scms_querydr_[0-9]+', prefix)


def sql(db, query, expected_error=None):
    assert re.fullmatch(r'(postgres|scms_querydr_[a-z0-9_]+)', db)
    result = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-At', '-v',
                             'ON_ERROR_STOP=1', '-U', 'postgres', '-d', db],
                            input=query, text=True, encoding='utf-8', capture_output=True)
    if expected_error:
        assert result.returncode and expected_error in result.stderr, result.stderr[-1800:]
    elif result.returncode:
        raise RuntimeError(result.stderr[-2400:])
    return result.stdout.strip()


def database(suffix):
    name = prefix + '_' + suffix
    sql('postgres', 'CREATE DATABASE ' + name)
    return name


def snapshot(db, tables):
    assert all(re.fullmatch(r'[a-z_][a-z0-9_]*', table) for table in tables)
    parts = ["SELECT '" + table + "' AS table_name,md5(coalesce(string_agg(to_jsonb(t)::text,',' "
             "ORDER BY to_jsonb(t)::text),'')) AS digest FROM public." + table + ' t' for table in tables]
    return json.loads(sql(db, 'SELECT json_object_agg(table_name,digest) FROM (' + ' UNION ALL '.join(parts) + ') snapshots'))


migration = (root / 'database/migrations/15_QueryDR.sql').read_text(encoding='utf-8')
table_ddl = migration.split('-- QUERYDR_TABLE_BEGIN\n', 1)[1].split('-- QUERYDR_TABLE_END', 1)[0].rstrip() + '\n'
checks = {}
db = database('upgrade')
baseline = (root / 'database/schema_postgres.sql').read_text(encoding='utf-8').replace(table_ddl, '', 1)
assert table_ddl not in baseline
sql(db, baseline)
tables = sql(db, "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").splitlines()
assert 'invoices' in tables and 'vnpay_payment_attempts' not in tables
before = snapshot(db, tables)
sql(db, migration)
assert snapshot(db, tables) == before
assert sql(db, 'SELECT count(*) FROM vnpay_payment_attempts') == '0'
assert sql(db, "SELECT relrowsecurity FROM pg_class WHERE oid='vnpay_payment_attempts'::regclass") == 't'
checks['additiveExistingTablesUnchanged'] = 'PASS'
sql(db, "INSERT INTO vnpay_payment_attempts(invoice_id,tmn_code,txn_ref,transaction_date,amount_minor,created_at) "
        "SELECT min(invoice_id),'TEST0001','MIGRATION-TEST','20261104120000',90000000,'2026-11-04 12:00:00' FROM invoices")
attempt_before = snapshot(db, ['vnpay_payment_attempts'])
sql(db, migration)
assert snapshot(db, tables) == before and snapshot(db, ['vnpay_payment_attempts']) == attempt_before
checks['replayPreservesAllRows'] = 'PASS'
sql(db, "INSERT INTO vnpay_payment_attempts(invoice_id,tmn_code,txn_ref,transaction_date,amount_minor,created_at) "
        "SELECT invoice_id,tmn_code,txn_ref,transaction_date,amount_minor,created_at FROM vnpay_payment_attempts",
    'uq_vnpay_attempt_request')
sql(db, "INSERT INTO vnpay_payment_attempts(invoice_id,tmn_code,txn_ref,transaction_date,amount_minor,created_at) "
        "VALUES(2147483647,'TEST0001','BAD-FK','20261104120000',1000,'2026-11-04')", 'fk_vnpay_attempt_invoice')
sql(db, "UPDATE vnpay_payment_attempts SET amount_minor=0", 'chk_vnpay_attempt_amount')
sql(db, "UPDATE vnpay_payment_attempts SET transaction_date='bad-date'", 'chk_vnpay_attempt_date')
sql(db, "DELETE FROM invoices WHERE invoice_id=(SELECT invoice_id FROM vnpay_payment_attempts LIMIT 1)", 'fk_vnpay_attempt_invoice')
assert snapshot(db, tables) == before and snapshot(db, ['vnpay_payment_attempts']) == attempt_before
checks['foreignKeyUniqueAndDataGuards'] = 'PASS'

conflict = database('conflict')
sql(conflict, 'CREATE TABLE invoices(invoice_id integer PRIMARY KEY); INSERT INTO invoices VALUES(1); '
              'CREATE TABLE vnpay_payment_attempts(unrelated text); INSERT INTO vnpay_payment_attempts VALUES(\'keep\')')
collision_before = snapshot(conflict, ['invoices', 'vnpay_payment_attempts'])
sql(conflict, migration, 'QUERYDR_SCHEMA_CONFLICT')
assert snapshot(conflict, ['invoices', 'vnpay_payment_attempts']) == collision_before
assert sql(conflict, "SELECT to_regclass('vnpay_payment_attempts_attempt_id_seq') IS NULL") == 't'
checks['nameCollisionRollsBackWithoutTouchingData'] = 'PASS'
wrong_guard = database('wrong_guard')
sql(wrong_guard, 'CREATE TABLE invoices(invoice_id integer PRIMARY KEY)')
sql(wrong_guard, table_ddl)
sql(wrong_guard, 'ALTER TABLE vnpay_payment_attempts DROP CONSTRAINT chk_vnpay_attempt_amount; '
                 'ALTER TABLE vnpay_payment_attempts ADD CONSTRAINT chk_vnpay_attempt_amount CHECK(amount_minor>0)')
sql(wrong_guard, migration, 'QUERYDR_SCHEMA_CONFLICT')
assert sql(wrong_guard, "SELECT relrowsecurity FROM pg_class WHERE oid='vnpay_payment_attempts'::regclass") == 'f'
checks['sameNamedButDifferentGuardRejectedBeforePermissionsChange'] = 'PASS'
missing = database('missing')
sql(missing, migration, 'QUERYDR_PREFLIGHT')
assert sql(missing, "SELECT to_regclass('vnpay_payment_attempts') IS NULL") == 't'
checks['missingPrerequisiteRollsBack'] = 'PASS'

for variant in ['schema_postgres.sql', 'database/schema_postgres.sql']:
    target = database('root' if variant.startswith('schema') else 'demo')
    content = (root / variant).read_text(encoding='utf-8')
    sql(target, content)
    sql(target, migration)
    sql(target, content)
    assert sql(target, 'SELECT count(*) FROM vnpay_payment_attempts') == '0'
    checks[variant + 'FreshAndReplayCompatible'] = 'PASS'

behavior = database('behavior')
release_dir = root / 'artifacts/runs/querydr/empty-release'
subprocess.run(['python', str(root / 'scripts/prepare-phases-release.py'), '--output', str(release_dir)], check=True)
sql(behavior, (release_dir / 'scms-phases-empty-schema.sql').read_text(encoding='utf-8'))
sql(behavior, migration)
assert sql(behavior, "SELECT count(*) FROM users") == '0'
assert sql(behavior, "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND "
                    "((table_name='member_packages' AND column_name='activated_at') OR "
                    "(table_name='subjects' AND column_name='outdoor'))") == '2'
checks['emptyReleaseCurrentRuntimeColumnsAndZeroAccounts'] = 'PASS'
report = {'result': 'PASS', 'scope': 'Disposable PostgreSQL17 at 127.0.0.1:55440 only; no .env/cloud',
          'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip(),
          'workingTree': True, 'checks': checks, 'oldTableDigests': before,
          'migrationSha256': hashlib.sha256(migration.encode()).hexdigest(),
          'behaviorDatabase': behavior, 'gatewayRequests': 0}
out = root / args.output
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'result': 'PASS', 'checks': list(checks), 'behaviorDatabase': behavior, 'output': str(out)}))
