"""Migration16 proof on newly created loopback PostgreSQL databases only; never loads .env."""
from pathlib import Path
import datetime as dt
import hashlib
import json
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CONTAINER = 'scms-phases-20261027'
info = json.loads(subprocess.check_output(['docker', 'inspect', CONTAINER], text=True))[0]
assert info['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]
PREFIX = 'scms_phases_legacy_' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d%H%M%S')
MIGRATION = ROOT / 'database/migrations/16_retire_legacy_memberships_postgres.sql'
SQL = MIGRATION.read_text(encoding='utf-8')
SCHEMA = (ROOT / 'database/schema_postgres.sql').read_text(encoding='utf-8')
OUT = ROOT / 'artifacts/runs/retire-legacy-memberships/2026-11-06'
OUT.mkdir(parents=True, exist_ok=True)
checks = {}


def sql(database, query, error=None):
    assert database == 'postgres' or re.fullmatch(re.escape(PREFIX) + r'_[a-z_]+', database)
    result = subprocess.run(['docker', 'exec', '-i', CONTAINER, 'psql', '-X', '-q', '-At',
                             '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', database],
                            input=query, text=True, encoding='utf-8', capture_output=True)
    if error:
        assert result.returncode and error in result.stderr, result.stderr[-1600:]
    elif result.returncode:
        raise RuntimeError(result.stderr[-1600:])
    return result.stdout.strip()


def database(suffix):
    name = PREFIX + '_' + suffix
    sql('postgres', 'CREATE DATABASE ' + name)
    sql(name, SCHEMA)
    return name


def snapshot(database, exclude=()):
    tables = sql(database, "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").splitlines()
    parts = []
    for table in tables:
        assert re.fullmatch(r'[a-z_]+', table)
        if table in exclude:
            continue
        parts.append("SELECT '" + table + "' AS name,md5(coalesce(string_agg(to_jsonb(t)::text,',' ORDER BY to_jsonb(t)::text),'')) AS digest FROM public." + table + ' t')
    return json.loads(sql(database, 'SELECT json_object_agg(name,digest) FROM (' + ' UNION ALL '.join(parts) + ') hashes'))


def seed_legacy(database):
    return json.loads(sql(database, """
      WITH package AS (
        INSERT INTO membership_packages(package_name,price,duration_days,package_type,status,center_id)
        VALUES('LOCAL obsolete shared membership',100000,30,'Normal','Active',1) RETURNING package_id
      ), subscription AS (
        INSERT INTO member_packages(member_id,package_id,start_date,end_date,remaining_credit,status)
        SELECT (SELECT min(user_id) FROM members),package_id,CURRENT_DATE,CURRENT_DATE+30,0,'Active' FROM package
        RETURNING subscription_id,member_id,package_id
      ), receipt AS (
        INSERT INTO invoices(member_id,package_id,subscription_id,amount,payment_status,payment_method,invoice_code,created_at,payment_date)
        SELECT member_id,package_id,subscription_id,100000,'Paid','Cash','LOCAL-OBSOLETE-RECEIPT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM subscription
        RETURNING invoice_id,subscription_id,package_id
      ) SELECT row_to_json(receipt) FROM receipt;
    """))


# Existing modern data must be byte-for-byte unchanged when no legacy data exists.
empty = database('empty')
before = snapshot(empty, ('scms_data_migration_audit',))
sql(empty, SQL)
assert snapshot(empty, ('scms_data_migration_audit',)) == before
checks['modernOnlyBaselineUnchanged'] = 'PASS'

# Complete unambiguous obsolete subgraph; current Advanced may retain a legacy catalog FK.
upgrade = database('upgrade')
legacy = seed_legacy(upgrade)
sql(upgrade, f"""
 INSERT INTO vnpay_payment_attempts(invoice_id,tmn_code,txn_ref,transaction_date,amount_minor,created_at)
 VALUES({legacy['invoice_id']},'LOCAL0001','LOCAL-OBSOLETE','20261106120000',10000000,CURRENT_TIMESTAMP);
 INSERT INTO payment_resolutions(invoice_id,resolution_type,amount,reason,created_by,created_at)
 VALUES({legacy['invoice_id']},'CashCorrection',100000,'LOCAL obsolete model fixture',
   (SELECT min(user_id) FROM center_managers),CURRENT_TIMESTAMP);
""")
bridge = json.loads(sql(upgrade, """
 WITH catalog AS (
  INSERT INTO membership_packages(package_name,price,duration_days,package_type,status,center_id)
  VALUES('LOCAL catalog link on current Advanced',3000000,45,'Advanced','Active',1) RETURNING package_id
 ), chosen AS (
  SELECT p.subscription_id,i.invoice_id FROM member_packages p JOIN invoices i USING(subscription_id)
  WHERE p.subject_id IS NOT NULL AND p.primary_coach_id IS NOT NULL ORDER BY p.subscription_id LIMIT 1
 ), detach_later AS (
  UPDATE member_packages SET package_id=(SELECT package_id FROM catalog)
  WHERE subscription_id=(SELECT subscription_id FROM chosen) RETURNING subscription_id
 ), receipt AS (
  UPDATE invoices SET package_id=(SELECT package_id FROM catalog)
  WHERE invoice_id=(SELECT invoice_id FROM chosen) RETURNING invoice_id,subscription_id
 ) SELECT row_to_json(receipt) FROM receipt;
"""))
protected = snapshot(upgrade, ('membership_packages', 'member_packages', 'invoices', 'payment_resolutions',
                                'vnpay_payment_attempts', 'scms_data_migration_audit'))
current_sub = sql(upgrade, f"SELECT (to_jsonb(p)-'package_id')::text FROM member_packages p WHERE subscription_id={bridge['subscription_id']}")
current_invoice = sql(upgrade, f"SELECT (to_jsonb(i)-'package_id')::text FROM invoices i WHERE invoice_id={bridge['invoice_id']}")
sql(upgrade, SQL)
assert snapshot(upgrade, tuple(set(snapshot(upgrade)) - set(protected))) == protected
assert sql(upgrade, 'SELECT count(*) FROM membership_packages') == '0'
assert sql(upgrade, f"SELECT count(*) FROM member_packages WHERE subscription_id={legacy['subscription_id']}") == '0'
assert sql(upgrade, f"SELECT count(*) FROM invoices WHERE invoice_id={legacy['invoice_id']}") == '0'
assert sql(upgrade, f"SELECT count(*) FROM payment_resolutions WHERE invoice_id={legacy['invoice_id']}") == '0'
assert sql(upgrade, f"SELECT count(*) FROM vnpay_payment_attempts WHERE invoice_id={legacy['invoice_id']}") == '0'
assert sql(upgrade, f"SELECT (to_jsonb(p)-'package_id')::text FROM member_packages p WHERE subscription_id={bridge['subscription_id']}") == current_sub
assert sql(upgrade, f"SELECT (to_jsonb(i)-'package_id')::text FROM invoices i WHERE invoice_id={bridge['invoice_id']}") == current_invoice
counts = json.loads(sql(upgrade, "SELECT after_data FROM scms_data_migration_audit WHERE migration_key='20261106_retire_legacy_memberships_v1'"))
assert counts == dict(deleted_catalog=2, deleted_legacy_subscriptions=1, deleted_legacy_invoices=1,
                     deleted_legacy_payment_attempts=1, deleted_legacy_resolutions=1,
                     detached_current_subscriptions=1, detached_current_invoices=1, model_retired=True), counts
checks['obsoleteSubgraphDeletedAndCurrentAdvancedPreserved'] = 'PASS'
after = snapshot(upgrade)
sql(upgrade, SQL)
assert snapshot(upgrade) == after
checks['replayDoesNotDeleteOrModifyAnythingElse'] = 'PASS'

sql(upgrade, "INSERT INTO membership_packages(package_name,price,duration_days) VALUES('cannot return',1,30)", 'LEGACY_MEMBERSHIP_RETIRED')
sql(upgrade, "INSERT INTO member_packages(member_id,status) SELECT min(user_id),'Pending' FROM members", 'LEGACY_MEMBERSHIP_RETIRED')
sql(upgrade, f"UPDATE invoices SET package_id=2147483647 WHERE invoice_id={bridge['invoice_id']}", 'LEGACY_MEMBERSHIP_RETIRED')
assert snapshot(upgrade) == after
checks['retiredCatalogSubscriptionAndInvoiceWritesBlocked'] = 'PASS'
sql(upgrade, f"BEGIN; UPDATE member_packages SET remaining_credit=remaining_credit WHERE subscription_id={bridge['subscription_id']}; UPDATE invoices SET amount=amount WHERE invoice_id={bridge['invoice_id']}; ROLLBACK;")
checks['currentAdvancedWritesRemainCompatible'] = 'PASS'

linked = database('linked')
old = seed_legacy(linked)
sql(linked, f"UPDATE class_enrollments SET eligibility_subscription_id={old['subscription_id']} WHERE enrollment_id=(SELECT min(enrollment_id) FROM class_enrollments)")
before = snapshot(linked)
sql(linked, SQL, 'SCMS_LEGACY_RETIRE_BLOCKED')
assert snapshot(linked) == before
checks['currentEnrollmentLinkStopsEntirePurge'] = 'PASS'

partial = database('partial')
sql(partial, "INSERT INTO member_packages(member_id,subject_id,status) SELECT min(user_id),(SELECT min(subject_id) FROM subjects),'Pending' FROM members")
before = snapshot(partial)
sql(partial, SQL, 'SCMS_LEGACY_RETIRE_BLOCKED')
assert snapshot(partial) == before
checks['partialAdvancedCannotBeMisclassifiedAsObsolete'] = 'PASS'

external = database('external')
old = seed_legacy(external)
sql(external, f"CREATE TABLE outside_scope(id int PRIMARY KEY,package_id int REFERENCES membership_packages(package_id) ON DELETE CASCADE); INSERT INTO outside_scope VALUES(1,{old['package_id']})")
before = snapshot(external)
sql(external, SQL, 'SCMS_LEGACY_RETIRE_PREFLIGHT')
assert snapshot(external) == before
checks['unknownCascadingForeignKeyStopsBeforeMutation'] = 'PASS'

# A hidden trigger attempting to alter current money during cleanup must be caught
# by postflight witnesses, and ALL preceding deletes must roll back.
corrupt = database('postflight')
seed_legacy(corrupt)
sql(corrupt, """
 CREATE FUNCTION fn_local_corrupt_receipt() RETURNS trigger LANGUAGE plpgsql AS $$
 BEGIN UPDATE invoices SET amount=amount+1 WHERE package_id IS NULL; RETURN OLD; END $$;
 CREATE TRIGGER trg_local_corrupt_receipt AFTER DELETE ON membership_packages
 FOR EACH ROW EXECUTE FUNCTION fn_local_corrupt_receipt();
""")
before = snapshot(corrupt)
sql(corrupt, SQL, 'SCMS_LEGACY_RETIRE_POSTFLIGHT')
assert snapshot(corrupt) == before
checks['postflightDetectsUnexpectedMoneyChangeAndRollsBackAllDeletes'] = 'PASS'

sql(upgrade, "ALTER TABLE membership_packages DISABLE TRIGGER trg_scms_retired_membership_catalog; INSERT INTO membership_packages(package_name,price,duration_days) VALUES('LOCAL simulated drift',1,30); ALTER TABLE membership_packages ENABLE TRIGGER trg_scms_retired_membership_catalog;")
before = snapshot(upgrade)
sql(upgrade, SQL, 'SCMS_LEGACY_RETIRE_REPLAY_CONFLICT')
assert snapshot(upgrade) == before
checks['replayWithUnexpectedReintroducedLegacyRowsStops'] = 'PASS'

result = dict(result='PASS', environment='PostgreSQL 17 / disposable Docker loopback 127.0.0.1:55440',
              migration=MIGRATION.relative_to(ROOT).as_posix(),
              migrationSha256=hashlib.sha256(MIGRATION.read_bytes()).hexdigest(),
              sourceSchemaSha256=hashlib.sha256((ROOT/'database/schema_postgres.sql').read_bytes()).hexdigest(),
              checks=checks, deletionCountsFixture=counts, databases=dict(empty=empty, upgrade=upgrade,
                linked=linked, partial=partial, external=external, postflight=corrupt),
              cloudExecution=False, currentPaymentOrEntitlementStateChanged=False)
(OUT/'migration.json').write_text(json.dumps(result, indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(result='PASS', checks=checks, report=(OUT/'migration.json').relative_to(ROOT).as_posix())))
