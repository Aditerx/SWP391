-- RUN 2 OF 2: 0212-2.sql. Paste this ENTIRE file into Supabase SQL Editor.
-- Run ONLY after 0211-1.sql succeeds. This file enforces that order.
-- Contains migration14 + read-only schema postflight. No historical backfill.
-- Deploy the new BE only after BOTH files and postflight succeed.
-- Canonical source: database/migrations/14_absence_attendance_postgres.sql.
-- Temporary root copy: may be deleted after successful execution/postflight.
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';

DO $package_order$
BEGIN
 IF to_regclass('public.scms_data_migration_audit') IS NULL THEN
  RAISE EXCEPTION 'RUN_0211_1_FIRST: 0211-1.sql must succeed before 0212-2.sql';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261102_room_catalog_v1') THEN
  RAISE EXCEPTION 'RUN_0211_1_FIRST: room migration marker is missing; do not skip file1 guards';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261102_owner_normal_class_capacity_v1') THEN
  RAISE EXCEPTION 'RUN_0211_1_FIRST: approved class-capacity audit marker is missing; run the latest 0211-1.sql';
 END IF;
END $package_order$;

-- Owner31/10, implemented02/11: Absent + notice atomically; retain superseded history.
LOCK TABLE attendances IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS absence_notice_id INT;
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS superseded_at TIMESTAMP;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='attendances'::regclass AND conname='fk_attendance_absence_notice') THEN
  ALTER TABLE attendances ADD CONSTRAINT fk_attendance_absence_notice FOREIGN KEY(absence_notice_id) REFERENCES session_absence_notices(id);
 END IF;
END $$;
-- Exactly one current attendance; archived automatic records remain traceable.
DROP INDEX IF EXISTS ux_attendance_session_member;
CREATE UNIQUE INDEX ux_attendance_session_member ON attendances(session_id,member_id)
 WHERE session_id IS NOT NULL AND superseded_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_attendance_absence_notice ON attendances(absence_notice_id) WHERE absence_notice_id IS NOT NULL;
COMMIT;

SELECT '0212-2 SUCCESS: migration14 committed; DB schema ready for new BE' AS result;
SELECT column_name,data_type FROM information_schema.columns
WHERE table_schema='public' AND table_name='attendances'
 AND column_name IN ('absence_notice_id','superseded_at') ORDER BY column_name;
SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='public'
 AND tablename='attendances' AND indexname IN ('ux_attendance_session_member','idx_attendance_absence_notice') ORDER BY indexname;
