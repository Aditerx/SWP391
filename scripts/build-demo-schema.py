"""Build the single reviewed SCMS demo-reset SQL. Never connects to a database."""
from pathlib import Path
import argparse
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', default='database/schema_postgres.sql')
p.add_argument('--anchor',default='2026-11-10',help='VN business date of the approved demo fixture')
args = p.parse_args()

TABLES = '''normal_refund_verifications normal_refund_documents normal_refund_attempts normal_refund_receivers normal_refunds normal_transfers normal_responses normal_proposals scms_data_migration_audit payment_resolutions incident_affected_members center_incidents session_changes
session_absence_notices advanced_registrations coach_days_off coach_work_slots coach_work_schedules subscription_pauses
normal_class_requests system_logs ai_consultations chat_read_state chat_messages chat_conversations
notification_recipients notifications support_requests vnpay_payment_attempts invoices holidays class_enrollments
member_packages membership_packages class_feedbacks evaluations attendances training_results sessions training_plans
classes rooms coach_specializations specializations subjects members coaches receptionists center_managers email_otps
users centers role_permissions permissions roles attendances_backup_flow4 class_enrollments_backup_flow4
role_permissions_backup_n06 training_results_backup_flow4'''.split()
FUNCTIONS = '''enforce_user_center_assignment fn_classes_validate fn_member_packages_validate fn_room_code_default
fn_rooms_protect fn_scms_current_advanced_only fn_scms_no_legacy_invoice fn_scms_retired_membership_catalog
fn_sessions_validate fn_users_center_guard'''.split()
MIGRATIONS = '''06_schedule_safety_postgres.sql 07_session_subscription_postgres.sql 08_advanced_entitlements_postgres.sql
09_approved_catalog_postgres.sql 10_session_incidents_postgres.sql 11_holiday_history_postgres.sql
12_payment_resolutions_postgres.sql 13_room_catalog_postgres.sql 14_absence_attendance_postgres.sql
15_QueryDR.sql 13_google_login_postgres.sql 17_attendance_review_postgres.sql 18_audit_scope_postgres.sql
19_room_activity_postgres.sql 20_class_publication_price_postgres.sql 21_normal_lifecycle_postgres.sql 22_normal_changes_refunds_postgres.sql'''.split()


def literal(value):
    return "'" + value.replace("'", "''") + "'"


scope = ','.join(map(literal, TABLES))
chunks = ['''-- SCMS DEMO REBUILD — Owner09/11/2026. This file DELETES the listed SCMS demo tables/data.
-- Execute this WHOLE file once, only on the intended SCMS demo DB. Do not run the root additive variant afterwards.
-- Scope: 55 SCMS tables + 4 obsolete SCMS backup tables observed on the current Supabase.
-- auth/storage/realtime/extensions, unlisted tables and unrelated functions are not dropped.
-- External dependencies fail RESTRICT and roll back. This is a reset, not an upgrade preserving demo history.
-- Fresh data: approved 47 demo accounts, 21 classes and explicitly synthetic Normal/Advanced/refund scenarios; no provider calls.
-- invoice_code_seq is NOT restarted: never reuse outward payment references after resetting demo rows.
BEGIN;
SET LOCAL search_path=public,extensions,pg_catalog;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout='10s';
SET LOCAL statement_timeout='180s';
SELECT pg_advisory_xact_lock(391,20261109);
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE TEMP TABLE _scms_reset_before(table_name TEXT PRIMARY KEY,row_count BIGINT) ON COMMIT DROP;
CREATE TEMP TABLE _scms_code_floor(value BIGINT NOT NULL) ON COMMIT DROP;
''', f'''DO $preflight$
DECLARE n TEXT; count_rows BIGINT; highest NUMERIC:=0; seq_value BIGINT:=0; ext RECORD;
BEGIN
 IF to_regclass('public.users') IS NOT NULL AND NOT EXISTS(SELECT 1 FROM information_schema.columns
   WHERE table_schema='public' AND table_name='users' AND column_name='password_hash') THEN
  RAISE EXCEPTION 'SCMS_DEMO_WRONG_TARGET: public.users is not the expected SCMS model';
 END IF;
 FOR ext IN SELECT e.extname,c.relname FROM pg_depend d JOIN pg_extension e ON e.oid=d.refobjid
   JOIN pg_class c ON c.oid=d.objid JOIN pg_namespace ns ON ns.oid=c.relnamespace
   WHERE d.classid='pg_class'::regclass AND d.refclassid='pg_extension'::regclass AND d.deptype='e'
     AND ns.nspname='public' AND c.relname=ANY(ARRAY[{scope}]) LOOP
  RAISE EXCEPTION 'SCMS_DEMO_EXTENSION_OBJECT: %.% belongs to extension %','public',ext.relname,ext.extname;
 END LOOP;
 FOREACH n IN ARRAY ARRAY[{scope}] LOOP
  IF to_regclass('public.'||n) IS NOT NULL THEN
   EXECUTE format('SELECT count(*) FROM public.%I',n) INTO count_rows;
   INSERT INTO _scms_reset_before VALUES(n,count_rows);
  END IF;
 END LOOP;
 IF to_regclass('public.invoice_code_seq') IS NOT NULL THEN
  EXECUTE 'SELECT last_value FROM public.invoice_code_seq' INTO seq_value;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='invoices' AND column_name='invoice_code') THEN
  EXECUTE $q$SELECT coalesce(max(substring(invoice_code from '^INV-[0-9]{{4}}-([0-9]+)$')::numeric),0) FROM public.invoices$q$ INTO highest;
 END IF;
 -- Fresh namespace floor also protects against references whose old demo invoices were deleted previously.
 highest:=greatest(highest,seq_value,floor(extract(epoch FROM clock_timestamp())*1000));
 IF highest>=9223372036854775806 THEN RAISE EXCEPTION 'SCMS_DEMO_INVOICE_COUNTER_RANGE: reconcile counter before reset'; END IF;
 INSERT INTO _scms_code_floor VALUES(highest::bigint);
END $preflight$;
''', 'DROP TABLE IF EXISTS '+',\n '.join('public.'+n for n in TABLES)+' RESTRICT;\n',
         '\n'.join('DROP FUNCTION IF EXISTS public.'+name+'() RESTRICT;' for name in FUNCTIONS)]
manifest = []


def append_source(name, body=None):
    path = ROOT / name
    data = path.read_bytes()
    if body is None:
        body = data.decode('utf-8-sig')
    body = re.sub(r'(?m)^\s*(?:BEGIN;|COMMIT;)\s*$', '', body)
    manifest.append({'path': name, 'sha256': hashlib.sha256(data).hexdigest()})
    chunks.extend(['\n-- SOURCE: '+name+' SHA256 '+manifest[-1]['sha256'], body])


bootstrap = (ROOT/'database/bootstrap/scms_structure_postgres.sql').read_text(encoding='utf-8')
bootstrap = re.sub(r'^--.*\n', '', bootstrap, count=3)
bootstrap = re.sub(r'DO \$empty\$.*?END \$empty\$;', '-- The scoped reset above preserves unrelated public objects.', bootstrap, flags=re.S)
append_source('database/bootstrap/scms_structure_postgres.sql', bootstrap)
for name in MIGRATIONS:
    append_source('database/migrations/'+name)

# Adopt the current retired model directly; never rerun migration16's live data cleanup.
retired = (ROOT/'database/migrations/16_retire_legacy_memberships_postgres.sql').read_text(encoding='utf-8')
retired = retired[retired.index('CREATE OR REPLACE FUNCTION public.fn_scms_retired_membership_catalog()'):retired.index('DO $postflight$')]
append_source('database/migrations/16_retire_legacy_memberships_postgres.sql', retired)
chunks.append('''SET LOCAL search_path=public,extensions,pg_catalog;
-- Remove the obsolete package-catalog trigger; the current Advanced-only guard replaces it.
DROP TRIGGER IF EXISTS trg_member_packages_validate ON public.member_packages;
DROP FUNCTION IF EXISTS public.fn_member_packages_validate() RESTRICT;
CREATE UNIQUE INDEX uq_scms_users_email_ci ON public.users(lower(email));
ALTER TABLE public.classes ADD CONSTRAINT ck_demo_class_total_sessions CHECK(total_sessions=12);
ALTER TABLE public.classes ADD CONSTRAINT ck_demo_class_positive_price CHECK(tuition_fee>0);
ALTER TABLE public.invoices ADD CONSTRAINT ck_demo_invoice_positive_amount CHECK(amount>0);
ALTER TABLE public.sessions ADD CONSTRAINT ck_demo_standard_session_slot CHECK((start_time,end_time) IN
 ((TIME '06:00',TIME '07:30'),(TIME '07:45',TIME '09:15'),(TIME '09:30',TIME '11:00'),
  (TIME '13:00',TIME '14:30'),(TIME '14:45',TIME '16:15'),(TIME '16:30',TIME '18:00'),(TIME '18:15',TIME '19:45')));
''')
append_source('database/bootstrap/scms_demo_reference.sql')
import subprocess
subprocess.run([__import__('sys').executable,str(ROOT/'scripts/build-demo-fixture.py'),'--anchor',args.anchor],check=True,capture_output=True)
append_source('database/bootstrap/scms_demo_scenarios.sql')
chunks.append(f'''-- Revoke direct browser/Data API access to SCMS data; backend owner/explicit service grants remain.
DO $acl$
DECLARE n TEXT; grantee_name TEXT; seq RECORD;
BEGIN
 FOREACH n IN ARRAY ARRAY[{scope}] LOOP
  IF to_regclass('public.'||n) IS NOT NULL THEN
   EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC',n);
   FOREACH grantee_name IN ARRAY ARRAY['anon','authenticated'] LOOP
    IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=grantee_name) THEN
     EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I',n,grantee_name);
    END IF;
   END LOOP;
  END IF;
 END LOOP;
 FOR seq IN SELECT sequencename FROM pg_sequences WHERE schemaname='public' AND
   (sequencename='invoice_code_seq' OR EXISTS(SELECT 1 FROM pg_class c JOIN pg_depend d ON d.objid=c.oid
     JOIN pg_class t ON t.oid=d.refobjid WHERE c.oid=to_regclass('public.'||sequencename) AND t.relname=ANY(ARRAY[{scope}]) AND d.deptype IN ('a','i'))) LOOP
  EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE public.%I FROM PUBLIC',seq.sequencename);
  FOREACH grantee_name IN ARRAY ARRAY['anon','authenticated'] LOOP
   IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=grantee_name) THEN EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE public.%I FROM %I',seq.sequencename,grantee_name); END IF;
  END LOOP;
 END LOOP;
END $acl$;
-- Reset row IDs only where owned by recreated SCMS tables. Payment-reference sequence is separate and monotonic.
DO $ids$
DECLARE r RECORD; seq TEXT; last_id BIGINT;
BEGIN
 FOR r IN SELECT table_name,column_name FROM information_schema.columns WHERE table_schema='public'
   AND table_name=ANY(ARRAY[{scope}]) AND (column_default LIKE 'nextval(%' OR is_identity='YES') LOOP
  seq:=pg_get_serial_sequence('public.'||r.table_name,r.column_name);
  IF seq IS NOT NULL AND seq<>'public.invoice_code_seq' AND seq<>'invoice_code_seq' THEN
   EXECUTE format('SELECT max(%I) FROM public.%I',r.column_name,r.table_name) INTO last_id;
   PERFORM setval(seq::regclass,coalesce(last_id,1),last_id IS NOT NULL);
  END IF;
 END LOOP;
 PERFORM setval('public.invoice_code_seq',greatest((SELECT value FROM _scms_code_floor),(SELECT last_value FROM public.invoice_code_seq)),true);
END $ids$;
INSERT INTO public.scms_data_migration_audit(migration_key,captured_at,before_data,after_data)
VALUES('20261110_demo_rebuild_v3',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',
 (SELECT coalesce(jsonb_object_agg(table_name,row_count),'{{}}'::jsonb)::text FROM _scms_reset_before),
 jsonb_build_object('mode','approved-synthetic-demo','accounts',47,'subjects',9,'rooms',20,'advanced_templates',9,
  'normal_classes',12,'synthetic_fixture',true,'invoice_code_floor',(SELECT value FROM _scms_code_floor))::text);
INSERT INTO public.scms_data_migration_audit VALUES('20261106_retire_legacy_memberships_v1',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',
 '{{"mode":"fresh-demo"}}','{{"mode":"fresh-demo","model_retired":true}}');
DO $postflight$
DECLARE n TEXT; c BIGINT;
BEGIN
 IF (SELECT count(*) FROM users)<>47 OR (SELECT count(*) FROM subjects)<>9 OR (SELECT count(*) FROM rooms)<>20
   OR (SELECT count(*) FROM classes)<>21 OR (SELECT count(*) FROM classes WHERE class_type='Normal')<>12
   OR (SELECT count(*) FROM normal_refunds)<>7 OR (SELECT count(*) FROM normal_refund_documents)<>2
   OR EXISTS(SELECT 1 FROM rooms WHERE activity_type IS NULL) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: approved fixture counts mismatch';
 END IF;
 IF EXISTS(SELECT 1 FROM classes c WHERE c.class_type='Normal' AND
   (c.min_capacity<(2*c.max_capacity+2)/3 OR c.min_capacity>c.max_capacity OR
    (SELECT count(*) FROM sessions s WHERE s.class_id=c.class_id AND NOT s.center_invalidated)<>12)) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: Normal minimum or twelve valid sessions mismatch';
 END IF;
 IF EXISTS(SELECT 1 FROM invoices i WHERE i.amount<=0 OR i.payment_status<>'Paid')
 OR EXISTS(SELECT 1 FROM normal_refunds f JOIN payment_resolutions r ON r.id=f.id JOIN invoices i ON i.invoice_id=r.invoice_id WHERE r.amount<>i.amount OR i.payment_status<>'Paid')
 OR EXISTS(SELECT 1 FROM normal_refunds f JOIN payment_resolutions r ON r.id=f.id JOIN invoices i ON i.invoice_id=r.invoice_id JOIN class_enrollments e ON e.member_id=i.member_id AND e.class_id=f.class_id WHERE e.status<>'Cancelled') THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: receipt/refund/right mismatch';
 END IF;
 FOREACH n IN ARRAY ARRAY['email_otps','vnpay_payment_attempts','membership_packages'] LOOP
  EXECUTE format('SELECT count(*) FROM public.%I',n) INTO c;
  IF c<>0 THEN RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: dynamic/retired data in %',n; END IF;
 END LOOP;
 IF EXISTS(SELECT 1 FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace ns ON ns.oid=t.relnamespace
   WHERE ns.nspname='public' AND t.relname=ANY(ARRAY[{scope}]) AND NOT c.convalidated) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: unvalidated constraint';
 END IF;
END $postflight$;
COMMIT;
SELECT 'PASS' AS result,migration_key,after_data::jsonb AS summary FROM public.scms_data_migration_audit WHERE migration_key='20261110_demo_rebuild_v3';
''')
output = (ROOT/args.output).resolve()
if not output.is_relative_to(ROOT):
    raise ValueError('Output must remain in the workspace')
output.parent.mkdir(parents=True, exist_ok=True)
data=('\n'.join(chunks)+'\n').encode('utf-8')
output.write_bytes(data)
print(json.dumps({'output':str(output.relative_to(ROOT)), 'sha256':hashlib.sha256(data).hexdigest(),
                  'dropTables':len(TABLES), 'sources':len(manifest), 'databaseAccess':False}))
