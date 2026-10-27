BEGIN;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS booking_key VARCHAR(80);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS booking_hash VARCHAR(64);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS created_by INT REFERENCES users(user_id);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS agreement_confirmed_at TIMESTAMP;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS support_reason VARCHAR(2000);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS credit_returned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS center_invalidated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS makeup_for_session_id INT REFERENCES sessions(session_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_booking_key ON sessions(subscription_id,booking_key) WHERE booking_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_makeup_original ON sessions(makeup_for_session_id) WHERE makeup_for_session_id IS NOT NULL;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS deferred_credit_return INT NOT NULL DEFAULT 0;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS approved_extra_nanos INT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS session_absence_notices (
    id SERIAL PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), member_id INT NOT NULL REFERENCES users(user_id),
    reported_at TIMESTAMP NOT NULL, original_start TIMESTAMP NOT NULL, original_end TIMESTAMP NOT NULL,
    reason VARCHAR(2000) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, superseded_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_absence_notice ON session_absence_notices(session_id) WHERE active;
CREATE TABLE IF NOT EXISTS session_changes (
    id SERIAL PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), request_key VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, actor_id INT NOT NULL REFERENCES users(user_id), changed_at TIMESTAMP NOT NULL,
    cause VARCHAR(40) NOT NULL, reason VARCHAR(2000) NOT NULL, member_agreed BOOLEAN NOT NULL,
    before_data VARCHAR(4000) NOT NULL, after_data VARCHAR(4000) NOT NULL, UNIQUE(session_id,request_key)
);
CREATE TABLE IF NOT EXISTS center_incidents (
    id SERIAL PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), request_key VARCHAR(80) NOT NULL,
    started_at TIMESTAMP NOT NULL, ended_at TIMESTAMP, reason VARCHAR(2000) NOT NULL, evidence VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WaitingForRecovery', created_by INT NOT NULL REFERENCES users(user_id), created_at TIMESTAMP NOT NULL,
    resolved_by INT REFERENCES users(user_id), resolved_at TIMESTAMP, original_deadline TIMESTAMP, deadline_before TIMESTAMP, deadline_after TIMESTAMP,
    recovered_credits INT NOT NULL DEFAULT 0, session_compensated BOOLEAN NOT NULL DEFAULT FALSE, UNIQUE(session_id,request_key),
    CHECK(ended_at IS NULL OR ended_at>=started_at)
);
CREATE TABLE IF NOT EXISTS incident_affected_members (
    incident_id INT NOT NULL REFERENCES center_incidents(id), member_id INT NOT NULL REFERENCES users(user_id), PRIMARY KEY(incident_id,member_id)
);
COMMIT;
