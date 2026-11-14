"""Verify the demo reset only on a NEW disposable loopback PostgreSQL DB. Never reads .env."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from datetime import date,timedelta
from release_catalog import load_requirements, PROFILE

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--database', required=True)
p.add_argument('--schema', default='database/schema_postgres.sql')
p.add_argument('--output', required=True)
p.add_argument('--container', default='scms-phases-20261027')
p.add_argument('--port', type=int, choices=(55440, 54391), default=55440)
args = p.parse_args()
if not re.fullmatch(r'scms_schema_demo_[a-z0-9_]+', args.database):
    p.error('A NEW scms_schema_demo_* local database is required')
root = Path(__file__).resolve().parents[1]
if not re.fullmatch(r'scms-(?:phases-20261027|bundle-[a-z0-9-]+)',args.container):
    p.error('Use the existing local test container or a task-specific scms-bundle-* container')
container = args.container
inspection = json.loads(subprocess.check_output(['docker', 'inspect', container], text=True))[0]
assert inspection['HostConfig']['PortBindings']['5432/tcp'] == [{'HostIp': '127.0.0.1', 'HostPort': str(args.port)}]
schema_path = (root / args.schema).resolve()
assert schema_path.is_relative_to(root)
schema = schema_path.read_text(encoding='utf-8')
anchor_match=re.search(r"business_date<>DATE '(\d{4}-\d{2}-\d{2})'",schema)
if not anchor_match:p.error('Regenerate bundle with the early anchor preflight')
anchor=date.fromisoformat(anchor_match.group(1))
assert schema.index('SCMS_DEMO_STALE_ANCHOR')<schema.index('CREATE SCHEMA')<schema.index('DROP TABLE')
proof = {'database': args.database, 'container':container, 'port':args.port, 'localOnly': True, 'envLoaded': False,
         'schemaSha256': hashlib.sha256(schema_path.read_bytes()).hexdigest(),
         'anchorDate':anchor.isoformat(),'timeZone':'Asia/Ho_Chi_Minh','checks': []}


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


actual_date=sql("SELECT (clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date",'postgres')
if actual_date!=anchor.isoformat():p.error('Anchor differs from current VN date; no database created, no Clock override')
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
check('anchor_metadata_matches_bundle',"SELECT (after_data::jsonb->>'anchor_date')||','||(after_data::jsonb->>'business_timezone')||','||(after_data::jsonb->>'fixture_version') FROM scms_data_migration_audit WHERE migration_key='20261110_demo_rebuild_v3'",anchor.isoformat()+',Asia/Ho_Chi_Minh,normal-phase1-v3')
before_anchor=sql("SELECT md5(string_agg(to_jsonb(u)::text,',' ORDER BY user_id)) FROM users u")
previous=(anchor-timedelta(days=1)).isoformat();future=(anchor+timedelta(days=1)).isoformat()
for invalid in [previous,future]:
    # Negative fixture only: deliberately stale date; the canonical bundle/guard is unchanged.
    sql(schema.replace("DATE '"+anchor.isoformat()+"'","DATE '"+invalid+"'"),error='SCMS_DEMO_STALE_ANCHOR')
    assert before_anchor==sql("SELECT md5(string_agg(to_jsonb(u)::text,',' ORDER BY user_id)) FROM users u")
check('past_and_future_anchor_rejected_before_ddl_with_rows_preserved')
check('fresh_install_reference_counts', "SELECT concat_ws(',',(SELECT count(*) FROM users),(SELECT count(*) FROM subjects),(SELECT count(*) FROM rooms),(SELECT count(*) FROM classes),(SELECT count(*) FROM coach_work_slots))", '47,9,20,20,756')
requirements=load_requirements(root,PROFILE)
expected_dates=','.join(requirements['holidayDates'])
check('annual_holidays_present_inactive_and_demo_closure_active',
      "SELECT string_agg(holiday_date::text,',' ORDER BY holiday_date) FROM holidays WHERE NOT is_active",expected_dates)
check('only_expected_demo_closure_active',
      "SELECT string_agg(holiday_date::text,',' ORDER BY holiday_date) FROM holidays WHERE is_active",(anchor+timedelta(days=50)).isoformat())
check('advanced_classes_have_accepted_registration_and_exact_owner',"""SELECT count(*) FROM classes c LEFT JOIN advanced_registrations r ON r.class_id=c.class_id
 WHERE c.class_type='Advanced' AND (r.id IS NULL OR r.status NOT IN ('Accepted','Invoiced') OR c.coach_id IS DISTINCT FROM r.coach_id OR c.subject_id IS DISTINCT FROM r.subject_id)""",'0')
check('advanced_sessions_use_exact_subscription_private_class',"""SELECT count(*) FROM sessions s JOIN advanced_registrations r ON r.subscription_id=s.subscription_id WHERE s.class_id IS DISTINCT FROM r.class_id""",'0')
check('advanced_names_without_one_to_one_suffix',
      "SELECT count(*) FROM classes WHERE class_type='Advanced' AND class_name LIKE '%(1:1)%'",'0')
check('no_live_session_on_active_closure',
      "SELECT count(*) FROM sessions s JOIN holidays h ON h.holiday_date=s.session_date AND h.is_active WHERE s.status<>'Cancelled' AND NOT s.center_invalidated",'0')
check('member_creator_fk_matches_user_entity',
      "SELECT confrelid::regclass::text FROM pg_constraint WHERE conrelid='members'::regclass AND conname='fk_member_registered_by'",'users')
member_id=sql('SELECT min(user_id) FROM members')
creator_before=sql('SELECT coalesce(registered_by::text,\'NULL\') FROM members WHERE user_id='+member_id)
members_before=sql("SELECT md5(string_agg(to_jsonb(m)::text,',' ORDER BY user_id)) FROM members m")
for creator_role in ['CenterManager','Receptionist']:
    creator=sql("SELECT min(u.user_id) FROM users u JOIN roles r USING(role_id) WHERE r.role_name='"+creator_role+"' AND u.status='Active'")
    sql('BEGIN; UPDATE members SET registered_by='+creator+' WHERE user_id='+member_id+'; COMMIT;')
    check('member_creator_'+creator_role+'_persists_after_commit',
          'SELECT registered_by FROM members WHERE user_id='+member_id,creator)
sql('UPDATE members SET registered_by='+creator_before+' WHERE user_id='+member_id)
sql('BEGIN; UPDATE members SET registered_by=-391 WHERE user_id='+member_id+'; COMMIT;',error='fk_member_registered_by')
assert members_before==sql("SELECT md5(string_agg(to_jsonb(m)::text,',' ORDER BY user_id)) FROM members m")
check('member_creator_unknown_user_rejected_and_fixture_restored')
check('unlisted_objects_and_grants_preserved', "SELECT concat_ws(',',(SELECT id FROM demo_external_sentinel),demo_external_function(),nextval('demo_external_sequence'),has_table_privilege('anon','demo_external_sentinel','SELECT'))", '391,391,391,t')
check('scms_direct_browser_grants_revoked', """SELECT count(*) FROM information_schema.role_table_grants
 WHERE table_schema='public' AND table_name<>'demo_external_sentinel' AND grantee IN ('PUBLIC','anon','authenticated')""", '0')
check('scms_sequence_browser_grants_revoked', """SELECT count(*) FROM pg_sequences
 WHERE schemaname='public' AND sequencename<>'demo_external_sequence'
 AND (has_sequence_privilege('anon',quote_ident(schemaname)||'.'||quote_ident(sequencename),'USAGE,SELECT,UPDATE')
 OR has_sequence_privilege('authenticated',quote_ident(schemaname)||'.'||quote_ident(sequencename),'USAGE,SELECT,UPDATE'))""", '0')
check('approved_accounts_passwords_and_first_login', "SELECT count(*) FROM users WHERE NOT is_first_login AND password_hash=extensions.crypt('SWP391',password_hash)", '47')
check('approved_synthetic_history_and_dynamic_secrets_empty', "SELECT (EXISTS(SELECT 1 FROM invoices) AND EXISTS(SELECT 1 FROM attendances) AND EXISTS(SELECT 1 FROM training_results) AND NOT EXISTS(SELECT 1 FROM email_otps) AND NOT EXISTS(SELECT 1 FROM vnpay_payment_attempts) AND NOT EXISTS(SELECT 1 FROM invoices WHERE payment_status<>'Paid'))::text", 'true')
check('advanced_registration_decision_order_and_receipt_chronology', """SELECT count(*) FROM advanced_registrations r
 LEFT JOIN invoices i ON i.subscription_id=r.subscription_id
 WHERE (r.status IN ('Accepted','Invoiced','Rejected') AND (r.accepted_by IS NULL OR r.accepted_at IS NULL OR r.accepted_at<r.created_at))
 OR (i.invoice_id IS NOT NULL AND (r.accepted_at>i.created_at OR i.created_at>=r.accepted_at+interval '24hours'
     OR i.payment_date<i.created_at OR (i.payment_status='Paid' AND NOT i.reconciliation_required AND i.payment_date>=i.created_at+interval '5minutes')))""", '0')
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

# Proposal rows are disposable SQL constraint fixtures, not evidence of Member consent.
# The canonical bundle must leave this table empty; HTTP acceptance is checked separately.
check('advanced24_fresh_table_empty', 'SELECT count(*) FROM private_session_proposals', '0')
proposal_columns = ('subscription_id,proposed_by,coach_id,room_id,class_id,subject_id,'
                    'session_date,start_time,end_time,request_key,request_hash,created_at,respond_by')
proposal_select = """SELECT s.subscription_id,s.coach_id,s.coach_id,s.room_id,s.class_id,c.subject_id,
 s.session_date,s.start_time,s.end_time,'bundle24-pending',repeat('a',64),
 clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',s.session_date+s.start_time-interval '12hours'
 FROM sessions s JOIN classes c ON c.class_id=s.class_id
 WHERE s.subscription_id IS NOT NULL AND s.status='Scheduled' ORDER BY s.session_id LIMIT 1"""
sql('INSERT INTO private_session_proposals('+proposal_columns+') '+proposal_select)
check('advanced24_pending_has_no_session',
      "SELECT status||','||(session_id IS NULL)::text FROM private_session_proposals", 'Pending,true')
sql('INSERT INTO private_session_proposals('+proposal_columns+') '+proposal_select, error='uq_private_proposal_request')
sql("UPDATE private_session_proposals SET subscription_id=-391", error='foreign key')
sql("UPDATE private_session_proposals SET status='Expired',responded_at=created_at", error='ck_private_proposal_status')
sql("UPDATE private_session_proposals SET status='Accepted',responded_at=created_at", error='ck_private_proposal_session')
sql("UPDATE private_session_proposals SET responded_at=created_at", error='ck_private_proposal_response')
sql("UPDATE private_session_proposals SET end_time=start_time", error='ck_private_proposal_time')
sql("UPDATE private_session_proposals SET class_id=NULL,subject_id=NULL", error='ck_private_proposal_target')
sql("""INSERT INTO private_session_proposals("""+proposal_columns+""",status,responded_at,session_id)
 SELECT p.subscription_id,p.proposed_by,p.coach_id,p.room_id,p.class_id,p.subject_id,
 p.session_date,p.start_time,p.end_time,'bundle24-accepted',p.request_hash,p.created_at,p.respond_by,
 'Accepted',p.created_at,(SELECT min(session_id) FROM sessions WHERE subscription_id=p.subscription_id AND status='Scheduled')
 FROM private_session_proposals p LIMIT 1""")
sql("""INSERT INTO private_session_proposals("""+proposal_columns+""",status,responded_at,session_id)
 SELECT subscription_id,proposed_by,coach_id,room_id,class_id,subject_id,
 session_date,start_time,end_time,'bundle24-duplicate-session',request_hash,created_at,respond_by,
 status,responded_at,session_id FROM private_session_proposals WHERE status='Accepted'""",error='unique constraint')
check('advanced24_fk_status_response_time_target_and_idempotency_constraints')
advanced_snapshot = "\n".join("SELECT md5(string_agg(to_jsonb(t)::text,',' ORDER BY "+key+")) FROM "+table+" t;"
                              for table,key in [('private_session_proposals','id'),('member_packages','subscription_id'),
                                                ('sessions','session_id'),('invoices','invoice_id')])
advanced_before = sql(advanced_snapshot)
advanced_migration=(root/'database/migrations/24_advanced_session_proposals_postgres.sql').read_text(encoding='utf-8')
sql(advanced_migration);sql(advanced_migration)
assert advanced_before == sql(advanced_snapshot)
check('advanced24_replay_preserves_proposals_sessions_credits_and_receipts')
sql('BEGIN; ALTER TABLE private_session_proposals ALTER COLUMN request_hash TYPE TEXT;'+
    advanced_migration.replace('BEGIN;','',1),error='SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE')
assert advanced_before == sql(advanced_snapshot)
check('advanced24_incompatible_shape_rolls_back')

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
check('advanced24_reset_with_pending_and_accepted_history_recreates_empty_table',
      'SELECT count(*) FROM private_session_proposals', '0')
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
