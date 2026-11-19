-- SCMS issued attendance codes: additive upgrade, no reset/seed/backfill.
-- Existing Member secrets, replay history, attendance, invoices and credits are preserved.
-- Run the WHOLE file before the matching BE release; a rebuilt bundle already includes it.
BEGIN;
SET LOCAL search_path=public,extensions,pg_catalog;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='60s';

DO $shape$
DECLARE item RECORD;
BEGIN
 IF to_regclass('public.members') IS NULL OR NOT EXISTS(
   SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass('public.members') AND attname='user_id'
     AND atttypid='integer'::regtype AND attnotnull AND NOT attisdropped)
   OR NOT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass('public.members')
     AND attname='checkin_secret' AND atttypid='character varying'::regtype AND atttypmod=68 AND NOT attisdropped)
   OR NOT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass('public.members')
     AND attname='checkin_last_step' AND atttypid='bigint'::regtype AND NOT attisdropped) THEN
  RAISE EXCEPTION 'SCMS_ISSUED_CODES_DEPENDENCY: Existing SCMS Member schema is required';
 END IF;
 FOR item IN SELECT * FROM (VALUES
   ('checkin_code_nonce','bigint'),('checkin_code_issued_at','timestamp without time zone'),
   ('checkin_code_expires_at','timestamp without time zone'),('checkin_code_used_at','timestamp without time zone')
 ) AS expected(name,type_name) LOOP
  IF EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid='public.members'::regclass AND attname=item.name AND NOT attisdropped)
    AND NOT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid='public.members'::regclass AND attname=item.name
      AND atttypid=item.type_name::regtype AND NOT attnotnull AND NOT atthasdef AND NOT attisdropped
      AND (item.type_name='bigint' OR atttypmod IN (-1,6))) THEN
   RAISE EXCEPTION 'SCMS_ISSUED_CODES_SCHEMA: Incompatible type/nullability/default for %',item.name;
  END IF;
 END LOOP;
END $shape$;

ALTER TABLE public.members ADD COLUMN IF NOT EXISTS checkin_code_nonce BIGINT NULL;
ALTER TABLE public.members ADD COLUMN IF NOT EXISTS checkin_code_issued_at TIMESTAMP NULL;
ALTER TABLE public.members ADD COLUMN IF NOT EXISTS checkin_code_expires_at TIMESTAMP NULL;
ALTER TABLE public.members ADD COLUMN IF NOT EXISTS checkin_code_used_at TIMESTAMP NULL;

DO $constraint$
DECLARE expected TEXT := '((checkin_code_nonce IS NULL AND checkin_code_issued_at IS NULL AND checkin_code_expires_at IS NULL AND checkin_code_used_at IS NULL)
 OR (checkin_code_nonce IS NOT NULL AND checkin_secret IS NOT NULL AND checkin_code_issued_at IS NOT NULL AND checkin_code_expires_at IS NOT NULL
 AND checkin_code_expires_at=checkin_code_issued_at+INTERVAL ''300 seconds''
 AND (checkin_code_used_at IS NULL OR (checkin_code_used_at>=checkin_code_issued_at AND checkin_code_used_at<checkin_code_expires_at))))';
 actual_definition TEXT; expected_definition TEXT;
BEGIN
 -- Compare PostgreSQL's own normalized expression, rather than accepting a colliding constraint name.
 IF to_regclass('pg_temp._scms_issued_code_shape26') IS NOT NULL THEN
  RAISE EXCEPTION 'SCMS_ISSUED_CODES_SCHEMA: Unexpected temporary validation object';
 END IF;
 EXECUTE 'CREATE TEMP TABLE _scms_issued_code_shape26(checkin_secret VARCHAR(64),checkin_code_nonce BIGINT,
   checkin_code_issued_at TIMESTAMP,checkin_code_expires_at TIMESTAMP,checkin_code_used_at TIMESTAMP,
   CONSTRAINT _scms_issued_code_expected26 CHECK '||expected||') ON COMMIT DROP';
 SELECT pg_get_constraintdef(oid) INTO expected_definition FROM pg_constraint
   WHERE conrelid='pg_temp._scms_issued_code_shape26'::regclass AND conname='_scms_issued_code_expected26';
 SELECT pg_get_constraintdef(oid) INTO actual_definition FROM pg_constraint
   WHERE conrelid='public.members'::regclass AND conname='ck_members_issued_checkin_code';
 IF actual_definition IS NULL THEN
  EXECUTE 'ALTER TABLE public.members ADD CONSTRAINT ck_members_issued_checkin_code CHECK '||expected;
 ELSIF actual_definition<>expected_definition OR NOT EXISTS(SELECT 1 FROM pg_constraint
     WHERE conrelid='public.members'::regclass AND conname='ck_members_issued_checkin_code' AND contype='c' AND convalidated) THEN
  RAISE EXCEPTION 'SCMS_ISSUED_CODES_SCHEMA: Incompatible issued-code constraint';
 END IF;
END $constraint$;
COMMIT;
