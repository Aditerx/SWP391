-- Owner31/10, implemented02/11: Absent + notice atomically; retain superseded history.
BEGIN;
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
