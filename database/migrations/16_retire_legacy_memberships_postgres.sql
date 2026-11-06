-- SCMS / Owner 2026-11-06: retire obsolete shared-membership DATA, not Advanced rights.
-- Copy this ENTIRE file into the intended Supabase SQL Editor and execute once.
-- No DROP TABLE, TRUNCATE, CASCADE, credential, seed or amount/status rewrite.
-- Ambiguous/current references STOP and ROLLBACK the entire transaction.
-- Old-model invoices and their direct payment-case/attempt rows are deleted ONLY
-- when the guarded old-model target below is unambiguous. No business-row archive.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '120s';
SET LOCAL search_path = public, pg_catalog;
SELECT pg_advisory_xact_lock(hashtext('scms_retire_legacy_memberships_v1'));

DO $preflight$
DECLARE name text; fk record;
BEGIN
  FOREACH name IN ARRAY ARRAY['centers','users','members','membership_packages','member_packages',
    'invoices','sessions','attendances','training_results','class_enrollments','advanced_registrations',
    'subscription_pauses','vnpay_payment_attempts','payment_resolutions','scms_data_migration_audit'] LOOP
    IF to_regclass('public.' || name) IS NULL THEN
      RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_PREFLIGHT: missing table %; nothing deleted', name;
    END IF;
  END LOOP;
  IF EXISTS (SELECT 1 FROM (VALUES
    ('member_packages','subject_id'),('member_packages','primary_coach_id'),
    ('member_packages','package_id'),('invoices','subscription_id'),('invoices','package_id'),
    ('class_enrollments','eligibility_subscription_id')) required(t,c)
    WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema='public' AND table_name=required.t AND column_name=required.c)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_PREFLIGHT: current Advanced schema is required; nothing deleted';
  END IF;
  -- Reject unknown/cascading incoming FK graphs before any business mutation.
  FOR fk IN SELECT ns.nspname, src.relname, a.attname, dst.relname AS target, c.confdeltype,
                   cardinality(c.conkey) AS key_count
      FROM pg_constraint c JOIN pg_class src ON src.oid=c.conrelid
      JOIN pg_namespace ns ON ns.oid=src.relnamespace
      JOIN pg_class dst ON dst.oid=c.confrelid
      JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
      WHERE c.contype='f' AND c.confrelid IN ('public.membership_packages'::regclass,
        'public.member_packages'::regclass,'public.invoices'::regclass)
  LOOP
    IF fk.nspname<>'public' OR fk.key_count<>1 OR fk.confdeltype NOT IN ('a','r') OR
       (fk.relname,fk.attname,fk.target) NOT IN (
        ('member_packages','package_id','membership_packages'),('invoices','package_id','membership_packages'),
        ('sessions','subscription_id','member_packages'),('invoices','subscription_id','member_packages'),
        ('advanced_registrations','subscription_id','member_packages'),('subscription_pauses','subscription_id','member_packages'),
        ('class_enrollments','eligibility_subscription_id','member_packages'),
        ('vnpay_payment_attempts','invoice_id','invoices'),('payment_resolutions','invoice_id','invoices')) THEN
      RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_PREFLIGHT: unknown or cascading FK %.% -> %; nothing deleted',
        fk.relname,fk.attname,fk.target;
    END IF;
  END LOOP;
END $preflight$;

-- Match application lock direction: serialize behind existing center writers first.
LOCK TABLE public.centers IN SHARE ROW EXCLUSIVE MODE;
SELECT center_id FROM public.centers ORDER BY center_id FOR UPDATE;
LOCK TABLE public.membership_packages, public.member_packages, public.invoices,
  public.sessions, public.class_enrollments, public.advanced_registrations,
  public.subscription_pauses, public.vnpay_payment_attempts, public.payment_resolutions
  IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE _scms_legacy_subs ON COMMIT DROP AS
  SELECT subscription_id,member_id FROM public.member_packages
  WHERE subject_id IS NULL AND primary_coach_id IS NULL AND package_id IS NOT NULL;
ALTER TABLE _scms_legacy_subs ADD PRIMARY KEY(subscription_id);
CREATE TEMP TABLE _scms_legacy_invoices ON COMMIT DROP AS
  SELECT i.invoice_id FROM public.invoices i
  WHERE i.subscription_id IN (SELECT subscription_id FROM _scms_legacy_subs)
     OR (i.package_id IS NOT NULL AND i.subscription_id IS NULL AND i.class_id IS NULL);
ALTER TABLE _scms_legacy_invoices ADD PRIMARY KEY(invoice_id);

DO $classification$
BEGIN
  IF EXISTS (SELECT 1 FROM public.member_packages p
    WHERE NOT (p.subject_id IS NOT NULL AND p.primary_coach_id IS NOT NULL)
      AND NOT EXISTS (SELECT 1 FROM _scms_legacy_subs old WHERE old.subscription_id=p.subscription_id)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_BLOCKED: subscription with missing/partial subject-Coach and no unambiguous legacy catalog; nothing deleted';
  END IF;
  IF EXISTS (SELECT 1 FROM public.sessions s JOIN _scms_legacy_subs old USING(subscription_id))
    OR EXISTS (SELECT 1 FROM public.class_enrollments e JOIN _scms_legacy_subs old ON old.subscription_id=e.eligibility_subscription_id)
    OR EXISTS (SELECT 1 FROM public.advanced_registrations r JOIN _scms_legacy_subs old USING(subscription_id))
    OR EXISTS (SELECT 1 FROM public.subscription_pauses p JOIN _scms_legacy_subs old USING(subscription_id)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_BLOCKED: legacy subscription is linked to sessions/enrollments/Advanced registration/pauses; inspect links before deleting, nothing deleted';
  END IF;
  IF EXISTS (SELECT 1 FROM public.invoices i JOIN _scms_legacy_invoices old USING(invoice_id)
      LEFT JOIN _scms_legacy_subs p ON p.subscription_id=i.subscription_id
      WHERE i.class_id IS NOT NULL OR (i.subscription_id IS NOT NULL AND (p.subscription_id IS NULL OR p.member_id<>i.member_id))) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_BLOCKED: invoice mixes legacy and current course/owner links; nothing deleted';
  END IF;
  IF EXISTS (SELECT 1 FROM public.invoices i JOIN public.member_packages p USING(subscription_id)
      WHERE i.package_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM _scms_legacy_invoices old WHERE old.invoice_id=i.invoice_id)
        AND (p.subject_id IS NULL OR p.primary_coach_id IS NULL OR i.class_id IS NOT NULL OR i.member_id<>p.member_id)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_BLOCKED: ambiguous current invoice/catalog link; nothing deleted';
  END IF;
  IF EXISTS (SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261106_retire_legacy_memberships_v1')
    AND (EXISTS(SELECT 1 FROM _scms_legacy_subs) OR EXISTS(SELECT 1 FROM _scms_legacy_invoices)
      OR EXISTS(SELECT 1 FROM public.membership_packages)
      OR EXISTS(SELECT 1 FROM public.member_packages WHERE package_id IS NOT NULL)
      OR EXISTS(SELECT 1 FROM public.invoices WHERE package_id IS NOT NULL)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_REPLAY_CONFLICT: obsolete data reappeared after retirement; nothing deleted';
  END IF;
END $classification$;

-- Temporary integrity witnesses vanish at COMMIT; only counts enter migration metadata.
CREATE TEMP TABLE _scms_keep_subs ON COMMIT DROP AS
  SELECT subscription_id,to_jsonb(p)-'package_id' AS data FROM public.member_packages p
  WHERE NOT EXISTS(SELECT 1 FROM _scms_legacy_subs old WHERE old.subscription_id=p.subscription_id);
CREATE TEMP TABLE _scms_keep_invoices ON COMMIT DROP AS
  SELECT invoice_id,to_jsonb(i)-'package_id' AS data FROM public.invoices i
  WHERE NOT EXISTS(SELECT 1 FROM _scms_legacy_invoices old WHERE old.invoice_id=i.invoice_id);
CREATE TEMP TABLE _scms_keep_attempts ON COMMIT DROP AS
  SELECT attempt_id,to_jsonb(a) AS data FROM public.vnpay_payment_attempts a
  WHERE NOT EXISTS(SELECT 1 FROM _scms_legacy_invoices old WHERE old.invoice_id=a.invoice_id);
CREATE TEMP TABLE _scms_keep_resolutions ON COMMIT DROP AS
  SELECT id,to_jsonb(r) AS data FROM public.payment_resolutions r
  WHERE NOT EXISTS(SELECT 1 FROM _scms_legacy_invoices old WHERE old.invoice_id=r.invoice_id);
CREATE TEMP TABLE _scms_unchanged(table_name text PRIMARY KEY,digest text) ON COMMIT DROP;
DO $witness$
DECLARE name text; digest text;
BEGIN
  FOREACH name IN ARRAY ARRAY['centers','users','members','subjects','classes','rooms','sessions','attendances','training_results',
    'class_enrollments','advanced_registrations','subscription_pauses'] LOOP
    EXECUTE format('SELECT md5(coalesce(string_agg(to_jsonb(t)::text,'','' ORDER BY to_jsonb(t)::text),'''')) FROM public.%I t',name) INTO digest;
    INSERT INTO _scms_unchanged VALUES(name,digest);
  END LOOP;
END $witness$;

CREATE TEMP TABLE _scms_retire_counts(data jsonb) ON COMMIT DROP;
INSERT INTO _scms_retire_counts SELECT jsonb_build_object(
  'deleted_catalog',(SELECT count(*) FROM public.membership_packages),
  'deleted_legacy_subscriptions',(SELECT count(*) FROM _scms_legacy_subs),
  'deleted_legacy_invoices',(SELECT count(*) FROM _scms_legacy_invoices),
  'deleted_legacy_payment_attempts',(SELECT count(*) FROM public.vnpay_payment_attempts WHERE invoice_id IN(SELECT invoice_id FROM _scms_legacy_invoices)),
  'deleted_legacy_resolutions',(SELECT count(*) FROM public.payment_resolutions WHERE invoice_id IN(SELECT invoice_id FROM _scms_legacy_invoices)),
  'detached_current_subscriptions',(SELECT count(*) FROM public.member_packages WHERE package_id IS NOT NULL AND subject_id IS NOT NULL AND primary_coach_id IS NOT NULL),
  'detached_current_invoices',(SELECT count(*) FROM public.invoices WHERE package_id IS NOT NULL AND invoice_id IN(SELECT invoice_id FROM _scms_keep_invoices)),
  'model_retired',true);

-- Preserve current subject/Coach entitlements and their receipts; remove catalog FK only.
UPDATE public.member_packages SET package_id=NULL
  WHERE package_id IS NOT NULL AND subject_id IS NOT NULL AND primary_coach_id IS NOT NULL;
UPDATE public.invoices SET package_id=NULL
  WHERE package_id IS NOT NULL AND invoice_id IN(SELECT invoice_id FROM _scms_keep_invoices);
DELETE FROM public.payment_resolutions WHERE invoice_id IN(SELECT invoice_id FROM _scms_legacy_invoices);
DELETE FROM public.vnpay_payment_attempts WHERE invoice_id IN(SELECT invoice_id FROM _scms_legacy_invoices);
DELETE FROM public.invoices WHERE invoice_id IN(SELECT invoice_id FROM _scms_legacy_invoices);
DELETE FROM public.member_packages WHERE subscription_id IN(SELECT subscription_id FROM _scms_legacy_subs);
DELETE FROM public.membership_packages;

CREATE OR REPLACE FUNCTION public.fn_scms_retired_membership_catalog() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: shared-membership catalog is retired'; END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_retired_membership_catalog
  BEFORE INSERT OR UPDATE ON public.membership_packages FOR EACH ROW EXECUTE FUNCTION public.fn_scms_retired_membership_catalog();
CREATE OR REPLACE FUNCTION public.fn_scms_current_advanced_only() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN
  IF NEW.package_id IS NOT NULL OR NEW.subject_id IS NULL OR NEW.primary_coach_id IS NULL THEN
    RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: entitlement requires subject and Primary Coach, without legacy package';
  END IF; RETURN NEW;
END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_current_advanced_only
  BEFORE INSERT OR UPDATE ON public.member_packages FOR EACH ROW EXECUTE FUNCTION public.fn_scms_current_advanced_only();
CREATE OR REPLACE FUNCTION public.fn_scms_no_legacy_invoice() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN
  IF NEW.package_id IS NOT NULL THEN RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: invoice cannot target a legacy catalog package'; END IF;
  RETURN NEW;
END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_no_legacy_invoice
  BEFORE INSERT OR UPDATE ON public.invoices FOR EACH ROW EXECUTE FUNCTION public.fn_scms_no_legacy_invoice();

DO $postflight$
DECLARE row record; actual text;
BEGIN
  IF EXISTS(SELECT 1 FROM public.membership_packages)
    OR EXISTS(SELECT 1 FROM public.member_packages WHERE package_id IS NOT NULL OR subject_id IS NULL OR primary_coach_id IS NULL)
    OR EXISTS(SELECT 1 FROM public.invoices WHERE package_id IS NOT NULL) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_POSTFLIGHT: obsolete data remains; entire transaction rolled back';
  END IF;
  IF (SELECT count(*) FROM public.member_packages)<>(SELECT count(*) FROM _scms_keep_subs)
    OR (SELECT count(*) FROM public.invoices)<>(SELECT count(*) FROM _scms_keep_invoices)
    OR EXISTS(SELECT 1 FROM _scms_keep_subs k LEFT JOIN public.member_packages p USING(subscription_id)
      WHERE p.subscription_id IS NULL OR k.data IS DISTINCT FROM to_jsonb(p)-'package_id')
    OR EXISTS(SELECT 1 FROM _scms_keep_invoices k LEFT JOIN public.invoices i USING(invoice_id)
      WHERE i.invoice_id IS NULL OR k.data IS DISTINCT FROM to_jsonb(i)-'package_id') THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_POSTFLIGHT: current entitlement/receipt changed unexpectedly; rolled back';
  END IF;
  IF (SELECT count(*) FROM public.vnpay_payment_attempts)<>(SELECT count(*) FROM _scms_keep_attempts)
    OR (SELECT count(*) FROM public.payment_resolutions)<>(SELECT count(*) FROM _scms_keep_resolutions)
    OR EXISTS(SELECT 1 FROM _scms_keep_attempts k LEFT JOIN public.vnpay_payment_attempts a USING(attempt_id)
      WHERE a.attempt_id IS NULL OR k.data IS DISTINCT FROM to_jsonb(a))
    OR EXISTS(SELECT 1 FROM _scms_keep_resolutions k LEFT JOIN public.payment_resolutions r USING(id)
      WHERE r.id IS NULL OR k.data IS DISTINCT FROM to_jsonb(r)) THEN
    RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_POSTFLIGHT: current payment attempt/resolution changed; rolled back';
  END IF;
  FOR row IN SELECT * FROM _scms_unchanged LOOP
    EXECUTE format('SELECT md5(coalesce(string_agg(to_jsonb(t)::text,'','' ORDER BY to_jsonb(t)::text),'''')) FROM public.%I t',row.table_name) INTO actual;
    IF actual IS DISTINCT FROM row.digest THEN RAISE EXCEPTION 'SCMS_LEGACY_RETIRE_POSTFLIGHT: protected table % changed; rolled back',row.table_name; END IF;
  END LOOP;
END $postflight$;

INSERT INTO public.scms_data_migration_audit(migration_key,captured_at,before_data,after_data)
  SELECT '20261106_retire_legacy_memberships_v1',CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh',
    data::text,data::text FROM _scms_retire_counts
  ON CONFLICT(migration_key) DO NOTHING;
COMMIT;

-- Expected final result: exactly one row, result=PASS, model_retired=true.
SELECT 'PASS' AS result,migration_key,after_data::jsonb AS summary
FROM public.scms_data_migration_audit WHERE migration_key='20261106_retire_legacy_memberships_v1';
