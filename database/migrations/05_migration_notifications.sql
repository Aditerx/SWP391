-- Additive, repeatable notification upgrade. Legacy notifications and recipients are preserved.
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS notification_type VARCHAR(40);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS related_type VARCHAR(40);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS related_id INT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(180);

ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS read_at TIMESTAMP NULL;
ALTER TABLE notification_recipients ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_dedupe_key
    ON notifications (dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notification_recipients_feed
    ON notification_recipients (user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications (created_at DESC, notification_id DESC);
