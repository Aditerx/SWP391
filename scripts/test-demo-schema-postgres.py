"""Verify the demo reset only on a NEW disposable loopback PostgreSQL DB. Never reads .env."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
p.add_argument('--schema', default='database/schema_postgres.sql')
p.add_argument('--output', required=True)
args = p.parse_args()
if not re.fullmatch(r'scms_schema_demo_[a-z0-9_]+', args.database):
    p.error('A NEW scms_schema_demo_* local database is required')
root = Path(__file__).resolve().parents[1]
container = 'scms-phases-20261027'
inspection = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': '55440'}]
schema_path = (root / args.schema).resolve()
assert schema_path.is_relative_to(root)
schema = schema_path.read_text(encoding='utf-8')
proof = {'database': args.database, 'localOnly': True, 'envLoaded': False,
         'schemaSha256': hashlib.sha256(schema_path.read_bytes()).hexdigest(), 'checks': []}


def sql(statement, database=None, error=None):
    result = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-At', '-v', 'ON_ERROR_STOP=1',
                             '-U', 'postgres', '-d', database or args.database],
                            input=statement, text=True, encoding='utf-8', capture_output=True)
    if error:
        assert result.returncode != 0 and error in result.stderr, result.stderr[-1400:]
    else:
        assert result.returncode == 0, result.stderr[-1400:]
    return result.stdout.strip()


def check(name, statement=None, expected=None):
    if statement:
        assert sql(statement) == expected, name
    proof['checks'].append({'name': name, 'status': 'PASS'})


sql('CREATE DATABASE ' + args.database, 'postgres')  # fails if it exists; never reset an arbitrary DB
sql("""DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='anon') THEN CREATE ROLE anon NOLOGIN; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN CREATE ROLE authenticated NOLOGIN; END IF;
END $$;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon,authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO anon,authenticated;
CREATE TABLE public.demo_external_sentinel(id INT PRIMARY KEY);
INSERT INTO public.demo_external_sentinel VALUES(391);
CREATE FUNCTION public.demo_external_function() RETURNS INT LANGUAGE SQL AS 'SELECT 391';
CREATE SEQUENCE public.demo_external_sequence START 391;
""")
sql(schema)
check('fresh_install_reference_counts', "SELECT concat_ws(',',(SELECT count(*) FROM users),(SELECT count(*) FROM subjects),(SELECT count(*) FROM rooms),(SELECT count(*) FROM classes),(SELECT count(*) FROM coach_work_slots))", '47,9,20,21,756')
check('unlisted_objects_and_grants_preserved', "SELECT concat_ws(',',(SELECT id FROM demo_external_sentinel),demo_external_function(),nextval('demo_external_sequence'),has_table_privilege('anon','demo_external_sentinel','SELECT'))", '391,391,391,t')
check('scms_direct_browser_grants_revoked', """SELECT count(*) FROM information_schema.role_table_grants
 WHERE table_schema='public' AND table_name<>'demo_external_sentinel' AND grantee IN ('PUBLIC','anon','authenticated')""", '0')
check('scms_sequence_browser_grants_revoked', """SELECT count(*) FROM pg_sequences
 WHERE schemaname='public' AND sequencename<>'demo_external_sequence'
 AND (has_sequence_privilege('anon',quote_ident(schemaname)||'.'||quote_ident(sequencename),'USAGE,SELECT,UPDATE')
 OR has_sequence_privilege('authenticated',quote_ident(schemaname)||'.'||quote_ident(sequencename),'USAGE,SELECT,UPDATE'))""", '0')
check('approved_accounts_passwords_and_first_login', "SELECT count(*) FROM users WHERE NOT is_first_login AND password_hash=extensions.crypt('SWP391',password_hash)", '47')
check('approved_synthetic_history_and_dynamic_secrets_empty', "SELECT (EXISTS(SELECT 1 FROM invoices) AND EXISTS(SELECT 1 FROM attendances) AND EXISTS(SELECT 1 FROM training_results) AND NOT EXISTS(SELECT 1 FROM email_otps) AND NOT EXISTS(SELECT 1 FROM vnpay_payment_attempts) AND NOT EXISTS(SELECT 1 FROM invoices WHERE payment_status<>'Paid'))::text", 'true')
check('current_guard_columns_and_validated_constraints', """SELECT concat_ws(',',
 (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND
 ((table_name='sessions' AND column_name='attendance_review_pending') OR (table_name='classes' AND column_name='pricing_draft') OR
 (table_name='rooms' AND column_name='activity_type') OR (table_name='system_logs' AND column_name IN ('audit_category','center_id','scope_source')))),
 (SELECT count(*) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname='public' AND NOT c.convalidated))""", '6,0')
sql("BEGIN; UPDATE users SET status='Pending' WHERE user_id=2; SELECT status FROM users WHERE user_id=2; ROLLBACK;")
check('otp_pending_remains_supported')
sql("BEGIN; UPDATE users SET email=upper((SELECT email FROM users WHERE user_id=2)) WHERE user_id=3; COMMIT;", error='uq_scms_users_email_ci')
check('case_insensitive_email_collision_rejected')

# DROP RESTRICT must roll back the entire reset when an external object refers to SCMS.
sql('CREATE TABLE public.demo_external_fk(id INT REFERENCES public.users(user_id)); INSERT INTO demo_external_fk VALUES(2);')
before = sql("SELECT md5(string_agg(user_id||':'||password_hash,',' ORDER BY user_id)) FROM users")
sql(schema, error='depend')
assert before == sql("SELECT md5(string_agg(user_id||':'||password_hash,',' ORDER BY user_id)) FROM users")
assert sql('SELECT id FROM demo_external_fk') == '2'
sql('DROP TABLE public.demo_external_fk RESTRICT;')
check('external_fk_blocks_reset_and_preserves_rows')
sql('CREATE VIEW public.demo_external_view AS SELECT user_id FROM public.users;')
sql(schema, error='depend')
assert before == sql("SELECT md5(string_agg(user_id||':'||password_hash,',' ORDER BY user_id)) FROM users")
sql('DROP VIEW public.demo_external_view RESTRICT;')
check('external_view_blocks_reset_and_preserves_rows')

# Dirty replay: historical backup debris and business data must actually disappear.
old_ref = int(sql("SELECT nextval('public.invoice_code_seq')"))
sql("""CREATE TABLE attendances_backup_flow4(id INT); INSERT INTO attendances_backup_flow4 VALUES(164);
CREATE TABLE class_enrollments_backup_flow4(id INT); INSERT INTO class_enrollments_backup_flow4 VALUES(1);
CREATE TABLE role_permissions_backup_n06(id INT); INSERT INTO role_permissions_backup_n06 VALUES(1);
CREATE TABLE training_results_backup_flow4(id INT); INSERT INTO training_results_backup_flow4 VALUES(1);
INSERT INTO invoices(invoice_code,member_id,class_id,amount,payment_status,payment_method,created_at)
 VALUES('INV-2026-9000000000000',2,1,900000,'Pending','Cash',CURRENT_TIMESTAMP);
UPDATE users SET full_name='Local dirty fixture' WHERE user_id=2;
""")
sql(schema)
assert sql("SELECT count(*) FROM invoices WHERE invoice_code='INV-2026-9000000000000'") == '0'
assert sql('SELECT count(*) FROM users') == '47'
assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4')") == '0'
assert int(sql("SELECT nextval('public.invoice_code_seq')")) > max(old_ref, 9_000_000_000_000)
check('dirty_replay_rebuilds_only_approved_synthetic_rows')
check('payment_reference_counter_never_reused')
normal_migration=(root/'database/migrations/22_normal_changes_refunds_postgres.sql').read_text(encoding='utf-8')
snapshot_query="""SELECT md5(string_agg(to_jsonb(i)::text,',' ORDER BY invoice_id)) FROM invoices i;
SELECT md5(string_agg(to_jsonb(r)::text,',' ORDER BY id)) FROM payment_resolutions r;
SELECT md5(string_agg(sha256||':'||octet_length(content),',' ORDER BY id)) FROM normal_refund_documents;"""
before=sql(snapshot_query)
sql(normal_migration);sql(normal_migration)
assert before==sql(snapshot_query)
check('normal22_replay_preserves_receipts_refunds_private_assets')
sql("BEGIN; ALTER TABLE normal_refunds ALTER COLUMN version TYPE TEXT;"+normal_migration.replace('BEGIN;','',1),error='Incompatible Normal table normal_refunds.version')
assert before==sql(snapshot_query)
check('normal22_incompatible_type_rolls_back')
sql("BEGIN; UPDATE normal_refunds SET receiver_id=(SELECT receiver_id FROM normal_refunds WHERE receiver_id IS NOT NULL ORDER BY id LIMIT 1) WHERE id=(SELECT id FROM normal_refunds WHERE receiver_id IS NULL ORDER BY id LIMIT 1); COMMIT;",error='fk_normal_refund_receiver')
sql("BEGIN; INSERT INTO normal_refund_attempts(refund_id,receiver_id,claim_key,claimed_by,claimed_at,status,amount,currency) SELECT refund_id,receiver_id,'39139139-1391-4391-8391-391391391391',claimed_by,claimed_at,'Claimed',amount,currency FROM normal_refund_attempts WHERE status='Submitted' LIMIT 1; COMMIT;",error='uq_normal_refund_active_attempt')
assert before==sql(snapshot_query)
check('receiver_fk_and_active_bank_claim_constraints_reject_cross_case_or_duplicate')
check('synthetic_schedule_has_no_resource_or_member_overlap',"""SELECT count(*) FROM sessions a JOIN sessions b ON a.session_id<b.session_id AND a.session_date=b.session_date AND a.start_time<b.end_time AND b.start_time<a.end_time
 WHERE a.status<>'Cancelled' AND b.status<>'Cancelled' AND NOT a.center_invalidated AND NOT b.center_invalidated AND
 (a.room_id=b.room_id OR a.coach_id=b.coach_id OR EXISTS(SELECT 1 FROM class_enrollments e JOIN class_enrollments f ON e.member_id=f.member_id WHERE e.class_id=a.class_id AND f.class_id=b.class_id AND e.status IN ('Registered','Completed') AND f.status IN ('Registered','Completed')) OR
 a.booked_member_id=b.booked_member_id OR EXISTS(SELECT 1 FROM class_enrollments e WHERE e.member_id=a.booked_member_id AND e.class_id=b.class_id AND e.status IN ('Registered','Completed')) OR EXISTS(SELECT 1 FROM class_enrollments e WHERE e.member_id=b.booked_member_id AND e.class_id=a.class_id AND e.status IN ('Registered','Completed')))""",'0')
check('expired_credit_and_reserved_advanced_balances',"""SELECT count(*) FROM member_packages p WHERE
 (p.status='Expired' AND coalesce(p.expiry_revoked_credit,0)+(SELECT count(*) FROM sessions s WHERE s.subscription_id=p.subscription_id AND NOT s.credit_returned)<>12) OR
 (p.status IN ('Active','Completed') AND p.remaining_credit+(SELECT count(*) FROM sessions s WHERE s.subscription_id=p.subscription_id AND NOT s.credit_returned)<>12)""",'0')
check('synthetic_financial_history_order_and_no_future_completion',"""SELECT count(*) FROM payment_resolutions r JOIN invoices i ON i.invoice_id=r.invoice_id WHERE
 r.created_at<i.payment_date OR r.approved_at<r.created_at OR r.completed_at<r.performed_at OR r.completed_at>(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh') OR
 EXISTS(SELECT 1 FROM normal_refund_attempts a WHERE a.refund_id=r.id AND (a.claimed_at<r.approved_at OR a.performed_at<a.claimed_at OR a.verified_at<a.performed_at)) OR
 EXISTS(SELECT 1 FROM normal_refunds f JOIN classes c ON c.class_id=f.class_id WHERE f.id=r.id AND r.created_at>c.start_date::timestamp AND f.origin='CenterCancellation')""",'0')
check('unlisted_objects_survive_replay', "SELECT concat_ws(',',(SELECT id FROM demo_external_sentinel),demo_external_function(),nextval('demo_external_sequence'))", '391,391,392')
sql('DROP TABLE public.demo_external_sentinel RESTRICT; DROP FUNCTION public.demo_external_function() RESTRICT; DROP SEQUENCE public.demo_external_sequence RESTRICT;')
proof['status'] = 'PASS'
output = (root / args.output).resolve()
assert output.is_relative_to(root)
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(proof, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'status': 'PASS', 'database': args.database, 'checks': len(proof['checks']), 'cloudMutation': False}))
