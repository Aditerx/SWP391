-- D15 Owner09/11: lifecycle at end; current no-show state is reviewed after local midnight.
-- Run before deploying the matching BE. No seed, reset, historical notification queue, or attendance edits.
BEGIN;
DO $$ BEGIN
 IF to_regclass('public.sessions') IS NULL OR to_regclass('public.attendances') IS NULL THEN
  RAISE EXCEPTION 'Attendance baseline is required';
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='attendances' AND column_name='superseded_at') THEN
  RAISE EXCEPTION 'Migration14 attendance history baseline is required';
 END IF;
END $$;
ALTER TABLE public.sessions ADD COLUMN IF NOT EXISTS attendance_review_pending BOOLEAN NOT NULL DEFAULT FALSE;
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='sessions'
     AND column_name='attendance_review_pending' AND data_type='boolean' AND is_nullable='NO' AND column_default='false') THEN
  RAISE EXCEPTION 'Incompatible sessions.attendance_review_pending; reconcile before deploy';
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='attendances'
     AND column_name='note' AND data_type='text') THEN
  RAISE EXCEPTION 'Expected existing attendances.note TEXT; reconcile before deploy';
 END IF;
END $$;
-- FALSE for every existing row; replay preserves pending reviews created by the new finalizer.
CREATE INDEX IF NOT EXISTS idx_sessions_attendance_review_pending
 ON public.sessions(session_date, session_id) WHERE attendance_review_pending = TRUE;
COMMIT;
