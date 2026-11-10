-- RP-13: only new, unpublished Normal drafts may choose their first publication price.
BEGIN;
DO $$ BEGIN
 IF to_regclass('public.classes') IS NULL THEN RAISE EXCEPTION 'classes baseline is required'; END IF;
END $$;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS pricing_draft BOOLEAN NOT NULL DEFAULT FALSE;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes'
     AND column_name='pricing_draft' AND data_type='boolean' AND is_nullable='NO' AND column_default='false') THEN
  RAISE EXCEPTION 'Incompatible classes.pricing_draft; reconcile before deploy';
 END IF;
END $$;
-- No guessing whether legacy Closed classes were unpublished. No price/invoice/history edits.
-- Replay preserves drafts created by the matching BE.
COMMIT;
