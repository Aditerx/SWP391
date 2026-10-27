-- Existing-schema upgrade only; no bootstrap/reset or entitlement backfill.
BEGIN;
SET LOCAL TIME ZONE 'UTC';
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='30s';
LOCK TABLE public.subjects, public.users, public.invoices, public.member_packages, public.class_enrollments, public.classes, public.sessions, public.membership_packages IN SHARE ROW EXCLUSIVE MODE;
DO $guard$ BEGIN
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "subject_id"),'')) FROM (SELECT "subject_id","subject_name","description","status","slug","normal_price","advanced_price","outdoor" FROM public."subjects") t) <> 'f7f4cc294bff2a2d8acbc583d6dfe369' THEN RAISE EXCEPTION 'Catalog changed; refresh reviewed backup'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "user_id"),'')) FROM (SELECT "user_id","role_id","full_name","email","phone","password_hash","address","gender","date_of_birth","status","created_at","failed_login_attempts","locked_until","is_first_login","avatar_url","avatar_public_id","center_id" FROM public."users") t) <> 'ed86323db9f2cefb858416d87032859b' THEN RAISE EXCEPTION 'Original rows changed: users'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "invoice_id"),'')) FROM (SELECT "invoice_id","member_id","package_id","receptionist_id","amount","payment_method","payment_status","payment_date","gateway_transaction_ref","class_id","subscription_id","invoice_code","created_at","expires_at","reconciliation_required" FROM public."invoices") t) <> '9fd65f291f73175e182b6a0a0ec789ac' THEN RAISE EXCEPTION 'Original rows changed: invoices'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "subscription_id"),'')) FROM (SELECT "subscription_id","member_id","package_id","start_date","end_date","status","remaining_credit","validity_days" FROM public."member_packages") t) <> '888571012baca09c7a17997c9f384f4e' THEN RAISE EXCEPTION 'Original rows changed: member_packages'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "enrollment_id"),'')) FROM (SELECT "enrollment_id","member_id","class_id","enrolled_at","status","cancelled_at","cancelled_by","cancel_reason","promoted_at" FROM public."class_enrollments") t) <> 'ee3236300fa6f79a2b117191a71cea4e' THEN RAISE EXCEPTION 'Original rows changed: class_enrollments'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "class_id"),'')) FROM (SELECT "class_id","class_name","subject_id","coach_id","max_capacity","start_date","end_date","status","center_id","tuition_fee","class_type","total_sessions" FROM public."classes") t) <> '9598680cac7001006230299e368c7878' THEN RAISE EXCEPTION 'Original rows changed: classes'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "session_id"),'')) FROM (SELECT "session_id","class_id","room_id","session_date","start_time","end_time","status","attendance_finalized","booked_member_id","coach_id","subscription_id" FROM public."sessions") t) <> 'dbe3e22e954be6737f30c30724dabfcd' THEN RAISE EXCEPTION 'Original rows changed: sessions'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "package_id"),'')) FROM (SELECT "package_id","package_name","price","duration_days","benefits","status","center_id","package_type","total_credit" FROM public."membership_packages") t) <> '664ebfb12b04ef8fe511aa4f7809c158' THEN RAISE EXCEPTION 'Original rows changed: membership_packages'; END IF;
END $guard$;
-- SOURCE 05_migration_notifications.sql SHA256 8dd2543db0920fd549721fcd0da84a53976114a61e04555fa969133cf55444b4
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

-- SOURCE 06_migration_chat.sql SHA256 89e45b88c52666ce1f1d9f9e4aecc8e55d65cd880d21346e80d1e632982fc28a

CREATE TABLE IF NOT EXISTS chat_conversations (
    id SERIAL PRIMARY KEY,
    member_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','CLOSED')),
    assigned_staff_user_id INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
    handler_type VARCHAR(16) NOT NULL DEFAULT 'STAFF' CHECK (handler_type IN ('STAFF','AI')),
    subject VARCHAR(200) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_member ON chat_conversations(member_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_inbox ON chat_conversations(status,last_message_at DESC,id DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_conversations_member_open
    ON chat_conversations(member_user_id) WHERE status='OPEN';

CREATE TABLE IF NOT EXISTS chat_messages (
    id SERIAL PRIMARY KEY,
    conversation_id INT NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_type VARCHAR(16) NOT NULL CHECK (sender_type IN ('MEMBER','STAFF','AI','SYSTEM')),
    sender_user_id INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
    metadata TEXT NULL,
    client_message_id VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation ON chat_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_cursor ON chat_messages(conversation_id,id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_message_client_id
    ON chat_messages(conversation_id,sender_user_id,client_message_id) WHERE client_message_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS chat_read_state (
    conversation_id INT NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    last_read_message_id INT NULL REFERENCES chat_messages(id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(conversation_id,user_id)
);

INSERT INTO permissions(permission_name,description) VALUES
    ('CHAT_AS_MEMBER','Member may access their own support chat'),
    ('HANDLE_CHAT','Receptionist and CenterManager may handle support chats')
ON CONFLICT(permission_name) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.role_id,p.permission_id FROM roles r CROSS JOIN permissions p
WHERE UPPER(r.role_name)='MEMBER' AND p.permission_name='CHAT_AS_MEMBER'
ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.role_id,p.permission_id FROM roles r CROSS JOIN permissions p
WHERE UPPER(r.role_name) IN ('RECEPTIONIST','CENTERMANAGER') AND p.permission_name='HANDLE_CHAT'
ON CONFLICT DO NOTHING;


-- SOURCE 06_schedule_safety_postgres.sql SHA256 3f13d1c54d770f96ac18d58a05f5a6e97fa610b490299fcf3a6b2d48748c2187
-- PostgreSQL only. Apply after migration 04; do NOT run destructive demo reset 05.
-- Additive, repeatable; no prices, invoices, credits or existing schedules are rewritten.

ALTER TABLE subjects ADD COLUMN IF NOT EXISTS outdoor BOOLEAN;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE subjects SET outdoor = TRUE WHERE outdoor IS NULL AND slug IN ('bong-da','quan-vot-tennis','boi-loi','ban-cung');
UPDATE subjects SET outdoor = FALSE WHERE outdoor IS NULL AND slug IN ('cau-long','bong-ban','bong-ro','bong-chuyen','pickleball');
-- Unclassified custom subjects remain NULL and must be configured by the Manager.
CREATE TABLE IF NOT EXISTS normal_class_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    center_id INTEGER NOT NULL REFERENCES centers(center_id),
    class_id INTEGER NOT NULL UNIQUE REFERENCES classes(class_id)
);
ALTER TABLE member_packages DROP CONSTRAINT IF EXISTS member_packages_status_check;
ALTER TABLE member_packages ADD CONSTRAINT member_packages_status_check CHECK (status IN ('Pending','Active','Expired','Cancelled','Completed'));
-- Only backfill known, unambiguous built-in specialization names. Custom mappings remain explicit.
UPDATE specializations sp SET subject_id = su.subject_id
FROM subjects su, (VALUES
    ('Tennis','quan-vot-tennis'),('Quần vợt','quan-vot-tennis'),('Quần vợt (Tennis)','quan-vot-tennis'),
    ('Bóng đá','bong-da'),('Bơi lội','boi-loi'),('Bắn cung','ban-cung'),('Cầu lông','cau-long'),
    ('Bóng bàn','bong-ban'),('Bóng rổ','bong-ro'),('Bóng chuyền','bong-chuyen'),('Pickleball','pickleball')
) AS mapping(name, slug)
WHERE sp.subject_id IS NULL AND LOWER(sp.name) = LOWER(mapping.name) AND su.slug = mapping.slug;

-- SOURCE 07_session_subscription_postgres.sql SHA256 c39249682d50fa27a96f53ea5115d2ac8a96cf56c316f61a6082cbd4d48a05dd
-- PostgreSQL/public schema only. Additive upgrade; never run the demo reset.
-- Run after 04 and 06. No historical subscription is inferred or backfilled.

SET LOCAL lock_timeout = '5s';
LOCK TABLE public.member_packages IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.sessions IN ACCESS EXCLUSIVE MODE;
ALTER TABLE public.sessions ADD COLUMN IF NOT EXISTS subscription_id INTEGER NULL;

DO $migration$
DECLARE
    source_column SMALLINT;
    target_column SMALLINT;
    existing_fk RECORD;
BEGIN
    SELECT attnum INTO source_column FROM pg_attribute
    WHERE attrelid = 'public.sessions'::regclass AND attname = 'subscription_id'
      AND NOT attisdropped AND atttypid = 'integer'::regtype AND NOT attnotnull;
    IF source_column IS NULL THEN
        RAISE EXCEPTION 'sessions.subscription_id must be nullable INTEGER; manual review required';
    END IF;
    SELECT attnum INTO target_column FROM pg_attribute
    WHERE attrelid = 'public.member_packages'::regclass AND attname = 'subscription_id' AND NOT attisdropped;

    IF EXISTS (SELECT 1 FROM public.sessions s LEFT JOIN public.member_packages mp
               ON mp.subscription_id = s.subscription_id
               WHERE s.subscription_id IS NOT NULL AND mp.subscription_id IS NULL) THEN
        RAISE EXCEPTION 'Orphan sessions.subscription_id detected; no business data was changed';
    END IF;

    -- Compare definitions, not names. Never silently retain an incompatible FK.
    IF EXISTS (SELECT 1 FROM pg_constraint c
               WHERE c.conrelid = 'public.sessions'::regclass AND c.contype = 'f'
                 AND source_column = ANY(c.conkey)
                 AND NOT (c.conkey = ARRAY[source_column]
                     AND c.confrelid = 'public.member_packages'::regclass
                     AND c.confkey = ARRAY[target_column]
                     AND c.confdeltype IN ('a', 'r') AND c.confupdtype IN ('a', 'r'))) THEN
        RAISE EXCEPTION 'Incompatible subscription foreign key; manual review required';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint c
                   WHERE c.conrelid = 'public.sessions'::regclass AND c.contype = 'f'
                     AND c.conkey = ARRAY[source_column]
                     AND c.confrelid = 'public.member_packages'::regclass
                     AND c.confkey = ARRAY[target_column]) THEN
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.sessions'::regclass
                   AND conname = 'fk_session_subscription') THEN
            RAISE EXCEPTION 'Constraint name fk_session_subscription already used by another definition';
        END IF;
        ALTER TABLE public.sessions ADD CONSTRAINT fk_session_subscription
            FOREIGN KEY (subscription_id) REFERENCES public.member_packages(subscription_id);
    END IF;
    FOR existing_fk IN SELECT conname FROM pg_constraint
        WHERE conrelid = 'public.sessions'::regclass AND contype = 'f'
          AND conkey = ARRAY[source_column] AND NOT convalidated
    LOOP
        EXECUTE format('ALTER TABLE public.sessions VALIDATE CONSTRAINT %I', existing_fk.conname);
    END LOOP;

    -- An existing valid btree with this leading column also supports FK lookups.
    IF NOT EXISTS (SELECT 1 FROM pg_index i JOIN pg_class ic ON ic.oid = i.indexrelid
                   JOIN pg_am am ON am.oid = ic.relam
                   WHERE i.indrelid = 'public.sessions'::regclass
                     AND i.indisvalid AND i.indisready AND i.indpred IS NULL
                     AND i.indexprs IS NULL AND i.indkey[0] = source_column AND am.amname = 'btree') THEN
        IF to_regclass('public.idx_sessions_subscription') IS NOT NULL THEN
            RAISE EXCEPTION 'Index name idx_sessions_subscription already used by another definition';
        END IF;
        CREATE INDEX IF NOT EXISTS idx_sessions_subscription ON public.sessions(subscription_id);
    END IF;
END
$migration$;

-- SOURCE 08_advanced_entitlements_postgres.sql SHA256 e39ff49a59d4688dda943326966ad75351cd198a25414648230a9bbe2c22bd55
-- Additive upgrade. Run only after 06_schedule_safety and 07; no seed/reset/backfill guesses.

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

-- SOURCE 09_approved_catalog_postgres.sql SHA256 a4935100af5099d6146d57511e8ca2376cda9f2e46e7b26af7dd1a2522229f46
-- D02: guarded catalog-only cleanup. Does not change classes, invoices, or legacy subscriptions.

LOCK TABLE subjects IN SHARE ROW EXCLUSIVE MODE;
CREATE TABLE IF NOT EXISTS scms_data_migration_audit (
    migration_key VARCHAR(100) PRIMARY KEY, captured_at TIMESTAMP NOT NULL, before_data TEXT NOT NULL, after_data TEXT NOT NULL
);
DO $$
DECLARE before_snapshot TEXT; fk RECORD; used BOOLEAN;
BEGIN
    IF EXISTS (SELECT 1 FROM scms_data_migration_audit WHERE migration_key='09_approved_catalog') THEN RETURN; END IF;
    SELECT json_agg(s ORDER BY subject_id)::TEXT INTO before_snapshot FROM subjects s;
    -- Only the exact unused zero-price snapshot is eligible for deletion.
    IF EXISTS(SELECT 1 FROM subjects WHERE slug IS NULL) THEN
        IF (SELECT count(*) FROM subjects WHERE slug IS NULL) <> 9 OR
           EXISTS(SELECT 1 FROM subjects WHERE slug IS NULL AND NOT (
             normal_price=0 AND advanced_price=0 AND outdoor IS NULL AND
             (subject_id,subject_name) IN ((1,'Cầu lông'),(2,'Pickleball'),(3,'Bóng bàn'),(4,'Bắn cung'),
                 (5,'Bóng rổ'),(6,'Bóng chuyền'),(7,'Bắn súng'),(8,'Bóng đá'),(9,'Quần vợt')))) OR
           (SELECT count(*) FROM subjects WHERE (subject_id,slug,subject_name,normal_price,advanced_price) IN
             ((19,'quan-vot-tennis','Quần vợt (Tennis)',1500000,10000000),(20,'boi-loi','Bơi lội',1200000,9000000),
              (21,'pickleball','Pickleball',1100000,8500000),(22,'ban-cung','Bắn cung',1400000,9500000),
              (23,'bong-ro','Bóng rổ',1000000,8000000),(24,'bong-da','Bóng đá',900000,8000000),
              (25,'bong-chuyen','Bóng chuyền',900000,7000000),(26,'cau-long','Cầu lông',900000,7500000),
              (27,'bong-ban','Bóng bàn',800000,6500000))) <> 9 THEN
            RAISE EXCEPTION 'D02 catalog snapshot mismatch: manual reconciliation required';
        END IF;
        FOR fk IN SELECT conrelid::regclass AS tbl, a.attname AS col FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
            WHERE c.contype='f' AND c.confrelid='subjects'::regclass LOOP
            EXECUTE format('SELECT EXISTS(SELECT 1 FROM %s WHERE %I IN (SELECT subject_id FROM subjects WHERE slug IS NULL))',fk.tbl,fk.col) INTO used;
            IF used THEN RAISE EXCEPTION 'D02 duplicate subject has new references in %: reconcile first',fk.tbl; END IF;
        END LOOP;
        DELETE FROM subjects WHERE slug IS NULL AND subject_id BETWEEN 1 AND 9;
    END IF;
    IF (SELECT count(*) FROM subjects) <> 9 OR EXISTS(SELECT 1 FROM subjects WHERE slug NOT IN
        ('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban')) THEN
        RAISE EXCEPTION 'D02 requires exactly nine canonical subjects';
    END IF;
    UPDATE subjects SET normal_price = CASE slug
        WHEN 'quan-vot-tennis' THEN 2500000 WHEN 'boi-loi' THEN 1500000 WHEN 'pickleball' THEN 1800000
        WHEN 'ban-cung' THEN 2000000 WHEN 'bong-ro' THEN 1200000 WHEN 'bong-da' THEN 1000000
        WHEN 'bong-chuyen' THEN 1000000 WHEN 'cau-long' THEN 900000 WHEN 'bong-ban' THEN 800000 END,
        advanced_price = CASE slug
        WHEN 'quan-vot-tennis' THEN 8000000 WHEN 'boi-loi' THEN 4500000 WHEN 'pickleball' THEN 5000000
        WHEN 'ban-cung' THEN 6000000 WHEN 'bong-ro' THEN 3500000 WHEN 'bong-da' THEN 3500000
        WHEN 'bong-chuyen' THEN 3500000 WHEN 'cau-long' THEN 3000000 WHEN 'bong-ban' THEN 2500000 END;
    ALTER TABLE subjects ADD CONSTRAINT ck_approved_subject_slug CHECK (slug IS NOT NULL AND slug IN
        ('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban'));
    ALTER TABLE subjects ADD CONSTRAINT ck_subject_positive_prices CHECK(normal_price>0 AND advanced_price>0);
    INSERT INTO scms_data_migration_audit VALUES('09_approved_catalog',CURRENT_TIMESTAMP,before_snapshot,
        (SELECT json_agg(s ORDER BY subject_id)::TEXT FROM subjects s));
END $$;

-- SOURCE 10_session_incidents_postgres.sql SHA256 9bedbb2e9079fd8316fac23c1160c4821a4a8dd47c9902f97dbd49d0acf4b712

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

-- SOURCE 11_holiday_history_postgres.sql SHA256 5102fd37f627c33405aa830e0482b2c93c2da31bdf64ff643bb97ed97d0198c7

ALTER TABLE holidays ADD COLUMN IF NOT EXISTS actual_closed_until TIMESTAMP;
-- No guessed backfill. An active holiday's actual past interval is captured on deactivation.

-- SOURCE 12_payment_resolutions_postgres.sql SHA256 12e6e4709595b442035ef623efb3ec3b500820a2489dad378244e30a8cade6f0

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS cash_note VARCHAR(2000);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS cash_confirmation_reversed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS resolution_status VARCHAR(30);
ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS eligibility_subscription_id INT REFERENCES member_packages(subscription_id);
CREATE TABLE IF NOT EXISTS payment_resolutions (
    id SERIAL PRIMARY KEY, invoice_id INT NOT NULL REFERENCES invoices(invoice_id), resolution_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Open', amount NUMERIC(12,2) NOT NULL, reason VARCHAR(2000) NOT NULL,
    evidence VARCHAR(2000), outcome_note VARCHAR(2000), created_by INT NOT NULL REFERENCES users(user_id), created_at TIMESTAMP NOT NULL,
    approved_by INT REFERENCES users(user_id), approved_at TIMESTAMP, completed_by INT REFERENCES users(user_id), completed_at TIMESTAMP,
    performed_at TIMESTAMP, UNIQUE(invoice_id,resolution_type), CHECK(amount>0)
);

DO $verify$ BEGIN
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "user_id"),'')) FROM (SELECT "user_id","role_id","full_name","email","phone","password_hash","address","gender","date_of_birth","status","created_at","failed_login_attempts","locked_until","is_first_login","avatar_url","avatar_public_id","center_id" FROM public."users") t) <> 'ed86323db9f2cefb858416d87032859b' THEN RAISE EXCEPTION 'Original rows changed: users'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "invoice_id"),'')) FROM (SELECT "invoice_id","member_id","package_id","receptionist_id","amount","payment_method","payment_status","payment_date","gateway_transaction_ref","class_id","subscription_id","invoice_code","created_at","expires_at","reconciliation_required" FROM public."invoices") t) <> '9fd65f291f73175e182b6a0a0ec789ac' THEN RAISE EXCEPTION 'Original rows changed: invoices'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "subscription_id"),'')) FROM (SELECT "subscription_id","member_id","package_id","start_date","end_date","status","remaining_credit","validity_days" FROM public."member_packages") t) <> '888571012baca09c7a17997c9f384f4e' THEN RAISE EXCEPTION 'Original rows changed: member_packages'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "enrollment_id"),'')) FROM (SELECT "enrollment_id","member_id","class_id","enrolled_at","status","cancelled_at","cancelled_by","cancel_reason","promoted_at" FROM public."class_enrollments") t) <> 'ee3236300fa6f79a2b117191a71cea4e' THEN RAISE EXCEPTION 'Original rows changed: class_enrollments'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "class_id"),'')) FROM (SELECT "class_id","class_name","subject_id","coach_id","max_capacity","start_date","end_date","status","center_id","tuition_fee","class_type","total_sessions" FROM public."classes") t) <> '9598680cac7001006230299e368c7878' THEN RAISE EXCEPTION 'Original rows changed: classes'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "session_id"),'')) FROM (SELECT "session_id","class_id","room_id","session_date","start_time","end_time","status","attendance_finalized","booked_member_id","coach_id","subscription_id" FROM public."sessions") t) <> 'dbe3e22e954be6737f30c30724dabfcd' THEN RAISE EXCEPTION 'Original rows changed: sessions'; END IF;
IF (SELECT md5(COALESCE(string_agg(to_jsonb(t)::text,',' ORDER BY "package_id"),'')) FROM (SELECT "package_id","package_name","price","duration_days","benefits","status","center_id","package_type","total_credit" FROM public."membership_packages") t) <> '664ebfb12b04ef8fe511aa4f7809c158' THEN RAISE EXCEPTION 'Original rows changed: membership_packages'; END IF;
IF (SELECT count(*) FROM subjects) <> 9 THEN RAISE EXCEPTION 'Nine approved subjects required'; END IF;
END $verify$;
COMMIT;
