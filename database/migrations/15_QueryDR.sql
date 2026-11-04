-- SCMS QueryDR, Owner approved 04/11/2026.
-- Copy this ENTIRE file into the Supabase SQL editor and run once before deploying BE.
-- Additive only: no existing table/column/data is updated, deleted or rebuilt.
-- A failed guard rolls back this whole transaction. Replay validates the same structure.
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';
SET LOCAL search_path = public, pg_catalog;
SELECT pg_advisory_xact_lock(391, 15);

DO $preflight$
BEGIN
  IF to_regclass('public.invoices') IS NULL OR NOT EXISTS (
    SELECT 1 FROM pg_attribute
    WHERE attrelid=to_regclass('public.invoices') AND attname='invoice_id'
      AND atttypid='integer'::regtype AND attnotnull AND NOT attisdropped
  ) THEN
    RAISE EXCEPTION 'QUERYDR_PREFLIGHT: public.invoices(invoice_id integer NOT NULL) is required';
  END IF;
  IF to_regclass('public.vnpay_payment_attempts') IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM pg_class WHERE oid=to_regclass('public.vnpay_payment_attempts') AND relkind='r'
  ) THEN
    RAISE EXCEPTION 'QUERYDR_PREFLIGHT: existing vnpay_payment_attempts is not an ordinary table';
  END IF;
END $preflight$;

-- QUERYDR_TABLE_BEGIN
CREATE TABLE IF NOT EXISTS public.vnpay_payment_attempts (
  attempt_id BIGSERIAL PRIMARY KEY,
  invoice_id INTEGER NOT NULL,
  tmn_code VARCHAR(32) NOT NULL,
  txn_ref VARCHAR(100) NOT NULL,
  transaction_date VARCHAR(14) NOT NULL,
  amount_minor BIGINT NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  last_query_at TIMESTAMP WITHOUT TIME ZONE NULL,
  CONSTRAINT fk_vnpay_attempt_invoice FOREIGN KEY(invoice_id)
    REFERENCES public.invoices(invoice_id) ON DELETE RESTRICT,
  CONSTRAINT uq_vnpay_attempt_request UNIQUE(invoice_id,tmn_code,txn_ref,transaction_date),
  CONSTRAINT chk_vnpay_attempt_tmn CHECK(tmn_code ~ '^[A-Za-z0-9_-]{1,32}$'),
  CONSTRAINT chk_vnpay_attempt_ref CHECK(length(txn_ref) BETWEEN 1 AND 100 AND txn_ref !~ '[[:cntrl:]]'),
  CONSTRAINT chk_vnpay_attempt_date CHECK(transaction_date ~ '^[0-9]{14}$'),
  CONSTRAINT chk_vnpay_attempt_amount CHECK(amount_minor BETWEEN 1 AND 999999999999)
);
-- QUERYDR_TABLE_END

-- Validate an existing relation before changing even this new table's permissions/indexes.
DO $shape$
DECLARE
  expected RECORD;
  actual_type TEXT;
  actual_required BOOLEAN;
  cols SMALLINT[];
BEGIN
  IF (SELECT count(*) FROM pg_attribute WHERE attrelid='public.vnpay_payment_attempts'::regclass
      AND attnum>0 AND NOT attisdropped) <> 8 THEN
    RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: unexpected columns; no existing data was modified';
  END IF;
  FOR expected IN SELECT * FROM (VALUES
    ('attempt_id','bigint',true),('invoice_id','integer',true),
    ('tmn_code','character varying(32)',true),('txn_ref','character varying(100)',true),
    ('transaction_date','character varying(14)',true),('amount_minor','bigint',true),
    ('created_at','timestamp without time zone',true),('last_query_at','timestamp without time zone',false)
  ) AS t(name,type_name,required) LOOP
    SELECT format_type(atttypid,atttypmod),attnotnull INTO actual_type,actual_required
    FROM pg_attribute WHERE attrelid='public.vnpay_payment_attempts'::regclass
      AND attname=expected.name AND attnum>0 AND NOT attisdropped;
    IF actual_type IS DISTINCT FROM expected.type_name OR actual_required IS DISTINCT FROM expected.required THEN
      RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: incompatible column %',expected.name;
    END IF;
  END LOOP;
  SELECT array_agg(attnum ORDER BY array_position(ARRAY['invoice_id','tmn_code','txn_ref','transaction_date'],attname::text))
    INTO cols FROM pg_attribute WHERE attrelid='public.vnpay_payment_attempts'::regclass
      AND attname=ANY(ARRAY['invoice_id','tmn_code','txn_ref','transaction_date']) AND NOT attisdropped;
  IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.vnpay_payment_attempts'::regclass
      AND contype='u' AND conname='uq_vnpay_attempt_request' AND conkey=cols) THEN
    RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: request identity unique constraint is missing/different';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
      WHERE c.conrelid='public.vnpay_payment_attempts'::regclass AND c.contype='p'
      AND cardinality(c.conkey)=1 AND a.attname='attempt_id') OR NOT EXISTS (
    SELECT 1 FROM pg_attrdef d JOIN pg_attribute a ON a.attrelid=d.adrelid AND a.attnum=d.adnum
    WHERE a.attrelid='public.vnpay_payment_attempts'::regclass AND a.attname='attempt_id'
      AND pg_get_expr(d.adbin,d.adrelid) LIKE 'nextval(%'
  ) THEN
    RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: generated attempt primary key is missing/different';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM pg_constraint c
      JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
      JOIN pg_attribute b ON b.attrelid=c.confrelid AND b.attnum=c.confkey[1]
      WHERE c.conrelid='public.vnpay_payment_attempts'::regclass AND c.contype='f'
        AND c.conname='fk_vnpay_attempt_invoice' AND c.confrelid='public.invoices'::regclass
        AND cardinality(c.conkey)=1 AND a.attname='invoice_id' AND b.attname='invoice_id'
        AND c.confdeltype='r' AND c.convalidated) THEN
    RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: invoice RESTRICT foreign key is missing/different';
  END IF;
  FOR expected IN SELECT * FROM (VALUES
    ('chk_vnpay_attempt_tmn', $d$CHECK (((tmn_code)::text ~ '^[A-Za-z0-9_-]{1,32}$'::text))$d$),
    ('chk_vnpay_attempt_ref', $d$CHECK ((((length((txn_ref)::text) >= 1) AND (length((txn_ref)::text) <= 100)) AND ((txn_ref)::text !~ '[[:cntrl:]]'::text)))$d$),
    ('chk_vnpay_attempt_date', $d$CHECK (((transaction_date)::text ~ '^[0-9]{14}$'::text))$d$),
    ('chk_vnpay_attempt_amount', $d$CHECK (((amount_minor >= 1) AND (amount_minor <= '999999999999'::bigint)))$d$)
  ) AS t(name,definition) LOOP
    IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.vnpay_payment_attempts'::regclass
      AND contype='c' AND convalidated AND conname=expected.name
      AND pg_get_constraintdef(oid)=expected.definition) THEN
      RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: missing/different request guard %',expected.name;
    END IF;
  END LOOP;
END $shape$;

CREATE INDEX IF NOT EXISTS idx_vnpay_attempt_invoice_query
  ON public.vnpay_payment_attempts(invoice_id,last_query_at,attempt_id DESC);
DO $index_guard$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('public.idx_vnpay_attempt_invoice_query')
      AND indrelid='public.vnpay_payment_attempts'::regclass AND indisvalid AND NOT indisunique
      AND pg_get_indexdef(indexrelid) LIKE '% USING btree (invoice_id, last_query_at, attempt_id DESC)') THEN
    RAISE EXCEPTION 'QUERYDR_SCHEMA_CONFLICT: query index is missing/different';
  END IF;
END $index_guard$;
ALTER TABLE public.vnpay_payment_attempts ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.vnpay_payment_attempts FROM PUBLIC;
REVOKE ALL ON SEQUENCE public.vnpay_payment_attempts_attempt_id_seq FROM PUBLIC;
DO $roles$
BEGIN
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
    REVOKE ALL ON public.vnpay_payment_attempts FROM anon;
    REVOKE ALL ON SEQUENCE public.vnpay_payment_attempts_attempt_id_seq FROM anon;
  END IF;
  IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
    REVOKE ALL ON public.vnpay_payment_attempts FROM authenticated;
    REVOKE ALL ON SEQUENCE public.vnpay_payment_attempts_attempt_id_seq FROM authenticated;
  END IF;
END $roles$;
COMMENT ON TABLE public.vnpay_payment_attempts IS
  'BE-only original VNPay pay requests for QueryDR. Stores no key/full URL. Existing invoices are not backfilled or changed.';
COMMENT ON COLUMN public.vnpay_payment_attempts.transaction_date IS
  'Exact vnp_CreateDate sent in the original pay request, yyyyMMddHHmmss Asia/Ho_Chi_Minh; not invoice.created_at or gateway PayDate.';
COMMENT ON COLUMN public.vnpay_payment_attempts.last_query_at IS
  'Clock-based query claim time; used under invoice/center lock for cooldown. Does not mean payment succeeded.';

SELECT 'QUERYDR_SCHEMA_READY' AS migration_status,
  (SELECT count(*) FROM public.vnpay_payment_attempts) AS preserved_attempt_rows;
COMMIT;
