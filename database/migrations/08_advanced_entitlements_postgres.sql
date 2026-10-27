-- Additive upgrade. Run only after 06_schedule_safety and 07; no seed/reset/backfill guesses.
BEGIN;
-- New Advanced registrations do not use a generic package or start validity before Paid.
ALTER TABLE member_packages ALTER COLUMN package_id DROP NOT NULL;
ALTER TABLE member_packages ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE member_packages ALTER COLUMN end_date DROP NOT NULL;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS subject_id INT REFERENCES subjects(subject_id);
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS primary_coach_id INT REFERENCES users(user_id);
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS purchase_amount NUMERIC(12,2);
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS expiry_revoked_credit INT DEFAULT 0;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS expiry_processed_at TIMESTAMP;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS approved_extra_seconds BIGINT NOT NULL DEFAULT 0;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoice_target;
ALTER TABLE invoices ADD CONSTRAINT chk_invoice_target CHECK (
    (class_id IS NOT NULL AND package_id IS NULL AND subscription_id IS NULL) OR
    (class_id IS NULL AND (package_id IS NOT NULL OR subscription_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_subscription_primary_coach ON member_packages(primary_coach_id);
CREATE INDEX IF NOT EXISTS idx_subscription_deadline ON member_packages(status, expires_at);
CREATE TABLE IF NOT EXISTS subscription_pauses (
    id SERIAL PRIMARY KEY, subscription_id INT NOT NULL REFERENCES member_packages(subscription_id),
    source_key VARCHAR(120) NOT NULL, starts_at TIMESTAMP NOT NULL, ends_at TIMESTAMP NOT NULL,
    UNIQUE(subscription_id,source_key), CHECK(ends_at > starts_at)
);
CREATE TABLE IF NOT EXISTS coach_work_schedules (
    coach_id INT PRIMARY KEY REFERENCES users(user_id), accepting_new BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS coach_work_slots (
    coach_id INT NOT NULL REFERENCES coach_work_schedules(coach_id), slot_key VARCHAR(20) NOT NULL,
    PRIMARY KEY(coach_id,slot_key)
);
CREATE TABLE IF NOT EXISTS coach_days_off (
    coach_id INT NOT NULL REFERENCES coach_work_schedules(coach_id), day_off DATE NOT NULL,
    PRIMARY KEY(coach_id,day_off)
);
CREATE TABLE IF NOT EXISTS advanced_registrations (
    id SERIAL PRIMARY KEY, member_id INT NOT NULL REFERENCES users(user_id),
    subject_id INT NOT NULL REFERENCES subjects(subject_id), coach_id INT NOT NULL REFERENCES users(user_id),
    request_key VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Requested', created_at TIMESTAMP NOT NULL,
    accepted_by INT REFERENCES users(user_id), accepted_at TIMESTAMP, reason VARCHAR(2000),
    subscription_id INT UNIQUE REFERENCES member_packages(subscription_id), UNIQUE(member_id,request_key)
);
COMMIT;
