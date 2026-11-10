-- RP-43: event-time provenance for new audit events. Existing history stays unchanged.
BEGIN;
DO $$ BEGIN
 IF to_regclass('public.system_logs') IS NULL THEN RAISE EXCEPTION 'system_logs baseline is required'; END IF;
END $$;
ALTER TABLE public.system_logs ADD COLUMN IF NOT EXISTS audit_category VARCHAR(24);
ALTER TABLE public.system_logs ADD COLUMN IF NOT EXISTS center_id INTEGER;
ALTER TABLE public.system_logs ADD COLUMN IF NOT EXISTS scope_source VARCHAR(40);
DO $$ BEGIN
 IF (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='system_logs'
     AND ((column_name='audit_category' AND data_type='character varying' AND character_maximum_length=24 AND is_nullable='YES')
       OR (column_name='center_id' AND data_type='integer' AND is_nullable='YES')
       OR (column_name='scope_source' AND data_type='character varying' AND character_maximum_length=40 AND is_nullable='YES'))) <> 3 THEN
  RAISE EXCEPTION 'Incompatible system_logs provenance columns; reconcile before deploy';
 END IF;
 IF EXISTS(SELECT 1 FROM public.system_logs WHERE audit_category NOT IN ('BUSINESS','SYSTEM_SECURITY')) THEN
  RAISE EXCEPTION 'Unknown audit category; reconcile before deploy';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.system_logs'::regclass AND conname='ck_system_logs_category') THEN
  ALTER TABLE public.system_logs ADD CONSTRAINT ck_system_logs_category CHECK(audit_category IN ('BUSINESS','SYSTEM_SECURITY'));
 END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_system_logs_scoped_page
 ON public.system_logs(audit_category, center_id, timestamp DESC, log_id DESC);
-- No backfill from mutable actor/target data, no deletion and no fabricated historical center.
COMMIT;
