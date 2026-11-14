-- SCMS DEMO REBUILD — Owner09/11/2026. This file DELETES the listed SCMS demo tables/data.
-- Execute this WHOLE file once, only on the intended SCMS demo DB. Do not run the root additive variant afterwards.
-- Scope: 56 SCMS tables + 4 obsolete SCMS backup tables observed on the current Supabase.
-- auth/storage/realtime/extensions, unlisted tables and unrelated functions are not dropped.
-- External dependencies fail RESTRICT and roll back. This is a reset, not an upgrade preserving demo history.
-- Fresh data: approved47 demo accounts,12 Normal classes and8 registration-owned Advanced classes; no provider calls.
-- invoice_code_seq is NOT restarted: never reuse outward payment references after resetting demo rows.
BEGIN;
SET LOCAL search_path=public,extensions,pg_catalog;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout='10s';
SET LOCAL statement_timeout='180s';

DO $anchor$
DECLARE business_date DATE := (clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
BEGIN
 IF business_date<>DATE '2026-11-14' THEN
  RAISE EXCEPTION 'SCMS_DEMO_STALE_ANCHOR: bundle anchor %, current VN date %; regenerate and verify the whole bundle', DATE '2026-11-14', business_date;
 END IF;
END $anchor$;

SELECT pg_advisory_xact_lock(391,20261109);
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE TEMP TABLE _scms_reset_before(table_name TEXT PRIMARY KEY,row_count BIGINT) ON COMMIT DROP;
CREATE TEMP TABLE _scms_code_floor(value BIGINT NOT NULL) ON COMMIT DROP;

DO $preflight$
DECLARE n TEXT; count_rows BIGINT; highest NUMERIC:=0; seq_value BIGINT:=0; ext RECORD;
BEGIN
 IF to_regclass('public.users') IS NOT NULL AND NOT EXISTS(SELECT 1 FROM information_schema.columns
   WHERE table_schema='public' AND table_name='users' AND column_name='password_hash') THEN
  RAISE EXCEPTION 'SCMS_DEMO_WRONG_TARGET: public.users is not the expected SCMS model';
 END IF;
 FOR ext IN SELECT e.extname,c.relname FROM pg_depend d JOIN pg_extension e ON e.oid=d.refobjid
   JOIN pg_class c ON c.oid=d.objid JOIN pg_namespace ns ON ns.oid=c.relnamespace
   WHERE d.classid='pg_class'::regclass AND d.refclassid='pg_extension'::regclass AND d.deptype='e'
     AND ns.nspname='public' AND c.relname=ANY(ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4']) LOOP
  RAISE EXCEPTION 'SCMS_DEMO_EXTENSION_OBJECT: %.% belongs to extension %','public',ext.relname,ext.extname;
 END LOOP;
 FOREACH n IN ARRAY ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4'] LOOP
  IF to_regclass('public.'||n) IS NOT NULL THEN
   EXECUTE format('SELECT count(*) FROM public.%I',n) INTO count_rows;
   INSERT INTO _scms_reset_before VALUES(n,count_rows);
  END IF;
 END LOOP;
 IF to_regclass('public.invoice_code_seq') IS NOT NULL THEN
  EXECUTE 'SELECT last_value FROM public.invoice_code_seq' INTO seq_value;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='invoices' AND column_name='invoice_code') THEN
  EXECUTE $q$SELECT coalesce(max(substring(invoice_code from '^INV-[0-9]{4}-([0-9]+)$')::numeric),0) FROM public.invoices$q$ INTO highest;
 END IF;
 -- Fresh namespace floor also protects against references whose old demo invoices were deleted previously.
 highest:=greatest(highest,seq_value,floor(extract(epoch FROM clock_timestamp())*1000));
 IF highest>=9223372036854775806 THEN RAISE EXCEPTION 'SCMS_DEMO_INVOICE_COUNTER_RANGE: reconcile counter before reset'; END IF;
 INSERT INTO _scms_code_floor VALUES(highest::bigint);
END $preflight$;

DROP TABLE IF EXISTS public.private_session_proposals,
 public.normal_refund_verifications,
 public.normal_refund_documents,
 public.normal_refund_attempts,
 public.normal_refund_receivers,
 public.normal_refunds,
 public.normal_transfers,
 public.normal_responses,
 public.normal_proposals,
 public.scms_data_migration_audit,
 public.payment_resolutions,
 public.incident_affected_members,
 public.center_incidents,
 public.session_changes,
 public.session_absence_notices,
 public.advanced_registrations,
 public.coach_days_off,
 public.coach_work_slots,
 public.coach_work_schedules,
 public.subscription_pauses,
 public.normal_class_requests,
 public.system_logs,
 public.ai_consultations,
 public.chat_read_state,
 public.chat_messages,
 public.chat_conversations,
 public.notification_recipients,
 public.notifications,
 public.support_requests,
 public.vnpay_payment_attempts,
 public.invoices,
 public.holidays,
 public.class_enrollments,
 public.member_packages,
 public.membership_packages,
 public.class_feedbacks,
 public.evaluations,
 public.attendances,
 public.training_results,
 public.sessions,
 public.training_plans,
 public.classes,
 public.rooms,
 public.coach_specializations,
 public.specializations,
 public.subjects,
 public.members,
 public.coaches,
 public.receptionists,
 public.center_managers,
 public.email_otps,
 public.users,
 public.centers,
 public.role_permissions,
 public.permissions,
 public.roles,
 public.attendances_backup_flow4,
 public.class_enrollments_backup_flow4,
 public.role_permissions_backup_n06,
 public.training_results_backup_flow4 RESTRICT;

DROP FUNCTION IF EXISTS public.enforce_user_center_assignment() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_classes_validate() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_member_packages_validate() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_room_code_default() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_rooms_protect() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_current_advanced_only() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_no_legacy_invoice() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_retired_membership_catalog() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_sessions_validate() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_users_center_guard() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_advanced_registration_class() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_advanced_class_identity() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_private_session_class() RESTRICT;
DROP FUNCTION IF EXISTS public.fn_scms_advanced_class_has_owner() RESTRICT;

-- SOURCE: database/bootstrap/scms_structure_postgres.sql SHA256 b1674226b99ec4799971465879d28b74f002bb733051af880a2a80077376acad
-- No accounts, Paid invoices, active subscriptions, sessions or demo transactions.
-- Empty public schema required. Existing databases must use reviewed incremental migrations.

-- The scoped reset above preserves unrelated public objects.
CREATE TABLE IF NOT EXISTS roles (
    role_id     SERIAL PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS permissions (
    permission_id   SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
        ON DELETE CASCADE
);

-- =====================================================================
-- 2. USERS + role subtypes
-- =====================================================================

CREATE TABLE IF NOT EXISTS centers (
    center_id SERIAL PRIMARY KEY,
    center_name VARCHAR(150) NOT NULL
);

INSERT INTO centers (center_id, center_name)
VALUES (1, 'Sports Center 1')
ON CONFLICT (center_id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('centers', 'center_id'),
              GREATEST((SELECT COALESCE(MAX(center_id), 1) FROM centers), 1));

CREATE TABLE IF NOT EXISTS users (
    user_id       SERIAL PRIMARY KEY,
    role_id       INT NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    address       VARCHAR(255),
    gender        VARCHAR(10)  CHECK (gender IN ('Male', 'Female', 'Other')),
    date_of_birth DATE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'Active'
        CHECK (status IN ('Active', 'Inactive', 'Locked', 'Pending')),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until  TIMESTAMP NULL,
    center_id     INT NULL DEFAULT 1,
    is_first_login BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(500) NULL,
    avatar_public_id VARCHAR(255) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id),
    CONSTRAINT fk_user_center
        FOREIGN KEY (center_id) REFERENCES centers(center_id)
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role_id);

CREATE TABLE IF NOT EXISTS email_otps (
    otp_id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    user_id INT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('REGISTER', 'RESET_PASSWORD')),
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_email_otps_quota ON email_otps(email, purpose, created_at);

UPDATE users u
SET center_id = 1
WHERE u.center_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles r WHERE r.role_id = u.role_id AND r.role_name = 'Admin'
  );

CREATE OR REPLACE FUNCTION enforce_user_center_assignment()
RETURNS TRIGGER AS $$
DECLARE role_name_value VARCHAR(50);
BEGIN
    SELECT role_name INTO role_name_value FROM roles WHERE role_id = NEW.role_id;
    IF role_name_value IS DISTINCT FROM 'Admin' AND NEW.center_id IS NULL THEN
        RAISE EXCEPTION 'center_id is required for non-admin users';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_require_center ON users;
CREATE TRIGGER trg_users_require_center
    BEFORE INSERT OR UPDATE OF role_id, center_id ON users
    FOR EACH ROW EXECUTE FUNCTION enforce_user_center_assignment();

CREATE TABLE IF NOT EXISTS center_managers (
    user_id INT PRIMARY KEY,
    CONSTRAINT fk_cm_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS receptionists (
    user_id INT PRIMARY KEY,
    CONSTRAINT fk_receptionist_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS coaches (
    user_id        INT PRIMARY KEY,
    specialization VARCHAR(100),
    certification  VARCHAR(150),
    bio            TEXT,
    CONSTRAINT fk_coach_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS members (
    user_id       INT PRIMARY KEY,
    goal          VARCHAR(255),
    health_note   TEXT,
    join_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    registered_by INT NULL,
    checkin_secret VARCHAR(64) NULL,
    checkin_last_step BIGINT NULL,
    booking_blocked_until TIMESTAMP NULL,
    CONSTRAINT fk_member_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_registered_by
        FOREIGN KEY (registered_by) REFERENCES users(user_id)
);

-- Flow 4 rotating QR check-in secret and replay protection (run manually by database owner)
ALTER TABLE members ADD COLUMN IF NOT EXISTS checkin_secret VARCHAR(64) NULL;
ALTER TABLE members ADD COLUMN IF NOT EXISTS checkin_last_step BIGINT NULL;
ALTER TABLE members ADD COLUMN IF NOT EXISTS booking_blocked_until TIMESTAMP NULL;

-- =====================================================================
-- 3. SUBJECTS – ROOMS – CLASSES
-- =====================================================================

CREATE TABLE IF NOT EXISTS subjects (
    subject_id     SERIAL PRIMARY KEY,
    subject_name   VARCHAR(100) NOT NULL,
    description    VARCHAR(255),
    normal_price   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (normal_price >= 0),
    advanced_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (advanced_price >= 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
    slug           VARCHAR(120) NULL UNIQUE
);

-- Idempotent column check for existing databases
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS normal_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (normal_price >= 0);
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS advanced_price NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (advanced_price >= 0);

CREATE TABLE IF NOT EXISTS specializations (
    specialization_id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    subject_id INT NULL REFERENCES subjects(subject_id),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive'))
);

CREATE TABLE IF NOT EXISTS coach_specializations (
    coach_id INT NOT NULL REFERENCES coaches(user_id) ON DELETE CASCADE,
    specialization_id INT NOT NULL REFERENCES specializations(specialization_id) ON DELETE CASCADE,
    PRIMARY KEY (coach_id, specialization_id)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id       SERIAL PRIMARY KEY,
    room_name     VARCHAR(50)  NOT NULL,
    location      VARCHAR(100),
    capacity      INT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'Available'
        CHECK (status IN ('Available', 'Maintenance', 'Closed')),
    facility_type VARCHAR(20)  NOT NULL DEFAULT 'Indoor'
        CHECK (facility_type IN ('Indoor', 'Outdoor')),
    center_id     INT NOT NULL DEFAULT 1 REFERENCES centers(center_id),
    CONSTRAINT chk_room_capacity CHECK (capacity IS NULL OR capacity > 0)
);

-- Idempotent column & constraint check for existing databases
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS facility_type VARCHAR(20) NOT NULL DEFAULT 'Indoor';
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS chk_facility_type;
ALTER TABLE rooms ADD CONSTRAINT chk_facility_type CHECK (facility_type IN ('Indoor', 'Outdoor'));

-- Room codes: additive and compatible with numeric IDs.
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS room_code VARCHAR(40);
UPDATE rooms SET room_code='C'||lpad(center_id::text,greatest(2,length(center_id::text)),'0')||'-LEGACY-'||room_id WHERE room_code IS NULL;
ALTER TABLE rooms ALTER COLUMN room_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_rooms_code ON rooms(room_code);
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='rooms'::regclass AND conname='chk_room_code') THEN
  ALTER TABLE rooms ADD CONSTRAINT chk_room_code CHECK (
   room_code ~ '^C[0-9]{2,}-[A-Z0-9]+-[A-Z0-9]+$'
   AND split_part(room_code,'-',1)='C'||lpad(center_id::text,greatest(2,length(center_id::text)),'0'));
 END IF;
END $$;
-- Allows a rolling deploy: old writers omit code; the numeric ID remains stable.
CREATE OR REPLACE FUNCTION fn_room_code_default() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.room_code IS NULL THEN NEW.room_code:='C'||lpad(NEW.center_id::text,greatest(2,length(NEW.center_id::text)),'0')||'-LEGACY-'||NEW.room_id; END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_room_code_default ON rooms;
CREATE TRIGGER trg_room_code_default BEFORE INSERT ON rooms FOR EACH ROW EXECUTE FUNCTION fn_room_code_default();


CREATE TABLE IF NOT EXISTS classes (
    class_id       SERIAL PRIMARY KEY,
    class_name     VARCHAR(100) NOT NULL,
    subject_id     INT NOT NULL,
    coach_id       INT NULL,
    max_capacity   INT,
    tuition_fee    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tuition_fee >= 0),
    start_date     DATE,
    end_date       DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'Open'
        CHECK (status IN ('Open', 'Ongoing', 'Closed', 'Cancelled')),
    class_type     VARCHAR(20) NOT NULL DEFAULT 'Normal'
        CHECK (class_type IN ('Normal', 'Advanced')),
    total_sessions INT NOT NULL DEFAULT 12,
    center_id      INT NOT NULL DEFAULT 1 REFERENCES centers(center_id),
    CONSTRAINT chk_class_capacity CHECK (
        (class_type = 'Normal' AND (max_capacity IS NULL OR max_capacity <= 15))
        OR (class_type = 'Advanced' AND (max_capacity IS NULL OR max_capacity = 1))
    ),
    CONSTRAINT chk_class_dates    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT fk_class_subject   FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
    CONSTRAINT fk_class_coach     FOREIGN KEY (coach_id)   REFERENCES coaches(user_id)
);

-- Idempotent column & constraint check for existing databases
ALTER TABLE classes ADD COLUMN IF NOT EXISTS class_type VARCHAR(20) DEFAULT 'Normal';
ALTER TABLE classes ADD COLUMN IF NOT EXISTS total_sessions INT DEFAULT 12;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_class_type;
ALTER TABLE classes ADD CONSTRAINT chk_class_type CHECK (class_type IN ('Normal', 'Advanced'));
ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_class_capacity;
ALTER TABLE classes ADD CONSTRAINT chk_class_capacity CHECK (
    (class_type = 'Normal' AND (max_capacity IS NULL OR max_capacity <= 15))
    OR (class_type = 'Advanced' AND (max_capacity IS NULL OR max_capacity = 1))
);

CREATE INDEX IF NOT EXISTS idx_classes_subject ON classes(subject_id);
CREATE INDEX IF NOT EXISTS idx_classes_coach   ON classes(coach_id);

-- =====================================================================
-- 4. TRAINING PLANS
-- =====================================================================

CREATE TABLE IF NOT EXISTS training_plans (
    plan_id    SERIAL PRIMARY KEY,
    coach_id   INT NOT NULL,
    class_id   INT NULL,
    member_id  INT NULL,
    title      VARCHAR(150) NOT NULL,
    content    TEXT,
    goal       VARCHAR(255),
    start_date DATE,
    end_date   DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_coach  FOREIGN KEY (coach_id)  REFERENCES coaches(user_id),
    CONSTRAINT fk_plan_class  FOREIGN KEY (class_id)  REFERENCES classes(class_id),
    CONSTRAINT fk_plan_member FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT chk_plan_target CHECK (
        (class_id IS NOT NULL AND member_id IS NULL)
        OR (class_id IS NULL AND member_id IS NOT NULL)
    ),
    CONSTRAINT chk_plan_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_plan_coach  ON training_plans(coach_id);
CREATE INDEX IF NOT EXISTS idx_plan_class  ON training_plans(class_id);
CREATE INDEX IF NOT EXISTS idx_plan_member ON training_plans(member_id);

-- =====================================================================
-- 5. SESSIONS + TRAINING RESULTS
-- =====================================================================

CREATE TABLE IF NOT EXISTS sessions (
    session_id           SERIAL PRIMARY KEY,
    class_id             INT NOT NULL,
    room_id              INT NOT NULL,
    session_date         DATE NOT NULL,
    start_time           TIME NOT NULL,
    end_time             TIME NOT NULL,
    attendance_finalized BOOLEAN NOT NULL DEFAULT FALSE,
    booked_member_id     INT NULL,
    subscription_id      INT NULL,
    coach_id             INT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'Scheduled'
        CHECK (status IN ('Scheduled', 'Completed', 'Cancelled')),
    CONSTRAINT chk_session_time         CHECK (end_time > start_time),
    CONSTRAINT fk_session_class         FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_room          FOREIGN KEY (room_id)  REFERENCES rooms(room_id),
    CONSTRAINT fk_session_booked_member FOREIGN KEY (booked_member_id) REFERENCES members(user_id),
    CONSTRAINT fk_session_coach         FOREIGN KEY (coach_id) REFERENCES coaches(user_id)
);

-- Idempotent column check for existing databases
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS booked_member_id INT NULL REFERENCES members(user_id);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS coach_id INT NULL REFERENCES coaches(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_booked_member ON sessions(booked_member_id);
CREATE INDEX IF NOT EXISTS idx_sessions_coach ON sessions(coach_id);

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS attendance_finalized BOOLEAN NOT NULL DEFAULT FALSE;
-- Mark sessions that already ended at migration time as finalized so the new job does not
-- retroactively penalize historical attendance gaps. The comparison uses Vietnam local time.
UPDATE sessions
SET attendance_finalized = TRUE
WHERE attendance_finalized = FALSE
  AND session_date + end_time < (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh');

CREATE INDEX IF NOT EXISTS idx_session_class ON sessions(class_id);
CREATE INDEX IF NOT EXISTS idx_session_room  ON sessions(room_id);

CREATE TABLE IF NOT EXISTS training_results (
    result_id         SERIAL PRIMARY KEY,
    session_id        INT NOT NULL,
    member_id         INT NOT NULL,
    coach_id          INT NOT NULL,
    content           TEXT,
    attendance_status VARCHAR(10) NOT NULL DEFAULT 'Present'
        CHECK (attendance_status IN ('Present', 'Absent', 'Late', 'Excused')),
    recorded_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_session FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_result_member  FOREIGN KEY (member_id)  REFERENCES members(user_id),
    CONSTRAINT fk_result_coach   FOREIGN KEY (coach_id)   REFERENCES coaches(user_id),
    CONSTRAINT uq_result_session_member UNIQUE (session_id, member_id)
);

-- =====================================================================
-- 5b. ATTENDANCES
-- =====================================================================

CREATE TABLE IF NOT EXISTS attendances (
    attendance_id  SERIAL PRIMARY KEY,
    session_id     INT NULL,
    member_id      INT NOT NULL,
    recorded_by    INT NULL,
    state          VARCHAR(20) NOT NULL DEFAULT 'Present'
        CHECK (state IN ('Present', 'Absent', 'Late', 'CheckedIn', 'Excused')),
    note           TEXT NULL,
    absence_notice_id INT NULL,
    superseded_at TIMESTAMP NULL,
    check_in_time  TIMESTAMP NULL,
    check_out_time TIMESTAMP NULL,
    CONSTRAINT fk_attendance_session  FOREIGN KEY (session_id)  REFERENCES sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_member   FOREIGN KEY (member_id)   REFERENCES members(user_id),
    CONSTRAINT fk_attendance_recorder FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- Flow 4 absence state migration (idempotent; run manually by database owner)
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS note TEXT NULL;
ALTER TABLE attendances DROP CONSTRAINT IF EXISTS attendances_state_check;
ALTER TABLE attendances DROP CONSTRAINT IF EXISTS ck_attendances_state;
ALTER TABLE attendances ADD CONSTRAINT ck_attendances_state
    CHECK (state IN ('Present', 'Absent', 'Late', 'CheckedIn', 'Excused'));
ALTER TABLE training_results DROP CONSTRAINT IF EXISTS training_results_attendance_status_check;
ALTER TABLE training_results DROP CONSTRAINT IF EXISTS ck_training_results_attendance_status;
ALTER TABLE training_results ADD CONSTRAINT ck_training_results_attendance_status
    CHECK (attendance_status IN ('Present', 'Absent', 'Late', 'Excused'));

-- Partial unique index: only one attendance record per (session, member) when session_id IS NOT NULL
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS absence_notice_id INT;
ALTER TABLE attendances ADD COLUMN IF NOT EXISTS superseded_at TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS ux_attendance_session_member
    ON attendances(session_id, member_id)
    WHERE session_id IS NOT NULL AND superseded_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_attendance_member ON attendances(member_id);

-- =====================================================================
-- 5c. EVALUATIONS
-- =====================================================================

CREATE TABLE IF NOT EXISTS evaluations (
    evaluation_id   SERIAL PRIMARY KEY,
    member_id       INT NOT NULL,
    coach_id        INT NOT NULL,
    evaluation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    comment         TEXT,
    progress_score  DECIMAL(3,1)
        CHECK (progress_score BETWEEN 0 AND 10),
    CONSTRAINT fk_eval_member FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_eval_coach  FOREIGN KEY (coach_id)  REFERENCES coaches(user_id)
);

CREATE INDEX IF NOT EXISTS idx_eval_member ON evaluations(member_id);
CREATE INDEX IF NOT EXISTS idx_eval_coach  ON evaluations(coach_id);

-- =====================================================================
-- 5d. MEMBER FEEDBACK FOR CLASSES AND COACHES
-- =====================================================================

CREATE TABLE IF NOT EXISTS class_feedbacks (
    feedback_id   SERIAL PRIMARY KEY,
    class_id      INT NOT NULL,
    member_id     INT NOT NULL,
    coach_id      INT NOT NULL,
    class_rating  SMALLINT NOT NULL
        CHECK (class_rating BETWEEN 1 AND 5),
    coach_rating  SMALLINT NOT NULL
        CHECK (coach_rating BETWEEN 1 AND 5),
    comment       TEXT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_feedback_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id),
    CONSTRAINT fk_class_feedback_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_class_feedback_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id),
    CONSTRAINT uq_feedback_class_member UNIQUE (class_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_class_feedback_coach
    ON class_feedbacks(coach_id);

-- =====================================================================
-- 6. MEMBERSHIP PACKAGES + SUBSCRIPTIONS + CLASS ENROLLMENTS
-- =====================================================================

CREATE TABLE IF NOT EXISTS membership_packages (
    package_id    SERIAL PRIMARY KEY,
    package_name  VARCHAR(100)    NOT NULL,
    price         DECIMAL(12,2)   NOT NULL,
    duration_days INT             NOT NULL,
    benefits      TEXT,
    package_type  VARCHAR(20)     NOT NULL DEFAULT 'Normal'
        CHECK (package_type IN ('Normal', 'Advanced')),
    total_credit  INT             NOT NULL DEFAULT 12,
    status        VARCHAR(20)     NOT NULL DEFAULT 'Active'
        CHECK (status IN ('Active', 'Inactive')),
    center_id     INT             NOT NULL DEFAULT 1 REFERENCES centers(center_id)
);

-- Idempotent column check for existing databases
ALTER TABLE membership_packages ADD COLUMN IF NOT EXISTS package_type VARCHAR(20) DEFAULT 'Normal';
ALTER TABLE membership_packages ADD COLUMN IF NOT EXISTS total_credit INT DEFAULT 12;
ALTER TABLE membership_packages DROP CONSTRAINT IF EXISTS chk_package_type;
ALTER TABLE membership_packages ADD CONSTRAINT chk_package_type CHECK (package_type IN ('Normal', 'Advanced'));

CREATE TABLE IF NOT EXISTS member_packages (
    subscription_id  SERIAL PRIMARY KEY,
    member_id        INT NOT NULL,
    package_id       INT NOT NULL,
    start_date       DATE NOT NULL,
    end_date         DATE NOT NULL,
    remaining_credit INT NOT NULL DEFAULT 0,
    validity_days    INT NOT NULL DEFAULT 45,
    status           VARCHAR(20) NOT NULL DEFAULT 'Active'
        CHECK (status IN ('Pending', 'Active', 'Expired', 'Cancelled')),
    CONSTRAINT fk_mp_member  FOREIGN KEY (member_id)  REFERENCES members(user_id),
    CONSTRAINT fk_mp_package FOREIGN KEY (package_id) REFERENCES membership_packages(package_id)
);

-- Idempotent column check for existing databases
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS remaining_credit INT DEFAULT 0;
ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS validity_days INT DEFAULT 45;

CREATE INDEX IF NOT EXISTS idx_mp_member  ON member_packages(member_id);
CREATE INDEX IF NOT EXISTS idx_mp_package ON member_packages(package_id);

-- Session subscription FK must follow creation of member_packages.
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

CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    member_id     INT NOT NULL,
    class_id      INT NOT NULL,
    enrolled_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(20) NOT NULL DEFAULT 'Registered'
        CHECK (status IN ('Pending', 'Registered', 'Waitlisted', 'Cancelled', 'Completed')),
    CONSTRAINT fk_enroll_member FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_enroll_class  FOREIGN KEY (class_id)  REFERENCES classes(class_id),
    CONSTRAINT uq_enrollment UNIQUE (member_id, class_id)
);

-- Flow 4 enrollment metadata and waitlist state (idempotent migration)
ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP NULL;
ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS cancelled_by INT NULL REFERENCES users(user_id);
ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS cancel_reason TEXT NULL;
ALTER TABLE class_enrollments ADD COLUMN IF NOT EXISTS promoted_at TIMESTAMP NULL;
ALTER TABLE class_enrollments DROP CONSTRAINT IF EXISTS class_enrollments_status_check;
ALTER TABLE class_enrollments DROP CONSTRAINT IF EXISTS ck_class_enrollments_status;
ALTER TABLE class_enrollments ADD CONSTRAINT ck_class_enrollments_status
    CHECK (status IN ('Pending', 'Registered', 'Waitlisted', 'Cancelled', 'Completed'));

-- =====================================================================
-- 6B. HOLIDAYS (QUẢN LÝ LỊCH NGHỈ LỄ/TẾT)
-- =====================================================================

CREATE TABLE IF NOT EXISTS holidays (
    holiday_id   SERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    holiday_name VARCHAR(120) NOT NULL,
    tag          VARCHAR(30) NOT NULL CHECK (tag IN ('NATIONAL_FIXED', 'LUNAR_FESTIVAL', 'CENTER_EVENT')),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    description  TEXT
);

CREATE INDEX IF NOT EXISTS idx_holidays_date ON holidays(holiday_date);
CREATE INDEX IF NOT EXISTS idx_holidays_active ON holidays(is_active);

-- =====================================================================
-- 7. INVOICES
-- =====================================================================

CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 1;

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id              SERIAL PRIMARY KEY,
    member_id               INT NOT NULL,
    package_id              INT NULL,
    class_id                INT NULL,
    subscription_id         INT NULL,
    receptionist_id         INT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    invoice_code            VARCHAR(30) NOT NULL DEFAULT ('INV-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(nextval('invoice_code_seq')::text, 6, '0')),
    payment_method          VARCHAR(20)
        CHECK (payment_method IN ('Cash', 'BankTransfer', 'CreditCard', 'EWallet', 'VNPay')),
    payment_status          VARCHAR(20) NOT NULL DEFAULT 'Pending'
        CHECK (payment_status IN ('Pending', 'Paid', 'Failed', 'Refunded', 'Expired', 'Cancelled')),
    payment_date            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMP,
    gateway_transaction_ref VARCHAR(100),
    CONSTRAINT fk_invoice_member       FOREIGN KEY (member_id)       REFERENCES members(user_id),
    CONSTRAINT fk_invoice_package      FOREIGN KEY (package_id)      REFERENCES membership_packages(package_id),
    CONSTRAINT fk_invoice_class        FOREIGN KEY (class_id)        REFERENCES classes(class_id),
    CONSTRAINT fk_invoice_subscription FOREIGN KEY (subscription_id) REFERENCES member_packages(subscription_id),
    CONSTRAINT fk_invoice_receptionist FOREIGN KEY (receptionist_id) REFERENCES receptionists(user_id),
    CONSTRAINT chk_invoice_target CHECK ((package_id IS NOT NULL AND class_id IS NULL) OR (package_id IS NULL AND class_id IS NOT NULL))
);

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

CREATE INDEX IF NOT EXISTS idx_invoice_member       ON invoices(member_id);
CREATE INDEX IF NOT EXISTS idx_invoice_receptionist ON invoices(receptionist_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_invoice_code ON invoices(invoice_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_gateway_transaction_ref ON invoices(gateway_transaction_ref) WHERE gateway_transaction_ref IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_member_package_pending ON invoices(member_id, package_id) WHERE payment_status = 'Pending' AND package_id IS NOT NULL;

-- =====================================================================
-- 8. SUPPORT REQUESTS
-- =====================================================================

CREATE TABLE IF NOT EXISTS support_requests (
    request_id      SERIAL PRIMARY KEY,
    member_id       INT NOT NULL,
    receptionist_id INT NULL,
    content         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'Open'
        CHECK (status IN ('Open', 'InProgress', 'Resolved', 'Closed')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP NULL,
    CONSTRAINT fk_support_member       FOREIGN KEY (member_id)       REFERENCES members(user_id),
    CONSTRAINT fk_support_receptionist FOREIGN KEY (receptionist_id) REFERENCES receptionists(user_id)
);

-- =====================================================================
-- 9. NOTIFICATIONS
-- =====================================================================

CREATE TABLE IF NOT EXISTS notifications (
    notification_id SERIAL PRIMARY KEY,
    user_id         INT NULL,
    sender_id       INT NULL,
    title           VARCHAR(150) NOT NULL,
    content         TEXT,
    type            VARCHAR(20) NOT NULL DEFAULT 'Other'
        CHECK (type IN ('Schedule', 'Payment', 'System', 'Promotion', 'Other')),
    notification_type VARCHAR(40),
    related_type    VARCHAR(40),
    related_id      BIGINT,
    dedupe_key      VARCHAR(180),
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_noti_user   FOREIGN KEY (user_id)   REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_noti_sender FOREIGN KEY (sender_id) REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_noti_user ON notifications(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_dedupe_key
    ON notifications (dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications (created_at DESC, notification_id DESC);

CREATE TABLE IF NOT EXISTS notification_recipients (
    notification_id INT NOT NULL,
    user_id         INT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notification_recipients PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_nr_notification FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_nr_user         FOREIGN KEY (user_id)         REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_nr_user ON notification_recipients(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_recipients_feed
    ON notification_recipients (user_id, is_read, created_at DESC);

-- =====================================================================
-- 10. SUPPORT CHAT
-- =====================================================================

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

-- =====================================================================
-- 11. AI CONSULTATIONS
-- =====================================================================

CREATE TABLE IF NOT EXISTS ai_consultations (
    consultation_id  SERIAL PRIMARY KEY,
    user_id          INT NOT NULL,
    request_content  TEXT NOT NULL,
    response_content TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =====================================================================
-- 11. SYSTEM LOGS
-- =====================================================================

CREATE TABLE IF NOT EXISTS system_logs (
    log_id        SERIAL PRIMARY KEY,
    user_id       INT NULL,
    action        VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100),
    target_id     INT,
    detail        TEXT,
    "timestamp"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_log_user      ON system_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_log_timestamp ON system_logs("timestamp");

-- =====================================================================
-- 12. TRIGGER FUNCTIONS (business-rule guards, PostgreSQL PL/pgSQL)
-- Placed after schema; seed data is inserted before triggers are
-- active so historical demo rows are not blocked.
-- =====================================================================

-- Guard: new Open class cannot start in the past
CREATE OR REPLACE FUNCTION fn_classes_validate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'Open' AND NEW.start_date IS NOT NULL AND NEW.start_date < CURRENT_DATE THEN
        IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND OLD.start_date IS DISTINCT FROM NEW.start_date) THEN
            RAISE EXCEPTION 'A new Open class cannot start in the past. (code 51001)';
        END IF;
    END IF;

    IF NEW.coach_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM users u JOIN roles r ON r.role_id = u.role_id
            WHERE u.user_id = NEW.coach_id AND UPPER(r.role_name) = 'COACH'
        ) THEN
            RAISE EXCEPTION 'The assigned user must have the Coach role. (code 51002)';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_classes_validate ON classes;
CREATE TRIGGER trg_classes_validate
    BEFORE INSERT OR UPDATE ON classes
    FOR EACH ROW EXECUTE FUNCTION fn_classes_validate();

-- Guard: new session cannot be in the past; room must be Available
CREATE OR REPLACE FUNCTION fn_sessions_validate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.session_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'A new session cannot be created in the past. (code 51004)';
    END IF;

    IF TG_OP = 'INSERT' OR OLD.room_id IS DISTINCT FROM NEW.room_id THEN
        IF (SELECT status FROM rooms WHERE room_id = NEW.room_id) <> 'Available' THEN
            RAISE EXCEPTION 'New or reassigned sessions require an Available room. (code 51005)';
        END IF;
    END IF;

    IF (SELECT max_capacity FROM classes WHERE class_id = NEW.class_id) >
       (SELECT capacity     FROM rooms   WHERE room_id  = NEW.room_id) THEN
        RAISE EXCEPTION 'Class capacity cannot exceed room capacity. (code 51006)';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sessions_validate ON sessions;
CREATE TRIGGER trg_sessions_validate
    BEFORE INSERT OR UPDATE ON sessions
    FOR EACH ROW EXECUTE FUNCTION fn_sessions_validate();

-- Guard: room cannot be made unavailable while future sessions are scheduled
CREATE OR REPLACE FUNCTION fn_rooms_protect()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status IN ('Maintenance', 'Closed') AND NEW.status IS DISTINCT FROM OLD.status THEN
        IF EXISTS (
            SELECT 1 FROM sessions
            WHERE room_id = NEW.room_id
              AND status = 'Scheduled'
              AND session_date >= CURRENT_DATE
        ) THEN
            RAISE EXCEPTION 'Reassign or cancel future scheduled sessions before making the room unavailable. (code 51007)';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM sessions s
        JOIN classes c ON c.class_id = s.class_id
        WHERE s.room_id = NEW.room_id AND c.max_capacity > NEW.capacity
    ) THEN
        RAISE EXCEPTION 'Room capacity cannot be reduced below the capacity of a class using it. (code 51008)';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_rooms_protect ON rooms;
CREATE TRIGGER trg_rooms_protect
    BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION fn_rooms_protect();

-- Guard: cannot subscribe to an Inactive package
CREATE OR REPLACE FUNCTION fn_member_packages_validate()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'Active' THEN
        IF (SELECT status FROM membership_packages WHERE package_id = NEW.package_id) = 'Inactive' THEN
            RAISE EXCEPTION 'Inactive packages cannot be used for new registrations or renewals. (code 51009)';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_member_packages_validate ON member_packages;
CREATE TRIGGER trg_member_packages_validate
    BEFORE INSERT OR UPDATE ON member_packages
    FOR EACH ROW EXECUTE FUNCTION fn_member_packages_validate();

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- Roles
INSERT INTO roles (role_name, description) VALUES
('CenterManager', 'Quan ly trung tam - van hanh lop hoc, goi tap, bao cao'),
('Coach',         'Huan luyen vien'),
('Member',        'Thanh vien / hoc vien'),
('Receptionist',  'Nhan vien le tan'),
('Admin',         'Quan tri vien he thong - quan tri nguoi dung va phan quyen RBAC')
ON CONFLICT (role_name) DO NOTHING;

-- Permissions
INSERT INTO permissions (permission_name, description) VALUES
('MANAGE_USERS',         'Quan ly nguoi dung'),
('MANAGE_CLASSES',       'Quan ly lop hoc'),
('MANAGE_PACKAGES',      'Quan ly goi thanh vien'),
('VIEW_REPORTS',         'Xem bao cao'),
('MANAGE_TRAINING_PLAN', 'Tao/sua ke hoach tap luyen'),
('RECORD_RESULT',        'Ghi nhan ket qua buoi tap'),
('PROCESS_PAYMENT',      'Ghi nhan thanh toan, xuat hoa don'),
('HANDLE_SUPPORT',       'Xu ly yeu cau ho tro'),
('VIEW_AUDIT_LOG',       'Xem lich su thao tac he thong'),
('MANAGE_RBAC',          'Phan quyen va quan tri vai tro he thong'),
('REGISTER_MEMBER',      'Dang ky thanh vien moi tai quay'),
('MANAGE_SUBSCRIPTIONS', 'Dang ky va gia han goi tap'),
('MANAGE_PERMISSIONS',   'Chi quan tri vien duoc cau hinh ma tran phan quyen'),
('MANAGE_INVOICES',      'Quan ly va xem hoa don cua cac thanh vien'),
('CHAT_AS_MEMBER',       'Member may access their own support chat'),
('HANDLE_CHAT',          'Receptionist and CenterManager may handle support chats'),
('LOCK_SUBJECTS',        'Khoa hoac mo mon hoc'),
('MANAGE_SPECIALIZATIONS', 'Quan ly chuyen mon huan luyen vien')
ON CONFLICT (permission_name) DO NOTHING;

-- Role → Permission assignments by names, not sequence-specific IDs.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM (VALUES
    ('CenterManager', 'MANAGE_USERS'),
    ('CenterManager', 'MANAGE_CLASSES'),
    ('CenterManager', 'MANAGE_PACKAGES'),
    ('CenterManager', 'VIEW_REPORTS'),
    ('CenterManager', 'PROCESS_PAYMENT'),
    ('CenterManager', 'HANDLE_SUPPORT'),
    ('CenterManager', 'VIEW_AUDIT_LOG'),
    ('CenterManager', 'MANAGE_INVOICES'),
    ('CenterManager', 'HANDLE_CHAT'),
    ('Coach', 'MANAGE_TRAINING_PLAN'),
    ('Coach', 'RECORD_RESULT'),
    ('Receptionist', 'RECORD_RESULT'),
    ('Receptionist', 'PROCESS_PAYMENT'),
    ('Receptionist', 'HANDLE_SUPPORT'),
    ('Receptionist', 'REGISTER_MEMBER'),
    ('Receptionist', 'MANAGE_SUBSCRIPTIONS'),
    ('Receptionist', 'MANAGE_INVOICES'),
    ('Receptionist', 'HANDLE_CHAT'),
    ('Member', 'CHAT_AS_MEMBER'),
    ('Admin', 'MANAGE_USERS'),
    ('Admin', 'VIEW_REPORTS'),
    ('Admin', 'VIEW_AUDIT_LOG'),
    ('Admin', 'MANAGE_RBAC'),
    ('Admin', 'MANAGE_PERMISSIONS'),
    ('Admin', 'LOCK_SUBJECTS'),
    ('Admin', 'MANAGE_SPECIALIZATIONS')
) AS defaults(role_name, permission_name)
JOIN roles r ON UPPER(r.role_name) = UPPER(defaults.role_name)
JOIN permissions p ON p.permission_name = defaults.permission_name
ON CONFLICT DO NOTHING;
INSERT INTO subjects (subject_name, description, normal_price, advanced_price, status, slug) VALUES
('Bóng đá',            'Môn thể thao vua ngoài trời, rèn luyện thể lực và phối hợp',         1000000, 3500000, 'Active', 'bong-da'),
('Quần vợt (Tennis)',  'Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực',             2500000, 8000000, 'Active', 'quan-vot-tennis'),
('Bơi lội',            'Môn thể thao dưới nước ngoài trời, phát triển toàn diện cơ thể',     1500000, 4500000, 'Active', 'boi-loi'),
('Bắn cung',           'Môn ngoài trời, rèn luyện sự tập trung cao độ và tính kiên nhẫn',   2000000, 6000000, 'Active', 'ban-cung'),
('Cầu lông',           'Môn thể thao nhanh nhẹn, linh hoạt trong nhà',                        900000, 3000000, 'Active', 'cau-long'),
('Bóng bàn',           'Phản xạ nhanh, khéo léo với nhịp độ cao trong nhà',                   800000, 2500000, 'Active', 'bong-ban'),
('Bóng rổ',            'Môn thể thao đồng đội tốc độ và sức bật trong nhà',                  1200000, 3500000, 'Active', 'bong-ro'),
('Bóng chuyền',        'Phối hợp đồng đội và kỹ thuật phản xạ trên lưới trong nhà',          1000000, 3500000, 'Active', 'bong-chuyen'),
('Pickleball',         'Môn thể thao thời thượng trong nhà, kết hợp tennis, bóng bàn',       1800000, 5000000, 'Active', 'pickleball')
ON CONFLICT (slug) DO NOTHING;


-- SOURCE: database/migrations/06_schedule_safety_postgres.sql SHA256 7000586a3366fe78af5ec081f7457775d543eb2f6cfff380c7b3192148d71dc4
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


-- SOURCE: database/migrations/07_session_subscription_postgres.sql SHA256 091b1a86dc34b3a345f46f1d71d8a28b1d1b41562c69ba7a77c5bdfc27a0a619
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


-- SOURCE: database/migrations/08_advanced_entitlements_postgres.sql SHA256 763a7b70440583498eb86a2c50963681c7c934f1a9f7e940bc3c1579902b22e8
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


-- SOURCE: database/migrations/09_approved_catalog_postgres.sql SHA256 3d27cf23c6ac511fdc06bccc24b1e7ea04134f1163bbe9d8bb3931492e250d81
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


-- SOURCE: database/migrations/10_session_incidents_postgres.sql SHA256 9e4234c7b1679777de95f76d89b4f2b184f3473d3787355f79adcfb448965bbf

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


-- SOURCE: database/migrations/11_holiday_history_postgres.sql SHA256 55fe61d4cef7e43bb3575a1cde57a9afee24480ce9082ae6fad43fc8df131e13

ALTER TABLE holidays ADD COLUMN IF NOT EXISTS actual_closed_until TIMESTAMP;
-- No guessed backfill. An active holiday's actual past interval is captured on deactivation.


-- SOURCE: database/migrations/12_payment_resolutions_postgres.sql SHA256 4e69f22e359367843d7d6d90641e5af0acabae847558bf23eb1ca7cce3382ce6

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


-- SOURCE: database/migrations/13_room_catalog_postgres.sql SHA256 56db046f8324a2455d2e6dea8aa1011bc31683a7d2bfe5958c4cd44ca5f82a9b
-- Owner02/11: numeric room_id + room_code; 20 center1 resources from room.jpg.
-- Additive/atomic. Existing history remains on legacy rooms. No subject_id.

SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE;
LOCK TABLE rooms, sessions, classes, class_enrollments IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS room_code VARCHAR(40);
UPDATE rooms SET room_code='C'||lpad(center_id::text,greatest(2,length(center_id::text)),'0')||'-LEGACY-'||room_id WHERE room_code IS NULL;
ALTER TABLE rooms ALTER COLUMN room_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_rooms_code ON rooms(room_code);
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='rooms'::regclass AND conname='chk_room_code') THEN
  ALTER TABLE rooms ADD CONSTRAINT chk_room_code CHECK (
   room_code ~ '^C[0-9]{2,}-[A-Z0-9]+-[A-Z0-9]+$'
   AND split_part(room_code,'-',1)='C'||lpad(center_id::text,greatest(2,length(center_id::text)),'0'));
 END IF;
END $$;
-- Allows a rolling deploy: old writers omit code; the numeric ID remains stable.
CREATE OR REPLACE FUNCTION fn_room_code_default() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.room_code IS NULL THEN NEW.room_code:='C'||lpad(NEW.center_id::text,greatest(2,length(NEW.center_id::text)),'0')||'-LEGACY-'||NEW.room_id; END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_room_code_default ON rooms;
CREATE TRIGGER trg_room_code_default BEFORE INSERT ON rooms FOR EACH ROW EXECUTE FUNCTION fn_room_code_default();

-- Catalog: guarded once; replay preserves later authorized edits.
CREATE TABLE IF NOT EXISTS scms_data_migration_audit (
 migration_key VARCHAR(100) PRIMARY KEY,captured_at TIMESTAMP NOT NULL,before_data TEXT NOT NULL,after_data TEXT NOT NULL);
DO $catalog$
DECLARE old_ids INT[]; before_state TEXT; unknown_rooms TEXT; schedule_issues TEXT; now_at TIMESTAMP:=clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh';
BEGIN
 IF EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261102_room_catalog_v1') THEN RETURN; END IF;
 IF NOT EXISTS(SELECT 1 FROM centers WHERE center_id=1) THEN RAISE EXCEPTION 'ROOM_CATALOG_CENTER_MISSING: center1 must exist'; END IF;
 CREATE TEMP TABLE _room_catalog(code TEXT PRIMARY KEY,name TEXT,location TEXT,kind TEXT,capacity INT) ON COMMIT DROP;
 INSERT INTO _room_catalog VALUES
 ('C01-TEN-01','Sân Tennis 01','Khu A – Ngoài trời','Outdoor',4),
 ('C01-TEN-02','Sân Tennis 02','Khu A – Ngoài trời','Outdoor',4),
 ('C01-FOOT-01','Sân Bóng đá 01','Khu B – Ngoài trời','Outdoor',15),
 ('C01-FOOT-02','Sân Bóng đá 02','Khu B – Ngoài trời','Outdoor',15),
 ('C01-ARC-01','Khu Bắn cung 01','Khu C – Ngoài trời','Outdoor',5),
 ('C01-ARC-02','Khu Bắn cung 02','Khu C – Ngoài trời','Outdoor',5),
 ('C01-BR-01','Sân Bóng rổ 01','Nhà thi đấu A','Indoor',8),
 ('C01-BR-02','Sân Bóng rổ 02','Nhà thi đấu A','Indoor',8),
 ('C01-BC-01','Sân Bóng chuyền 01','Nhà thi đấu A','Indoor',8),
 ('C01-BC-02','Sân Bóng chuyền 02','Nhà thi đấu A','Indoor',8),
 ('C01-PICK-01','Sân Pickleball 01','Nhà thi đấu B','Indoor',4),
 ('C01-PICK-02','Sân Pickleball 02','Nhà thi đấu B','Indoor',4),
 ('C01-BAD-01','Sân Cầu lông 01','Nhà thi đấu B','Indoor',4),
 ('C01-BAD-02','Sân Cầu lông 02','Nhà thi đấu B','Indoor',4),
 ('C01-TT-01','Bàn Bóng bàn 01','Phòng Bóng bàn','Indoor',2),
 ('C01-TT-02','Bàn Bóng bàn 02','Phòng Bóng bàn','Indoor',2),
 ('C01-TT-03','Bàn Bóng bàn 03','Phòng Bóng bàn','Indoor',2),
 ('C01-TT-04','Bàn Bóng bàn 04','Phòng Bóng bàn','Indoor',2),
 ('C01-SWIM-01','Hồ bơi 01','Khu hồ bơi','Outdoor',10),
 ('C01-SWIM-02','Hồ bơi 02','Khu hồ bơi','Outdoor',10);
 SELECT coalesce(array_agg(room_id),'{}'::INT[]) INTO old_ids FROM rooms WHERE center_id=1 AND room_code NOT IN(SELECT code FROM _room_catalog);
 -- A resource already named in the approved 20-room catalog is not a custom room.
 -- In particular the existing Tennis02 must be accepted alongside the old eight.
 SELECT jsonb_agg(to_jsonb(blocked) ORDER BY room_id)::text INTO unknown_rooms FROM (
 SELECT room_id,room_name,status FROM rooms WHERE room_id=ANY(old_ids) AND status<>'Closed'
 AND room_name NOT IN(SELECT name FROM _room_catalog) AND room_name NOT IN(
  'Sân Tennis 01','Sân Tennis ngoài trời 1','Hồ bơi 01','Hồ bơi ngoài trời tiêu chuẩn',
  'Sân Pickleball 01','Sân Pickleball có mái che','Khu Bắn cung 01','Trường bắn ngoài trời',
  'Sân đa năng 01','Nhà thi đấu đa năng (Bóng rổ & Chuyền)','Sân Bóng đá 01','Sân cỏ ngoài trời (Bóng đá)',
  'Sân Cầu lông 01','Nhà thi đấu Cầu lông (Sàn thảm)','Khu Bóng bàn 01','Phòng Bóng bàn tiêu chuẩn')
 ORDER BY room_id LIMIT 20) blocked;
 IF unknown_rooms IS NOT NULL THEN
  RAISE EXCEPTION 'ROOM_CATALOG_CUSTOM_ROOM: reconcile noncanonical active rooms before replacing catalog'
   USING DETAIL=unknown_rooms,HINT='Only approved catalog names and known old aliases may be retired automatically; reconcile the listed rooms without removing this guard.';
 END IF;
 IF EXISTS(SELECT 1 FROM rooms r JOIN _room_catalog c ON c.code=r.room_code WHERE
  r.center_id<>1 OR r.room_name<>c.name OR r.location IS DISTINCT FROM c.location OR r.capacity IS DISTINCT FROM c.capacity OR r.facility_type<>c.kind OR r.status<>'Available') THEN
  RAISE EXCEPTION 'ROOM_CATALOG_CODE_CONFLICT: a requested code already has different metadata';
 END IF;
 CREATE TEMP TABLE _room_moves(session_id INT PRIMARY KEY,target_code TEXT) ON COMMIT DROP;
 INSERT INTO _room_moves
 SELECT planned.session_id,coalesce(named.code,planned.default_code)
 FROM (SELECT s.session_id,old_room.room_name,CASE su.slug
  WHEN 'quan-vot-tennis' THEN 'C01-TEN-01' WHEN 'boi-loi' THEN 'C01-SWIM-01' WHEN 'pickleball' THEN 'C01-PICK-01'
  WHEN 'ban-cung' THEN 'C01-ARC-01' WHEN 'bong-ro' THEN 'C01-BR-01' WHEN 'bong-chuyen' THEN 'C01-BC-01'
  WHEN 'bong-da' THEN 'C01-FOOT-01' WHEN 'cau-long' THEN 'C01-BAD-01' WHEN 'bong-ban' THEN 'C01-TT-01' END AS default_code
 FROM sessions s JOIN classes c ON c.class_id=s.class_id LEFT JOIN subjects su ON su.subject_id=c.subject_id
 JOIN rooms old_room ON old_room.room_id=s.room_id
 WHERE s.room_id=ANY(old_ids) AND s.status='Scheduled' AND NOT s.center_invalidated AND s.session_date>=now_at::date) planned
 LEFT JOIN _room_catalog named ON named.name=planned.room_name
  AND split_part(named.code,'-',2)=split_part(planned.default_code,'-',2);
 SELECT jsonb_agg(to_jsonb(issue) ORDER BY session_id)::text INTO schedule_issues FROM (
 SELECT s.session_id,cl.class_id,cl.class_name,cl.center_id,m.target_code,
  s.session_date+s.start_time AS session_start,cl.max_capacity,c.capacity AS target_capacity,seats.held_seats,
  array_remove(ARRAY[
   CASE WHEN c.code IS NULL THEN 'UNKNOWN_SUBJECT_OR_TARGET' END,
   CASE WHEN cl.center_id<>1 THEN 'WRONG_CENTER' END,
   CASE WHEN s.session_date+s.start_time<=now_at THEN 'STARTED_OR_PAST_SCHEDULED' END,
   CASE WHEN cl.max_capacity IS NULL THEN 'MISSING_CLASS_CAPACITY' END,
   CASE WHEN cl.max_capacity>c.capacity THEN 'CLASS_CAPACITY_EXCEEDS_ROOM' END,
   CASE WHEN seats.held_seats>c.capacity THEN 'ENROLLMENTS_EXCEED_ROOM' END
  ],NULL) AS reasons
 FROM _room_moves m JOIN sessions s USING(session_id) JOIN classes cl ON cl.class_id=s.class_id LEFT JOIN _room_catalog c ON c.code=m.target_code
 CROSS JOIN LATERAL(SELECT count(*) AS held_seats FROM class_enrollments e WHERE e.class_id=cl.class_id AND e.status IN ('Registered','Pending')) seats
  WHERE c.code IS NULL OR cl.center_id<>1 OR s.session_date+s.start_time<=now_at OR cl.max_capacity IS NULL OR cl.max_capacity>c.capacity
   OR seats.held_seats>c.capacity
 ORDER BY s.session_id LIMIT 20) issue;
 IF schedule_issues IS NOT NULL THEN
  RAISE EXCEPTION 'ROOM_CATALOG_SCHEDULE_REVIEW: resolve in-progress/unknown-subject/over-capacity classes before migration'
   USING DETAIL=schedule_issues,HINT='Resolve the listed reasons first. This migration never shrinks classes, removes members or rewrites past sessions automatically.';
 END IF;
 SELECT jsonb_build_object('rooms',(SELECT jsonb_agg(to_jsonb(r) ORDER BY r.room_id) FROM rooms r WHERE room_id=ANY(old_ids)),
  'sessions',(SELECT jsonb_agg(to_jsonb(s) ORDER BY s.session_id) FROM sessions s JOIN _room_moves m USING(session_id)))::text INTO before_state;
 DROP INDEX IF EXISTS uq_scms_rooms_center_name;
 INSERT INTO rooms(room_code,room_name,location,capacity,status,facility_type,center_id)
 SELECT code,name,location,capacity,'Available',kind,1 FROM _room_catalog ORDER BY code ON CONFLICT(room_code) DO NOTHING;
 IF EXISTS(SELECT 1 FROM _room_moves m JOIN sessions a ON a.session_id=m.session_id JOIN rooms target ON target.room_code=m.target_code
  JOIN sessions b ON b.session_id<>a.session_id AND b.session_date=a.session_date AND a.start_time<b.end_time AND b.start_time<a.end_time
  LEFT JOIN _room_moves mb ON mb.session_id=b.session_id
  WHERE b.status<>'Cancelled' AND NOT b.center_invalidated AND (b.room_id=target.room_id OR mb.target_code=m.target_code)) THEN
  RAISE EXCEPTION 'ROOM_CATALOG_OVERLAP: resolve target-room conflicts before migration';
 END IF;
 UPDATE sessions s SET room_id=r.room_id FROM _room_moves m JOIN rooms r ON r.room_code=m.target_code WHERE s.session_id=m.session_id;
 UPDATE rooms SET status='Closed' WHERE room_id=ANY(old_ids) AND status<>'Closed';
 CREATE UNIQUE INDEX uq_scms_rooms_center_name ON rooms(center_id,lower(room_name)) WHERE status='Available';
 INSERT INTO scms_data_migration_audit VALUES('20261102_room_catalog_v1',now_at,before_state,
  jsonb_build_object('rooms',(SELECT jsonb_agg(to_jsonb(r) ORDER BY r.room_id) FROM rooms r WHERE center_id=1),
  'sessions',(SELECT jsonb_agg(to_jsonb(s) ORDER BY s.session_id) FROM sessions s JOIN _room_moves m USING(session_id)))::text);
END $catalog$;


-- SOURCE: database/migrations/14_absence_attendance_postgres.sql SHA256 1fc7b787aaf939bb1fc445d80e85cb9b26424969b56b0831c5642b385ded55b3
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


-- SOURCE: database/migrations/15_QueryDR.sql SHA256 febd1e74c7b5d35aa749c0f65d5ed2bc91cadf24eb38f10f5870f2332627e33d
-- SCMS QueryDR, Owner approved 04/11/2026.
-- Copy this ENTIRE file into the Supabase SQL editor and run once before deploying BE.
-- Additive only: no existing table/column/data is updated, deleted or rebuilt.
-- A failed guard rolls back this whole transaction. Replay validates the same structure.

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


-- SOURCE: database/migrations/13_google_login_postgres.sql SHA256 984566d55a76ceb13a136792ef898c4a187ea03594a4b69144fb8143ac5e39a7

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_sub ON public.users (google_sub) WHERE google_sub IS NOT NULL;


-- SOURCE: database/migrations/17_attendance_review_postgres.sql SHA256 61dab705a446f57dac31e60aa40f49c161555f5f9e473a749f4c3346cc3aae03
-- D15 Owner09/11: lifecycle at end; current no-show state is reviewed after local midnight.
-- Run before deploying the matching BE. No seed, reset, historical notification queue, or attendance edits.

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


-- SOURCE: database/migrations/18_audit_scope_postgres.sql SHA256 5945eb64c6594f784c639ae2018f5c732057f6e70ca0217b3b76ffe9115ce95d
-- RP-43: event-time provenance for new audit events. Existing history stays unchanged.

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


-- SOURCE: database/migrations/19_room_activity_postgres.sql SHA256 fb12ecd02bbfd9897cc4e2aec78de73aa6653593ad917b4d83b7b456330894ac
-- Physical court type for Owner02/11's twenty resources. No rooms.subject_id, no runtime name/code inference.

DO $$ BEGIN
 IF to_regclass('public.rooms') IS NULL OR to_regclass('public.scms_data_migration_audit') IS NULL THEN
  RAISE EXCEPTION 'Migration13 room catalog baseline is required';
 END IF;
END $$;
SELECT center_id FROM public.centers WHERE center_id=1 FOR UPDATE;
LOCK TABLE public.rooms, public.sessions, public.classes IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE public.rooms ADD COLUMN IF NOT EXISTS activity_type VARCHAR(24);
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='rooms'
     AND column_name='activity_type' AND data_type='character varying' AND character_maximum_length=24 AND is_nullable='YES') THEN
  RAISE EXCEPTION 'Incompatible rooms.activity_type; reconcile before deploy';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.rooms'::regclass AND conname='ck_rooms_activity_type') THEN
  ALTER TABLE public.rooms ADD CONSTRAINT ck_rooms_activity_type CHECK(activity_type IN
   ('TENNIS','FOOTBALL','ARCHERY','BASKETBALL','VOLLEYBALL','PICKLEBALL','BADMINTON','TABLE_TENNIS','SWIMMING'));
 END IF;
END $$;
DO $activity$
DECLARE snapshot JSONB; before_state TEXT;
BEGIN
 IF EXISTS(SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261109_room_activity_v1') THEN RETURN; END IF;
 SELECT after_data::jsonb INTO snapshot FROM public.scms_data_migration_audit WHERE migration_key='20261102_room_catalog_v1';
 IF snapshot IS NULL THEN RAISE EXCEPTION 'Approved immutable room catalog snapshot is required; do not infer from current room names/codes'; END IF;
 CREATE TEMP TABLE _approved_activity(room_id INT PRIMARY KEY, activity TEXT NOT NULL) ON COMMIT DROP;
 -- These codes identify rows in the immutable approved snapshot, not the current editable catalog.
 INSERT INTO _approved_activity
 SELECT (r->>'room_id')::int, c.activity FROM jsonb_array_elements(snapshot->'rooms') r
 JOIN (VALUES
  ('C01-TEN-01','TENNIS'),('C01-TEN-02','TENNIS'),('C01-FOOT-01','FOOTBALL'),('C01-FOOT-02','FOOTBALL'),
  ('C01-ARC-01','ARCHERY'),('C01-ARC-02','ARCHERY'),('C01-BR-01','BASKETBALL'),('C01-BR-02','BASKETBALL'),
  ('C01-BC-01','VOLLEYBALL'),('C01-BC-02','VOLLEYBALL'),('C01-PICK-01','PICKLEBALL'),('C01-PICK-02','PICKLEBALL'),
  ('C01-BAD-01','BADMINTON'),('C01-BAD-02','BADMINTON'),('C01-TT-01','TABLE_TENNIS'),('C01-TT-02','TABLE_TENNIS'),
  ('C01-TT-03','TABLE_TENNIS'),('C01-TT-04','TABLE_TENNIS'),('C01-SWIM-01','SWIMMING'),('C01-SWIM-02','SWIMMING')
 ) c(code,activity) ON r->>'room_code'=c.code;
 IF (SELECT count(*) FROM _approved_activity)<>20 OR
    (SELECT count(*) FROM _approved_activity a JOIN public.rooms r USING(room_id) WHERE r.center_id=1)<>20 THEN
  RAISE EXCEPTION 'Approved twenty room IDs no longer match center1; reconcile explicitly';
 END IF;
 IF EXISTS(SELECT 1 FROM _approved_activity a JOIN public.rooms r USING(room_id) WHERE r.activity_type IS NOT NULL AND r.activity_type<>a.activity) THEN
  RAISE EXCEPTION 'Existing activity conflicts with the approved snapshot; reconcile explicitly';
 END IF;
 SELECT jsonb_agg(to_jsonb(r) ORDER BY room_id)::text INTO before_state FROM public.rooms r JOIN _approved_activity USING(room_id);
 UPDATE public.rooms r SET activity_type=a.activity FROM _approved_activity a WHERE r.room_id=a.room_id AND r.activity_type IS NULL;
 INSERT INTO public.scms_data_migration_audit VALUES('20261109_room_activity_v1',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',before_state,
  (SELECT jsonb_agg(to_jsonb(r) ORDER BY room_id)::text FROM public.rooms r JOIN _approved_activity USING(room_id)));
END $activity$;
DO $$ DECLARE issues TEXT; BEGIN
 SELECT string_agg(s.session_id::text,',' ORDER BY s.session_id) INTO issues
 FROM public.sessions s JOIN public.classes c USING(class_id) JOIN public.subjects sub USING(subject_id) JOIN public.rooms r USING(room_id)
 WHERE c.center_id=1 AND s.status='Scheduled' AND s.session_date+s.end_time > clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh'
 AND (r.activity_type IS NULL OR r.center_id<>c.center_id OR
  r.activity_type IS DISTINCT FROM CASE sub.slug WHEN 'quan-vot-tennis' THEN 'TENNIS' WHEN 'bong-da' THEN 'FOOTBALL'
   WHEN 'ban-cung' THEN 'ARCHERY' WHEN 'bong-ro' THEN 'BASKETBALL' WHEN 'bong-chuyen' THEN 'VOLLEYBALL'
   WHEN 'pickleball' THEN 'PICKLEBALL' WHEN 'cau-long' THEN 'BADMINTON' WHEN 'bong-ban' THEN 'TABLE_TENNIS' WHEN 'boi-loi' THEN 'SWIMMING' END);
 IF issues IS NOT NULL THEN RAISE EXCEPTION 'ROOM_ACTIVITY_SCHEDULE_CONFLICT: Reconcile existing future sessions before deploy' USING DETAIL=issues; END IF;
END $$;
-- Historical sessions, links, names, codes, prices and invoices are untouched. Replay preserves later authorized edits.


-- SOURCE: database/migrations/20_class_publication_price_postgres.sql SHA256 1df1fb393c26327f5ce1fd8e013c87da2429575fc5f11bfdba0e699554866dc7
-- RP-13: only new, unpublished Normal drafts may choose their first publication price.

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


-- SOURCE: database/migrations/21_normal_lifecycle_postgres.sql SHA256 0588f53a5b13f5bea01c85b77e5babd1549bbd61e4c297c633ad7f8ccbfafbf1
-- Normal lifecycle metadata. Additive upgrade; no guessing legacy confirmation/minimum or changing receipts.

DO $$ BEGIN
 IF to_regclass('public.classes') IS NULL OR to_regclass('public.sessions') IS NULL THEN
  RAISE EXCEPTION 'Normal lifecycle requires the classes/sessions baseline';
 END IF;
END $$;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS min_capacity INT;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_confirmed_by INT REFERENCES public.users(user_id);
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_confirmed_at TIMESTAMP;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_reason VARCHAR(2000);
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS postponement_count INT NOT NULL DEFAULT 0;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS lifecycle_version BIGINT NOT NULL DEFAULT 0;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes'
   AND ((column_name IN ('min_capacity','opening_confirmed_by','postponement_count') AND data_type<>'integer')
     OR (column_name='lifecycle_version' AND data_type<>'bigint')
     OR (column_name='opening_confirmed_at' AND data_type<>'timestamp without time zone')
     OR (column_name='opening_reason' AND (data_type<>'character varying' OR character_maximum_length<>2000)))) THEN
  RAISE EXCEPTION 'Incompatible Normal lifecycle columns; reconcile before upgrade';
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes'
   AND column_name IN ('postponement_count','lifecycle_version') AND (is_nullable<>'NO' OR coalesce(column_default,'')<>'0')) THEN
  RAISE EXCEPTION 'Incompatible Normal lifecycle defaults/nullability; reconcile before upgrade';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
   WHERE c.conrelid='public.classes'::regclass AND c.confrelid='public.users'::regclass AND c.contype='f' AND a.attname='opening_confirmed_by') THEN
  ALTER TABLE public.classes ADD CONSTRAINT fk_normal_opening_actor FOREIGN KEY(opening_confirmed_by) REFERENCES public.users(user_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_capacity') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_capacity CHECK(min_capacity IS NULL OR (min_capacity>0 AND min_capacity<=max_capacity));
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_postponement') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_postponement CHECK(postponement_count IN (0,1) AND lifecycle_version>=0);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_confirmation') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_confirmation CHECK((opening_confirmed_by IS NULL)=(opening_confirmed_at IS NULL));
 END IF;
END $$;
-- NULL minimum/confirmation on legacy rows is deliberate: the matching BE blocks confirmation until reviewed.


-- SOURCE: database/migrations/22_normal_changes_refunds_postgres.sql SHA256 5bdcbbcb1ef1c165607667b84c8d67a08372b5149a041342993c79469585ad86
-- Additive Normal consent/entitlement/manual-refund model. No cleanup/seed/provider operations.

ALTER TABLE classes ADD COLUMN IF NOT EXISTS change_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS entitlement_class_id INT REFERENCES classes(class_id);
DO $guard$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes' AND column_name='change_pending' AND data_type='boolean' AND is_nullable='NO' AND column_default='false')
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='invoices' AND column_name='entitlement_class_id' AND data_type='integer' AND is_nullable='YES') THEN
  RAISE EXCEPTION 'Incompatible Normal change columns';
 END IF;
END $guard$;
CREATE TABLE IF NOT EXISTS normal_proposals (
 id SERIAL PRIMARY KEY, class_id INT NOT NULL REFERENCES classes(class_id), target_class_id INT REFERENCES classes(class_id),
 kind VARCHAR(20) NOT NULL CHECK(kind IN ('Postpone','Transfer')), status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK(status IN ('Pending','Applied','Cancelled')),
 shift_days INT, version BIGINT NOT NULL, fingerprint VARCHAR(64) NOT NULL, schedule_json TEXT NOT NULL, reason VARCHAR(2000) NOT NULL,
 created_by INT NOT NULL REFERENCES users(user_id), created_at TIMESTAMP NOT NULL,
 CHECK((kind='Postpone' AND shift_days BETWEEN 1 AND 366 AND target_class_id IS NULL) OR (kind='Transfer' AND shift_days IS NULL AND target_class_id IS NOT NULL AND target_class_id<>class_id))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_normal_pending_proposal ON normal_proposals(class_id) WHERE status='Pending';
CREATE TABLE IF NOT EXISTS normal_responses (
 id SERIAL PRIMARY KEY, proposal_id INT NOT NULL REFERENCES normal_proposals(id), invoice_id INT NOT NULL REFERENCES invoices(invoice_id),
 member_id INT NOT NULL REFERENCES users(user_id), choice VARCHAR(20) NOT NULL CHECK(choice IN ('Waiting','Accept','Refund')),
 version BIGINT NOT NULL, responded_at TIMESTAMP NOT NULL, UNIQUE(proposal_id,invoice_id)
);
CREATE TABLE IF NOT EXISTS normal_transfers (
 id SERIAL PRIMARY KEY, proposal_id INT NOT NULL REFERENCES normal_proposals(id), invoice_id INT NOT NULL REFERENCES invoices(invoice_id),
 source_enrollment_id INT NOT NULL REFERENCES class_enrollments(enrollment_id), target_enrollment_id INT NOT NULL REFERENCES class_enrollments(enrollment_id),
 consented_by INT NOT NULL REFERENCES users(user_id), transferred_at TIMESTAMP NOT NULL,
 UNIQUE(proposal_id,invoice_id), CHECK(source_enrollment_id<>target_enrollment_id)
);
CREATE TABLE IF NOT EXISTS normal_refunds (
 id INT PRIMARY KEY REFERENCES payment_resolutions(id), class_id INT NOT NULL REFERENCES classes(class_id), origin VARCHAR(30) NOT NULL,
 receiver_id INT, approved_receiver_id INT, version BIGINT NOT NULL DEFAULT 0, due_at TIMESTAMP, first_due_at TIMESTAMP, issue_note VARCHAR(2000),
 CHECK(origin IN ('MemberChoice','CenterCancellation','LateReceipt'))
);
CREATE TABLE IF NOT EXISTS normal_refund_receivers (
 id SERIAL PRIMARY KEY, refund_id INT NOT NULL REFERENCES normal_refunds(id), bank VARCHAR(120) NOT NULL, account_number VARCHAR(40) NOT NULL,
 account_name VARCHAR(150) NOT NULL, confirmed_by INT NOT NULL REFERENCES users(user_id), confirmed_at TIMESTAMP NOT NULL,
 verified_by INT REFERENCES users(user_id), verified_at TIMESTAMP, verification_source VARCHAR(2000), UNIQUE(id,refund_id),
 CHECK((verified_by IS NULL AND verified_at IS NULL) OR (verified_by IS NOT NULL AND verified_at IS NOT NULL AND verification_source IS NOT NULL))
);
DO $fk$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='normal_refunds'::regclass AND conname='fk_normal_refund_receiver') THEN
  ALTER TABLE normal_refunds ADD CONSTRAINT fk_normal_refund_receiver FOREIGN KEY(receiver_id,id) REFERENCES normal_refund_receivers(id,refund_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='normal_refunds'::regclass AND conname='fk_normal_refund_approved_receiver') THEN
  ALTER TABLE normal_refunds ADD CONSTRAINT fk_normal_refund_approved_receiver FOREIGN KEY(approved_receiver_id,id) REFERENCES normal_refund_receivers(id,refund_id);
 END IF;
END $fk$;
CREATE TABLE IF NOT EXISTS normal_refund_attempts (
 id SERIAL PRIMARY KEY, refund_id INT NOT NULL REFERENCES normal_refunds(id), receiver_id INT NOT NULL,
 claim_key VARCHAR(36) NOT NULL UNIQUE, claimed_by INT NOT NULL REFERENCES users(user_id), claimed_at TIMESTAMP NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'Claimed' CHECK(status IN ('Claimed','Submitted','Verified','Failed','Reversed')),
 amount NUMERIC(12,2) NOT NULL CHECK(amount>0), currency VARCHAR(3) NOT NULL CHECK(currency='VND'), bank_reference VARCHAR(120) UNIQUE,
 performed_at TIMESTAMP, verified_by INT REFERENCES users(user_id), verified_at TIMESTAMP, bank_source VARCHAR(2000), outcome_note VARCHAR(2000),
 FOREIGN KEY(receiver_id,refund_id) REFERENCES normal_refund_receivers(id,refund_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_normal_refund_active_attempt ON normal_refund_attempts(refund_id) WHERE status IN ('Claimed','Submitted','Verified');
CREATE TABLE IF NOT EXISTS normal_refund_documents (
 id SERIAL PRIMARY KEY, attempt_id INT NOT NULL REFERENCES normal_refund_attempts(id), uploaded_by INT NOT NULL REFERENCES users(user_id),
 uploaded_at TIMESTAMP NOT NULL, content_type VARCHAR(30) NOT NULL CHECK(content_type IN ('application/pdf','image/png','image/jpeg')),
 sha256 VARCHAR(64) NOT NULL, content BYTEA NOT NULL CHECK(octet_length(content) BETWEEN 1 AND 5242880)
);
CREATE TABLE IF NOT EXISTS normal_refund_verifications (
 id SERIAL PRIMARY KEY, attempt_id INT NOT NULL REFERENCES normal_refund_attempts(id), verified_by INT NOT NULL REFERENCES users(user_id),
 verified_at TIMESTAMP NOT NULL, outcome VARCHAR(20) NOT NULL CHECK(outcome IN ('Success','Failed','Reversed','Pending')),
 bank_source VARCHAR(2000) NOT NULL, note VARCHAR(2000) NOT NULL
);
-- Column compatibility is checked on replay too; IF NOT EXISTS is not schema validation.
DO $model$
DECLARE item RECORD;
BEGIN
 FOR item IN SELECT * FROM (VALUES
('normal_proposals','id','integer',true),
('normal_proposals','class_id','integer',true),
('normal_proposals','target_class_id','integer',false),
('normal_proposals','kind','character varying(20)',true),
('normal_proposals','status','character varying(20)',true),
('normal_proposals','shift_days','integer',false),
('normal_proposals','version','bigint',true),
('normal_proposals','fingerprint','character varying(64)',true),
('normal_proposals','schedule_json','text',true),
('normal_proposals','reason','character varying(2000)',true),
('normal_proposals','created_by','integer',true),
('normal_proposals','created_at','timestamp without time zone',true),
('normal_responses','id','integer',true),
('normal_responses','proposal_id','integer',true),
('normal_responses','invoice_id','integer',true),
('normal_responses','member_id','integer',true),
('normal_responses','choice','character varying(20)',true),
('normal_responses','version','bigint',true),
('normal_responses','responded_at','timestamp without time zone',true),
('normal_transfers','id','integer',true),
('normal_transfers','proposal_id','integer',true),
('normal_transfers','invoice_id','integer',true),
('normal_transfers','source_enrollment_id','integer',true),
('normal_transfers','target_enrollment_id','integer',true),
('normal_transfers','consented_by','integer',true),
('normal_transfers','transferred_at','timestamp without time zone',true),
('normal_refunds','id','integer',true),
('normal_refunds','class_id','integer',true),
('normal_refunds','origin','character varying(30)',true),
('normal_refunds','receiver_id','integer',false),
('normal_refunds','approved_receiver_id','integer',false),
('normal_refunds','version','bigint',true),
('normal_refunds','due_at','timestamp without time zone',false),
('normal_refunds','first_due_at','timestamp without time zone',false),
('normal_refunds','issue_note','character varying(2000)',false),
('normal_refund_receivers','id','integer',true),
('normal_refund_receivers','refund_id','integer',true),
('normal_refund_receivers','bank','character varying(120)',true),
('normal_refund_receivers','account_number','character varying(40)',true),
('normal_refund_receivers','account_name','character varying(150)',true),
('normal_refund_receivers','confirmed_by','integer',true),
('normal_refund_receivers','confirmed_at','timestamp without time zone',true),
('normal_refund_receivers','verified_by','integer',false),
('normal_refund_receivers','verified_at','timestamp without time zone',false),
('normal_refund_receivers','verification_source','character varying(2000)',false),
('normal_refund_attempts','id','integer',true),
('normal_refund_attempts','refund_id','integer',true),
('normal_refund_attempts','receiver_id','integer',true),
('normal_refund_attempts','claim_key','character varying(36)',true),
('normal_refund_attempts','claimed_by','integer',true),
('normal_refund_attempts','claimed_at','timestamp without time zone',true),
('normal_refund_attempts','status','character varying(30)',true),
('normal_refund_attempts','amount','numeric(12,2)',true),
('normal_refund_attempts','currency','character varying(3)',true),
('normal_refund_attempts','bank_reference','character varying(120)',false),
('normal_refund_attempts','performed_at','timestamp without time zone',false),
('normal_refund_attempts','verified_by','integer',false),
('normal_refund_attempts','verified_at','timestamp without time zone',false),
('normal_refund_attempts','bank_source','character varying(2000)',false),
('normal_refund_attempts','outcome_note','character varying(2000)',false),
('normal_refund_documents','id','integer',true),
('normal_refund_documents','attempt_id','integer',true),
('normal_refund_documents','uploaded_by','integer',true),
('normal_refund_documents','uploaded_at','timestamp without time zone',true),
('normal_refund_documents','content_type','character varying(30)',true),
('normal_refund_documents','sha256','character varying(64)',true),
('normal_refund_documents','content','bytea',true),
('normal_refund_verifications','id','integer',true),
('normal_refund_verifications','attempt_id','integer',true),
('normal_refund_verifications','verified_by','integer',true),
('normal_refund_verifications','verified_at','timestamp without time zone',true),
('normal_refund_verifications','outcome','character varying(20)',true),
('normal_refund_verifications','bank_source','character varying(2000)',true),
('normal_refund_verifications','note','character varying(2000)',true)
 ) expected(table_name,column_name,type_name,required) LOOP
  IF NOT EXISTS(SELECT 1 FROM pg_attribute a WHERE a.attrelid=to_regclass('public.'||item.table_name) AND a.attname=item.column_name AND NOT a.attisdropped AND format_type(a.atttypid,a.atttypmod)=item.type_name AND a.attnotnull=item.required) THEN
   RAISE EXCEPTION 'Incompatible Normal table %.%',item.table_name,item.column_name;
  END IF;
 END LOOP;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='invoices'::regclass AND contype='f' AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='invoices'::regclass AND attname='entitlement_class_id')]::smallint[]) THEN RAISE EXCEPTION 'Incompatible entitlement FK'; END IF;
 IF EXISTS(SELECT 1 FROM (VALUES('uq_normal_refund_active_attempt'),('uq_normal_pending_proposal')) expected(name) WHERE NOT EXISTS(SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid WHERE c.relname=expected.name AND i.indisunique AND i.indpred IS NOT NULL AND i.indisvalid)) THEN RAISE EXCEPTION 'Incompatible Normal uniqueness guards'; END IF;
END $model$;


-- SOURCE: database/migrations/24_advanced_session_proposals_postgres.sql SHA256 b78305c142a6bcc20768152c0ad1f316a46294fac4199d8502f92851fa77d8e1
-- SCMS Advanced: additive upgrade preserving all existing invoices, credits and sessions.
-- Owner runs this separately before deploying Member schedule acceptance.
-- No reset, seed, UPDATE, DELETE, DROP or change to the demo rebuild bundle.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

DO $$
BEGIN
    IF to_regclass('public.member_packages') IS NULL OR to_regclass('public.users') IS NULL
       OR to_regclass('public.sessions') IS NULL OR to_regclass('public.rooms') IS NULL
       OR to_regclass('public.classes') IS NULL OR to_regclass('public.subjects') IS NULL
       OR to_regclass('public.center_incidents') IS NULL THEN
        RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_DEPENDENCY: Existing Advanced schema is required';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                  AND table_name='advanced_registrations' AND column_name='accepted_at' AND data_type='timestamp without time zone') THEN
        RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_DEPENDENCY: Coach acceptance timestamp is required';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.private_session_proposals (
    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    subscription_id INTEGER NOT NULL REFERENCES public.member_packages(subscription_id),
    proposed_by INTEGER NOT NULL REFERENCES public.users(user_id),
    coach_id INTEGER NOT NULL REFERENCES public.users(user_id),
    room_id INTEGER NOT NULL REFERENCES public.rooms(room_id),
    class_id INTEGER REFERENCES public.classes(class_id),
    subject_id INTEGER REFERENCES public.subjects(subject_id),
    session_date DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    respond_by TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITHOUT TIME ZONE,
    support_reason VARCHAR(2000),
    incident_id INTEGER REFERENCES public.center_incidents(id),
    extra_validity_reason VARCHAR(2000),
    session_id INTEGER UNIQUE REFERENCES public.sessions(session_id),
    CONSTRAINT uq_private_proposal_request UNIQUE(subscription_id,request_key),
    CONSTRAINT ck_private_proposal_status CHECK(status IN ('Pending','Accepted','Rejected')),
    CONSTRAINT ck_private_proposal_time CHECK(end_time>start_time),
    CONSTRAINT ck_private_proposal_target CHECK(class_id IS NOT NULL OR subject_id IS NOT NULL),
    CONSTRAINT ck_private_proposal_session CHECK((status='Accepted')=(session_id IS NOT NULL)),
    CONSTRAINT ck_private_proposal_response CHECK((status='Pending')=(responded_at IS NULL))
);

-- IF NOT EXISTS alone is insufficient: reject a different pre-existing table instead of silently upgrading it.
DO $$
DECLARE item RECORD;
BEGIN
    FOR item IN SELECT * FROM (VALUES
        ('id','integer',true),('subscription_id','integer',true),('proposed_by','integer',true),
        ('coach_id','integer',true),('room_id','integer',true),('class_id','integer',false),('subject_id','integer',false),
        ('session_date','date',true),('start_time','time without time zone',true),('end_time','time without time zone',true),
        ('request_key','character varying',true),('request_hash','character varying',true),('status','character varying',true),
        ('created_at','timestamp without time zone',true),('respond_by','timestamp without time zone',true),
        ('responded_at','timestamp without time zone',false),('support_reason','character varying',false),
        ('incident_id','integer',false),('extra_validity_reason','character varying',false),('session_id','integer',false)
    ) AS required(column_name,data_type,required_not_null)
    LOOP
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns c WHERE c.table_schema='public'
            AND c.table_name='private_session_proposals' AND c.column_name=item.column_name AND c.data_type=item.data_type
            AND (c.is_nullable='NO')=item.required_not_null) THEN
            RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE: column % differs',item.column_name;
        END IF;
    END LOOP;
    FOR item IN SELECT * FROM (VALUES
        ('request_key',80),('request_hash',64),('status',20),('support_reason',2000),('extra_validity_reason',2000)
    ) AS required(column_name,max_length)
    LOOP
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns c WHERE c.table_schema='public'
            AND c.table_name='private_session_proposals' AND c.column_name=item.column_name
            AND c.character_maximum_length=item.max_length) THEN
            RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE: length % differs',item.column_name;
        END IF;
    END LOOP;
    FOR item IN SELECT * FROM (VALUES
        ('subscription_id','member_packages','subscription_id'),('proposed_by','users','user_id'),
        ('coach_id','users','user_id'),('room_id','rooms','room_id'),('class_id','classes','class_id'),
        ('subject_id','subjects','subject_id'),('incident_id','center_incidents','id'),('session_id','sessions','session_id')
    ) AS required(column_name,target_table,target_column)
    LOOP
        IF NOT EXISTS(SELECT 1 FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
            JOIN pg_attribute b ON b.attrelid=c.confrelid AND b.attnum=c.confkey[1]
            WHERE c.conrelid='public.private_session_proposals'::regclass AND c.contype='f' AND c.convalidated
              AND cardinality(c.conkey)=1 AND cardinality(c.confkey)=1
              AND a.attname=item.column_name AND c.confrelid=to_regclass('public.'||item.target_table)
              AND b.attname=item.target_column AND c.confdeltype='a' AND c.confupdtype='a') THEN
            RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE: FK % differs',item.column_name;
        END IF;
    END LOOP;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='private_session_proposals'
                  AND column_name='id' AND (is_identity='YES' OR column_default LIKE 'nextval(%'))
       OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.private_session_proposals'::regclass AND contype='p'
                  AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='public.private_session_proposals'::regclass AND attname='id')]::smallint[])
       OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.private_session_proposals'::regclass AND contype='u'
                  AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='public.private_session_proposals'::regclass AND attname='session_id')]::smallint[]) THEN
        RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE: identity, primary key or unique session differs';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.private_session_proposals'::regclass
                  AND conname='uq_private_proposal_request' AND contype='u'
                  AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='public.private_session_proposals'::regclass AND attname='subscription_id'),
                                   (SELECT attnum FROM pg_attribute WHERE attrelid='public.private_session_proposals'::regclass AND attname='request_key')]::smallint[])
       OR (SELECT count(*) FROM pg_constraint WHERE conrelid='public.private_session_proposals'::regclass
           AND contype='c' AND convalidated AND conname IN ('ck_private_proposal_status','ck_private_proposal_time',
                'ck_private_proposal_target','ck_private_proposal_session','ck_private_proposal_response'))<>5 THEN
        RAISE EXCEPTION 'SCMS_ADVANCED_PROPOSAL_INCOMPATIBLE: required constraints missing';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_private_proposal_member_subscription ON public.private_session_proposals(subscription_id,id DESC);
CREATE INDEX IF NOT EXISTS idx_private_proposal_coach ON public.private_session_proposals(coach_id,id DESC);

-- New SCMS table only: do not inherit direct browser Data API access through default grants.
REVOKE ALL ON public.private_session_proposals FROM PUBLIC;
DO $$
DECLARE browser_role TEXT; identity_sequence TEXT;
BEGIN
    identity_sequence:=pg_get_serial_sequence('public.private_session_proposals','id');
    IF identity_sequence IS NOT NULL THEN EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM PUBLIC',identity_sequence); END IF;
    FOREACH browser_role IN ARRAY ARRAY['anon','authenticated'] LOOP
        IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=browser_role) THEN
            EXECUTE format('REVOKE ALL ON TABLE public.private_session_proposals FROM %I',browser_role);
            IF identity_sequence IS NOT NULL THEN EXECUTE format('REVOKE ALL ON SEQUENCE %s FROM %I',identity_sequence,browser_role); END IF;
        END IF;
    END LOOP;
END $$;

SELECT 'PASS' AS result,'20261114_advanced_session_proposals_v1' AS migration_key,
       'private_session_proposals' AS added_table,
       'Existing invoices, credits and sessions unchanged; demo bundle unchanged' AS scope;


-- SOURCE: database/migrations/25_advanced_registration_classes_postgres.sql SHA256 87e8b2f88c4628bc1734c12c2e5967a00fd040b30b0e616a6cd19078f16bfe0e
-- Owner-run upgrade: one private class per accepted Advanced registration.
-- Preserves invoice amounts, credit balances, session IDs/status/Coach/Member, audit and consent history.
-- Reassigns only sessions.class_id through the exact existing subscription->registration relation.
-- Legacy shared templates are retained for a separate guarded cleanup; the new API does not list them.

SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='90s';
SELECT pg_advisory_xact_lock(391,20261114);
LOCK TABLE public.classes,public.advanced_registrations,public.sessions,public.private_session_proposals IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE public.advanced_registrations ADD COLUMN IF NOT EXISTS class_id INTEGER;
DO $preflight$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='advanced_registrations'
   AND column_name='class_id' AND data_type='integer' AND is_nullable='YES') THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: advanced_registrations.class_id';
 END IF;
 IF EXISTS(SELECT 1 FROM advanced_registrations r JOIN users m ON m.user_id=r.member_id JOIN users c ON c.user_id=r.coach_id
           WHERE r.status IN ('Accepted','Invoiced') AND (r.accepted_at IS NULL OR r.accepted_by IS NULL OR m.center_id IS DISTINCT FROM c.center_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: missing acceptance or mismatched center';
 END IF;
 IF EXISTS(SELECT 1 FROM sessions s JOIN classes c ON c.class_id=s.class_id
           LEFT JOIN advanced_registrations r ON r.subscription_id=s.subscription_id
           WHERE (c.class_type='Advanced' OR s.subscription_id IS NOT NULL) AND
            (r.id IS NULL OR r.status NOT IN ('Accepted','Invoiced') OR c.class_type<>'Advanced'
             OR s.booked_member_id IS DISTINCT FROM r.member_id OR c.subject_id IS DISTINCT FROM r.subject_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: session lacks an exact registration/member/subject link';
 END IF;
 IF EXISTS(SELECT 1 FROM training_plans p JOIN classes c ON c.class_id=p.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM class_feedbacks f JOIN classes c ON c.class_id=f.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM class_enrollments e JOIN classes c ON c.class_id=e.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM invoices i JOIN classes c ON c.class_id=i.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: class-scoped legacy documents require explicit mapping; no history moved';
 END IF;
 IF EXISTS(SELECT 1 FROM private_session_proposals p JOIN advanced_registrations r ON r.subscription_id=p.subscription_id
           WHERE p.status='Pending' AND p.class_id IS NOT NULL AND p.class_id IS DISTINCT FROM r.class_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_PENDING_PROPOSAL: reject/reissue explicit legacy class offers through the authorized API first';
 END IF;
END $preflight$;

DO $backfill$
DECLARE owned_registration RECORD; new_class INTEGER;
BEGIN
 FOR owned_registration IN SELECT a.*,u.center_id,s.subject_name,coalesce(p.purchase_amount,s.advanced_price) AS price
  FROM advanced_registrations a JOIN users u ON u.user_id=a.member_id JOIN subjects s USING(subject_id)
  LEFT JOIN member_packages p ON p.subscription_id=a.subscription_id
  WHERE a.status IN ('Accepted','Invoiced') AND a.class_id IS NULL ORDER BY a.id
 LOOP
  INSERT INTO classes(class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft)
  VALUES(left(owned_registration.subject_name,65)||' - Advanced #'||owned_registration.id,owned_registration.subject_id,owned_registration.coach_id,1,owned_registration.price,'Closed','Advanced',12,owned_registration.center_id,false)
  RETURNING class_id INTO new_class;
  UPDATE advanced_registrations SET class_id=new_class WHERE id=owned_registration.id;
 END LOOP;
 IF EXISTS(SELECT 1 FROM advanced_registrations r JOIN classes c ON c.class_id=r.class_id JOIN users m ON m.user_id=r.member_id
           WHERE c.class_type<>'Advanced' OR c.max_capacity<>1 OR c.subject_id IS DISTINCT FROM r.subject_id
             OR c.coach_id IS DISTINCT FROM r.coach_id OR c.center_id IS DISTINCT FROM m.center_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: existing private class belongs to another course/Coach/center';
 END IF;
END $backfill$;

UPDATE sessions s SET class_id=r.class_id FROM advanced_registrations r
WHERE r.subscription_id=s.subscription_id AND r.class_id IS NOT NULL AND s.class_id IS DISTINCT FROM r.class_id;

DO $constraints$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='fk_advanced_registration_class') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT fk_advanced_registration_class FOREIGN KEY(class_id) REFERENCES classes(class_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='uq_advanced_registration_class') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT uq_advanced_registration_class UNIQUE(class_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='ck_advanced_registration_class_state') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT ck_advanced_registration_class_state CHECK((status IN ('Accepted','Invoiced'))=(class_id IS NOT NULL));
 END IF;
 IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass
           AND conname IN ('fk_advanced_registration_class','uq_advanced_registration_class','ck_advanced_registration_class_state') AND NOT convalidated)
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='fk_advanced_registration_class'
      AND contype='f' AND confrelid='classes'::regclass
      AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='advanced_registrations'::regclass AND attname='class_id')]::smallint[]
      AND confkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='classes'::regclass AND attname='class_id')]::smallint[])
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='uq_advanced_registration_class' AND contype='u'
      AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='advanced_registrations'::regclass AND attname='class_id')]::smallint[]) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: unexpected FK/unique/validation';
 END IF;
END $constraints$;
CREATE TEMP TABLE _scms_expected_advanced_class_state(
 status VARCHAR(20),class_id INTEGER,
 CHECK((status IN ('Accepted','Invoiced'))=(class_id IS NOT NULL))
) ON COMMIT DROP;
DO $check_definition$
BEGIN
 IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass
      AND conname='ck_advanced_registration_class_state') IS DISTINCT FROM
    (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='_scms_expected_advanced_class_state'::regclass AND contype='c') THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: unexpected state CHECK';
 END IF;
END $check_definition$;

CREATE OR REPLACE FUNCTION public.fn_scms_advanced_registration_class() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND OLD.class_id IS NOT NULL AND
  (NEW.class_id IS DISTINCT FROM OLD.class_id OR NEW.member_id IS DISTINCT FROM OLD.member_id
   OR NEW.subject_id IS DISTINCT FROM OLD.subject_id OR NEW.coach_id IS DISTINCT FROM OLD.coach_id
   OR (OLD.subscription_id IS NOT NULL AND NEW.subscription_id IS DISTINCT FROM OLD.subscription_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_IMMUTABLE: approved registration identity cannot be replaced';
 END IF;
 IF NEW.class_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM classes c JOIN users m ON m.user_id=NEW.member_id
   WHERE c.class_id=NEW.class_id AND c.class_type='Advanced' AND c.max_capacity=1
     AND c.subject_id=NEW.subject_id AND c.coach_id=NEW.coach_id AND c.center_id=m.center_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_OWNER: exact private class/subject/Coach/center required';
 END IF;
 IF NEW.class_id IS NOT NULL AND (NEW.accepted_at IS NULL OR NEW.accepted_by IS NULL) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_OWNER: recorded decision actor and time required';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_advanced_registration_class ON public.advanced_registrations;
CREATE TRIGGER trg_advanced_registration_class BEFORE INSERT OR UPDATE ON public.advanced_registrations
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_registration_class();
CREATE OR REPLACE FUNCTION public.fn_scms_advanced_class_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=OLD.class_id) AND
   (NEW.subject_id IS DISTINCT FROM OLD.subject_id OR NEW.coach_id IS DISTINCT FROM OLD.coach_id
    OR NEW.center_id IS DISTINCT FROM OLD.center_id OR NEW.class_type IS DISTINCT FROM OLD.class_type
    OR NEW.max_capacity IS DISTINCT FROM 1) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_IMMUTABLE: use the registered course and per-session substitute workflow';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_advanced_class_identity ON public.classes;
CREATE TRIGGER trg_advanced_class_identity BEFORE UPDATE ON public.classes
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_identity();
CREATE OR REPLACE FUNCTION public.fn_scms_private_session_class() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF (NEW.subscription_id IS NOT NULL OR EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=NEW.class_id))
  AND NOT EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.subscription_id=NEW.subscription_id
                 AND r.class_id=NEW.class_id AND r.member_id=NEW.booked_member_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_SESSION_OWNER: exact subscription, Member and private class required';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_private_session_class ON public.sessions;
CREATE TRIGGER trg_private_session_class BEFORE INSERT OR UPDATE ON public.sessions
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_private_session_class();
CREATE OR REPLACE FUNCTION public.fn_scms_advanced_class_has_owner() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE private_id INTEGER;
BEGIN
 private_id:=CASE WHEN TG_OP='DELETE' THEN OLD.class_id ELSE NEW.class_id END;
 IF EXISTS(SELECT 1 FROM classes c WHERE c.class_id=private_id AND c.class_type='Advanced')
    AND NOT EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=private_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_UNOWNED: private class must belong to an accepted registration at commit';
 END IF;
 RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS trg_private_class_has_owner ON public.classes;
CREATE CONSTRAINT TRIGGER trg_private_class_has_owner AFTER INSERT ON public.classes
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_has_owner();
DROP TRIGGER IF EXISTS trg_deleted_registration_keeps_class_owner ON public.advanced_registrations;
CREATE CONSTRAINT TRIGGER trg_deleted_registration_keeps_class_owner AFTER DELETE ON public.advanced_registrations
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_has_owner();

SELECT 'PASS' AS result,'20261114_advanced_registration_classes_v1' AS migration_key,
       'Existing sessions re-linked by their subscription; money, credit, consent and session history retained' AS scope;


-- SOURCE: database/migrations/16_retire_legacy_memberships_postgres.sql SHA256 0db82a46720bfd23b9f91d4a2ab08333a4f791e8b9daa1499aad5b11aa748604
CREATE OR REPLACE FUNCTION public.fn_scms_retired_membership_catalog() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: shared-membership catalog is retired'; END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_retired_membership_catalog
  BEFORE INSERT OR UPDATE ON public.membership_packages FOR EACH ROW EXECUTE FUNCTION public.fn_scms_retired_membership_catalog();
CREATE OR REPLACE FUNCTION public.fn_scms_current_advanced_only() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN
  IF NEW.package_id IS NOT NULL OR NEW.subject_id IS NULL OR NEW.primary_coach_id IS NULL THEN
    RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: entitlement requires subject and Primary Coach, without legacy package';
  END IF; RETURN NEW;
END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_current_advanced_only
  BEFORE INSERT OR UPDATE ON public.member_packages FOR EACH ROW EXECUTE FUNCTION public.fn_scms_current_advanced_only();
CREATE OR REPLACE FUNCTION public.fn_scms_no_legacy_invoice() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog,public AS $fn$
BEGIN
  IF NEW.package_id IS NOT NULL THEN RAISE EXCEPTION 'LEGACY_MEMBERSHIP_RETIRED: invoice cannot target a legacy catalog package'; END IF;
  RETURN NEW;
END $fn$;
CREATE OR REPLACE TRIGGER trg_scms_no_legacy_invoice
  BEFORE INSERT OR UPDATE ON public.invoices FOR EACH ROW EXECUTE FUNCTION public.fn_scms_no_legacy_invoice();


SET LOCAL search_path=public,extensions,pg_catalog;
-- Remove the obsolete package-catalog trigger; the current Advanced-only guard replaces it.
DROP TRIGGER IF EXISTS trg_member_packages_validate ON public.member_packages;
DROP FUNCTION IF EXISTS public.fn_member_packages_validate() RESTRICT;
CREATE UNIQUE INDEX uq_scms_users_email_ci ON public.users(lower(email));
ALTER TABLE public.classes ADD CONSTRAINT ck_demo_class_total_sessions CHECK(total_sessions=12);
ALTER TABLE public.classes ADD CONSTRAINT ck_demo_class_positive_price CHECK(tuition_fee>0);
ALTER TABLE public.invoices ADD CONSTRAINT ck_demo_invoice_positive_amount CHECK(amount>0);
ALTER TABLE public.sessions ADD CONSTRAINT ck_demo_standard_session_slot CHECK((start_time,end_time) IN
 ((TIME '06:00',TIME '07:30'),(TIME '07:45',TIME '09:15'),(TIME '09:30',TIME '11:00'),
  (TIME '13:00',TIME '14:30'),(TIME '14:45',TIME '16:15'),(TIME '16:30',TIME '18:00'),(TIME '18:15',TIME '19:45')));


-- SOURCE: database/bootstrap/scms_demo_reference.sql SHA256 ad77361119c4ab38bedc70db468119b0412678eb88e0def6e79141f45c98c739
-- Public learning/demo reference seed, carried from Owner-approved account list.
-- No payment, entitlement, attendance, training result, booking or historical date is fabricated.
CREATE TEMP TABLE _scms_accounts(ordinal INT PRIMARY KEY,role_name TEXT,name TEXT,email TEXT,phone TEXT,address TEXT,gender TEXT,birth DATE) ON COMMIT DROP;
INSERT INTO _scms_accounts VALUES
(1, 'Admin', 'Admin Account', 'admin@gmail.com', '0905000101', 'Văn phòng Trung tâm, Q1, TP.HCM', 'Male', '1985-08-12'),
(2, 'Member', 'Nguyễn Minh An', 'an@gmail.com', '0901000101', 'Quận 1, TP.HCM', 'Male', '2003-05-12'),
(3, 'Member', 'Trần Bảo Linh', 'linh@gmail.com', '0901000102', 'Quận 3, TP.HCM', 'Female', '2002-08-21'),
(4, 'Member', 'Lê Gia Huy', 'huy@gmail.com', '0901000103', 'Quận 7, TP.HCM', 'Male', '2004-02-15'),
(5, 'Member', 'Phạm Ngọc Mai', 'mai@gmail.com', '0901000104', 'Bình Thạnh, TP.HCM', 'Female', '2001-11-09'),
(6, 'Member', 'Võ Đức Khang', 'khang@gmail.com', '0901000105', 'TP. Thủ Đức, TP.HCM', 'Male', '2003-07-24'),
(7, 'Member', 'Đặng Thảo Vy', 'vy@gmail.com', '0901000106', 'Tân Bình, TP.HCM', 'Female', '2004-04-18'),
(8, 'Member', 'Bùi Quốc Bảo', 'bao@gmail.com', '0901000107', 'Quận 10, TP.HCM', 'Male', '2002-12-03'),
(9, 'Member', 'Hồ Khánh Nhi', 'nhi@gmail.com', '0901000108', 'Phú Nhuận, TP.HCM', 'Female', '2005-06-27'),
(10, 'Member', 'Đỗ Tuấn Kiệt', 'kiet@gmail.com', '0901000109', 'Quận 11, TP.HCM', 'Male', '2001-09-14'),
(11, 'Member', 'Phan Thu Hà', 'ha@gmail.com', '0901000110', 'Gò Vấp, TP.HCM', 'Female', '2003-03-30'),
(12, 'Coach', 'Nguyễn Hoàng Quân', 'coach.tennis1@gmail.com', '0902000101', 'Quận 1, TP.HCM', 'Male', '1990-03-15'),
(13, 'Coach', 'Huỳnh Diễm My', 'coach.tennis2@gmail.com', '0902000102', 'Quận 3, TP.HCM', 'Female', '1992-07-22'),
(14, 'Coach', 'Lê Minh Duy', 'coach.swimming1@gmail.com', '0902000103', 'Quận 7, TP.HCM', 'Male', '1989-11-08'),
(15, 'Coach', 'Lê Phương Anh', 'coach.swimming2@gmail.com', '0902000104', 'Bình Thạnh, TP.HCM', 'Female', '1993-05-17'),
(16, 'Coach', 'Võ Thanh Long', 'coach.pickleball1@gmail.com', '0902000105', 'TP. Thủ Đức, TP.HCM', 'Male', '1994-09-26'),
(17, 'Coach', 'Phạm Thị Lan', 'coach.pickleball2@gmail.com', '0902000106', 'Tân Bình, TP.HCM', 'Female', '1995-02-12'),
(18, 'Coach', 'Lê Nguyễn Thiên Phúc', 'coach.archery1@gmail.com', '0902000107', 'Quận 10, TP.HCM', 'Male', '1991-12-04'),
(19, 'Coach', 'Hồ Thảo My', 'coach.archery2@gmail.com', '0902000108', 'Phú Nhuận, TP.HCM', 'Female', '1996-06-19'),
(20, 'Coach', 'Trần Anh Dũng', 'coach.basketball1@gmail.com', '0902000109', 'Quận 11, TP.HCM', 'Male', '1988-01-30'),
(21, 'Coach', 'Phan Kim Ngân', 'coach.basketball2@gmail.com', '0902000110', 'Gò Vấp, TP.HCM', 'Female', '1992-08-14'),
(22, 'Coach', 'Lê Trần Phi', 'coach.football1@gmail.com', '0902000111', 'Quận 12, TP.HCM', 'Male', '1987-04-25'),
(23, 'Coach', 'Trần Ngọc Yến', 'coach.football2@gmail.com', '0902000112', 'Quận 6, TP.HCM', 'Female', '1993-10-09'),
(24, 'Coach', 'Nguyễn Văn Hoàng', 'coach.volleyball1@gmail.com', '0902000113', 'Quận 5, TP.HCM', 'Male', '1989-02-21'),
(25, 'Coach', 'Phạm Thanh Thư', 'coach.volleyball2@gmail.com', '0902000114', 'Quận 8, TP.HCM', 'Female', '1995-07-07'),
(26, 'Coach', 'Võ Minh Đức', 'coach.badminton1@gmail.com', '0902000115', 'Quận 4, TP.HCM', 'Male', '1990-09-18'),
(27, 'Coach', 'Trần Thị Hương', 'coach.badminton2@gmail.com', '0902000116', 'Tân Phú, TP.HCM', 'Female', '1996-01-11'),
(28, 'Coach', 'Lê Minh Khoa', 'coach.tabletennis1@gmail.com', '0902000117', 'Bình Tân, TP.HCM', 'Male', '1988-05-23'),
(29, 'Coach', 'Hồ Bích Oanh', 'coach.tabletennis2@gmail.com', '0902000118', 'Quận 3, TP.HCM', 'Female', '1994-12-16'),
(30, 'Receptionist', 'Nguyễn Thị Ngọc Anh', 'receptionist1@gmail.com', '0903000101', 'Bình Thạnh, TP.HCM', 'Female', '1998-06-14'),
(31, 'Receptionist', 'Trần Minh Khôi', 'receptionist2@gmail.com', '0903000102', 'TP. Thủ Đức, TP.HCM', 'Male', '1997-09-22'),
(32, 'CenterManager', 'Nguyễn Thành Đạt', 'manager1@gmail.com', '0904000101', 'Quận 7, TP.HCM', 'Male', '1988-04-18'),
(33, 'CenterManager', 'Trần Ngọc Diệp', 'manager2@gmail.com', '0904000102', 'Quận 3, TP.HCM', 'Female', '1990-09-25');

CREATE TEMP TABLE _scms_subjects(
    ordinal INT PRIMARY KEY, name TEXT, slug TEXT, normal_price NUMERIC, advanced_price NUMERIC,
    outdoor BOOLEAN, room_ordinal INT, coach_code TEXT
) ON COMMIT DROP;
INSERT INTO _scms_subjects VALUES
(1, 'Tennis', 'quan-vot-tennis', 2500000, 8000000, TRUE, 1, 'tennis'),
(2, 'Bơi lội', 'boi-loi', 1500000, 4500000, TRUE, 2, 'swimming'),
(3, 'Pickleball', 'pickleball', 1800000, 5000000, FALSE, 3, 'pickleball'),
(4, 'Bắn cung', 'ban-cung', 2000000, 6000000, TRUE, 4, 'archery'),
(5, 'Bóng rổ', 'bong-ro', 1200000, 3500000, FALSE, 5, 'basketball'),
(6, 'Bóng đá', 'bong-da', 1000000, 3500000, TRUE, 6, 'football'),
(7, 'Bóng chuyền', 'bong-chuyen', 1000000, 3500000, FALSE, 13, 'volleyball'),
(8, 'Cầu lông', 'cau-long', 900000, 3000000, FALSE, 7, 'badminton'),
(9, 'Bóng bàn', 'bong-ban', 800000, 2500000, FALSE, 8, 'tabletennis');
INSERT INTO users(user_id,role_id,full_name,email,phone,password_hash,address,gender,date_of_birth,status,center_id,is_first_login)
SELECT a.ordinal,r.role_id,a.name,a.email,a.phone,crypt('SWP391',gen_salt('bf',10)),a.address,a.gender,a.birth,'Active',
 CASE WHEN a.role_name='Admin' THEN NULL ELSE 1 END,FALSE
FROM _scms_accounts a JOIN roles r ON r.role_name=a.role_name;
INSERT INTO members(user_id,join_date) SELECT u.user_id,(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date FROM users u JOIN roles r USING(role_id) WHERE r.role_name='Member';
INSERT INTO coaches(user_id) SELECT u.user_id FROM users u JOIN roles r USING(role_id) WHERE r.role_name='Coach';
INSERT INTO receptionists(user_id) SELECT u.user_id FROM users u JOIN roles r USING(role_id) WHERE r.role_name='Receptionist';
INSERT INTO center_managers(user_id) SELECT u.user_id FROM users u JOIN roles r USING(role_id) WHERE r.role_name='CenterManager';
INSERT INTO specializations(name,subject_id,status)
SELECT x.name,s.subject_id,'Active' FROM _scms_subjects x JOIN subjects s USING(slug) ORDER BY x.ordinal;
INSERT INTO coach_specializations(coach_id,specialization_id)
SELECT u.user_id,sp.specialization_id FROM _scms_subjects x JOIN subjects s USING(slug)
 JOIN specializations sp ON sp.subject_id=s.subject_id
 JOIN users u ON u.email IN ('coach.'||x.coach_code||'1@gmail.com','coach.'||x.coach_code||'2@gmail.com');
UPDATE coaches c SET specialization=x.name,bio='Huấn luyện viên demo môn '||x.name||'.'
 FROM users u,_scms_subjects x WHERE c.user_id=u.user_id AND u.email IN ('coach.'||x.coach_code||'1@gmail.com','coach.'||x.coach_code||'2@gmail.com');
-- No subject-level Advanced classes: Coach acceptance creates one private class per registration.
-- Published demo availability only; actual heat/holiday/conflict/credit checks remain in BE.
INSERT INTO coach_work_schedules(coach_id,accepting_new) SELECT user_id,TRUE FROM coaches;
INSERT INTO coach_work_slots(coach_id,slot_key)
SELECT c.user_id,d.day||':'||slot FROM coaches c
 CROSS JOIN (VALUES('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY'),('SUNDAY')) d(day)
 CROSS JOIN generate_series(1,7) slot;


-- SOURCE: database/seed_holidays_catalog_2026.sql SHA256 008b3ebe8ff73fb6298a78c650e536ae73c53a7c78090e53443b32ea0b4e1f0d
-- Annual catalog compiled into inactive literal fixture rows below; standalone DML is not run here.

-- SOURCE: database/bootstrap/scms_demo_scenarios.sql SHA256 9b3f0eb6b4f9c205eb07aa2fbcb80363b559a0aedf6278049270660c43278276
-- Approved synthetic demo fixture. Anchor 2026-11-14 Asia/Ho_Chi_Minh. No real bank/payment events.
DO $anchor$
DECLARE business_date DATE := (clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
BEGIN
 IF business_date<>DATE '2026-11-14' THEN
  RAISE EXCEPTION 'SCMS_DEMO_STALE_ANCHOR: bundle anchor %, current VN date %; regenerate and verify the whole bundle', DATE '2026-11-14', business_date;
 END IF;
END $anchor$;

ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;
ALTER TABLE sessions DISABLE TRIGGER trg_sessions_validate;
SELECT setval('invoice_code_seq',greatest((SELECT value FROM _scms_code_floor),(SELECT last_value FROM invoice_code_seq)),true);
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(34,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo11','member11@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(34,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(35,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo12','member12@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(35,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(36,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo13','member13@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(36,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(37,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo14','member14@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(37,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(38,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo15','member15@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(38,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(39,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo16','member16@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(39,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(40,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo17','member17@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(40,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(41,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo18','member18@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(41,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(42,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo19','member19@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(42,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(43,(SELECT role_id FROM roles WHERE role_name='Member'),'Member Demo20','member20@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Active',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(43,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(44,(SELECT role_id FROM roles WHERE role_name='Member'),'Demo Inactive Member','member.inactive@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Inactive',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(44,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(45,(SELECT role_id FROM roles WHERE role_name='Member'),'Demo Locked Member','member.locked@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Locked',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(45,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(46,(SELECT role_id FROM roles WHERE role_name='Member'),'Demo Pending Member','member.pending@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Pending',1,FALSE);
INSERT INTO members(user_id,join_date) VALUES(46,'2026-11-14');
INSERT INTO users(user_id,role_id,full_name,email,password_hash,status,center_id,is_first_login) VALUES(47,(SELECT role_id FROM roles WHERE role_name='Coach'),'Demo Inactive Coach','coach.inactive@scms.demo.invalid',extensions.crypt('SWP391',extensions.gen_salt('bf',10)),'Inactive',1,FALSE);
INSERT INTO coaches(user_id) VALUES(47);
INSERT INTO coach_specializations(coach_id,specialization_id) SELECT 47,specialization_id FROM specializations WHERE subject_id=(SELECT subject_id FROM subjects WHERE slug='cau-long');
DELETE FROM coach_work_slots WHERE slot_key LIKE 'SUNDAY:%';
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2027-01-03','DEMO trung tâm đóng cửa','CENTER_EVENT',TRUE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-01-01','Tết Dương lịch','NATIONAL_FIXED',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-02-16','Tết Nguyên đán — 29 tháng Chạp','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-02-17','Tết Nguyên đán — Mùng 1','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-02-18','Tết Nguyên đán — Mùng 2','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-02-19','Tết Nguyên đán — Mùng 3','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-02-20','Tết Nguyên đán — Mùng 4','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-04-26','Giỗ Tổ Hùng Vương','LUNAR_FESTIVAL',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-04-30','Ngày Giải phóng miền Nam','NATIONAL_FIXED',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-05-01','Ngày Quốc tế Lao động','NATIONAL_FIXED',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-09-01','Quốc khánh — Ngày liền kề mẫu','NATIONAL_FIXED',FALSE);
INSERT INTO holidays(holiday_date,holiday_name,tag,is_active) VALUES('2026-09-02','Quốc khánh','NATIONAL_FIXED',FALSE);
INSERT INTO coach_days_off(coach_id,day_off) VALUES(17,'2027-01-13');
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(1,'Cầu lông Normal N01',(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,4,3,900000,'Open','Normal',12,1,FALSE,'2026-11-23','2026-12-18',NULL,NULL,NULL,0,0,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(2,'Tennis Normal N02',(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,4,3,2500000,'Open','Normal',12,1,FALSE,'2026-11-16','2026-12-11',NULL,NULL,NULL,0,0,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(3,'Bơi lội Normal N03',(SELECT subject_id FROM subjects WHERE slug='boi-loi'),14,10,7,1500000,'Open','Normal',12,1,FALSE,'2026-11-18','2026-12-12',NULL,NULL,NULL,0,0,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(4,'Bắn cung Normal N04',(SELECT subject_id FROM subjects WHERE slug='ban-cung'),18,5,4,2000000,'Open','Normal',12,1,FALSE,'2026-11-23','2026-12-18',NULL,NULL,NULL,1,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(5,'Pickleball Normal N05',(SELECT subject_id FROM subjects WHERE slug='pickleball'),16,4,3,1800000,'Open','Normal',12,1,FALSE,'2026-11-21','2026-12-17',NULL,NULL,NULL,0,1,TRUE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(6,'Bóng rổ Normal N06',(SELECT subject_id FROM subjects WHERE slug='bong-ro'),20,8,6,1200000,'Cancelled','Normal',12,1,FALSE,'2026-11-23','2026-12-18',NULL,NULL,NULL,0,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(7,'Bóng đá Normal N07',(SELECT subject_id FROM subjects WHERE slug='bong-da'),22,15,10,1000000,'Open','Normal',12,1,FALSE,'2026-11-24','2026-12-19',NULL,NULL,NULL,0,0,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(8,'Bóng chuyền Normal N08',(SELECT subject_id FROM subjects WHERE slug='bong-chuyen'),24,8,6,1000000,'Ongoing','Normal',12,1,FALSE,'2026-11-03','2026-11-28',32,'2026-11-01T13:00:00','DEMO confirmed opening',0,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(9,'Bóng bàn Normal N09',(SELECT subject_id FROM subjects WHERE slug='bong-ban'),28,2,2,800000,'Closed','Normal',12,1,FALSE,'2026-09-25','2026-10-21',32,'2026-09-23T14:45:00','DEMO confirmed opening',0,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(10,'Cầu lông Normal N10',(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,4,3,900000,'Ongoing','Normal',12,1,FALSE,'2026-10-27','2026-11-25',32,'2026-10-25T14:45:00','DEMO confirmed opening',0,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(11,'Pickleball Normal N11',(SELECT subject_id FROM subjects WHERE slug='pickleball'),17,4,3,1800000,'Open','Normal',12,1,FALSE,'2026-11-23','2026-12-18',NULL,NULL,NULL,0,0,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,min_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft,start_date,end_date,opening_confirmed_by,opening_confirmed_at,opening_reason,postponement_count,lifecycle_version,change_pending) VALUES(12,'Cầu lông Normal N12',(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,4,3,900000,'Cancelled','Normal',12,1,FALSE,'2026-10-27','2026-11-21',NULL,NULL,NULL,0,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(13,'Pickleball - Advanced #AR02',(SELECT subject_id FROM subjects WHERE slug='pickleball'),16,1,5000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(14,'Tennis - Advanced #AR04',(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,1,8000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(15,'Tennis - Advanced #AR05',(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,1,8000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(16,'Cầu lông - Advanced #AR06',(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,1,3000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(17,'Tennis - Advanced #AR07',(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,1,8000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(18,'Tennis - Advanced #AR08',(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,1,8000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(19,'Cầu lông - Advanced #AR09',(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,1,3000000,'Closed','Advanced',12,1,FALSE);
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft) VALUES(20,'Bơi lội - Advanced #AR10',(SELECT subject_id FROM subjects WHERE slug='boi-loi'),14,1,4500000,'Closed','Advanced',12,1,FALSE);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(1,3,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'2026-11-10','2026-12-25',12,45,'Active',8000000,'2026-11-10T08:00','2026-12-25T08:00',0,NULL);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(2,4,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'2026-11-09','2026-12-24',10,45,'Active',8000000,'2026-11-09T08:00','2026-12-24T08:00',0,NULL);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(3,5,(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,'2026-11-08','2026-12-23',0,45,'Active',3000000,'2026-11-08T08:00','2026-12-23T08:00',0,NULL);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(4,6,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'2026-10-05','2026-11-19',0,45,'Completed',8000000,'2026-10-05T08:00','2026-11-19T08:00',0,NULL);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(5,7,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'2026-09-05','2026-10-20',0,45,'Expired',8000000,'2026-09-05T08:00','2026-10-20T08:00',10,'2026-10-20T08:00');
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(6,8,(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,'2026-11-12','2026-12-27',12,45,'Active',3000000,'2026-11-12T08:00','2026-12-27T08:00',0,NULL);
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,start_date,end_date,remaining_credit,validity_days,status,purchase_amount,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at) VALUES(7,9,(SELECT subject_id FROM subjects WHERE slug='boi-loi'),14,'2026-11-11','2026-12-26',0,45,'Cancelled',4500000,NULL,NULL,0,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(1,2,1,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(2,5,2,NULL,2500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(3,6,2,NULL,2500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(4,7,2,NULL,2500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(5,8,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(6,9,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(7,10,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(8,11,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(9,34,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(10,35,3,NULL,1500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(11,2,4,NULL,2000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(12,3,4,NULL,2000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(13,4,4,NULL,2000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(14,2,5,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(15,3,5,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(16,4,5,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(17,5,6,NULL,1200000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(18,6,6,NULL,1200000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(19,7,6,NULL,1200000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(20,8,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(21,9,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(22,10,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(23,11,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(24,34,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(25,35,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(26,36,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(27,37,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(28,38,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(29,39,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(30,40,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(31,41,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(32,42,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(33,43,7,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(34,2,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(35,3,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(36,4,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(37,5,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(38,6,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(39,7,8,NULL,1000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-27T08:00','2026-10-27T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(40,2,9,NULL,800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-09-18T08:00','2026-09-18T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(41,3,9,NULL,800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-09-18T08:00','2026-09-18T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(42,2,10,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(43,3,10,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(44,4,10,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(45,34,11,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(46,35,11,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(47,36,11,NULL,1800000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-07T08:00','2026-11-07T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(48,8,12,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(49,9,12,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(50,10,12,NULL,900000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-20T08:00','2026-10-20T08:01',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(51,3,NULL,1,8000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-10T07:58','2026-11-10T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(52,4,NULL,2,8000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-09T07:58','2026-11-09T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(53,5,NULL,3,3000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-08T07:58','2026-11-08T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(54,6,NULL,4,8000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-10-05T07:58','2026-10-05T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(55,7,NULL,5,8000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-09-05T07:58','2026-09-05T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(56,8,NULL,6,3000000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'Cash','Paid','2026-11-12T07:58','2026-11-12T08:00',NULL,30,FALSE,FALSE,NULL);
INSERT INTO invoices(invoice_id,member_id,class_id,subscription_id,amount,invoice_code,payment_method,payment_status,created_at,payment_date,expires_at,receptionist_id,reconciliation_required,cash_confirmation_reversed,gateway_transaction_ref) VALUES(57,9,NULL,7,4500000,'INV-'||extract(year FROM DATE '2026-11-14')::int||'-'||nextval('invoice_code_seq'),'VNPay','Paid','2026-11-11T07:50','2026-11-11T08:00',NULL,NULL,TRUE,FALSE,'DEMO-LATE-RECEIPT');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(1,1,2,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(2,2,5,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(3,2,6,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(4,2,7,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(5,3,8,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(6,3,9,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(7,3,10,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(8,3,11,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(9,3,34,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(10,3,35,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(11,4,2,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(12,4,3,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(13,4,4,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(14,5,2,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(15,5,3,'Cancelled','2026-11-07T08:00','2026-11-11T00:00:00',3,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(16,5,4,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(17,6,5,'Cancelled','2026-11-07T08:00','2026-11-11T00:00:00',32,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(18,6,6,'Cancelled','2026-11-07T08:00','2026-11-11T00:00:00',32,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(19,6,7,'Cancelled','2026-11-07T08:00','2026-11-11T00:00:00',32,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(20,7,8,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(21,7,9,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(22,7,10,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(23,7,11,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(24,7,34,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(25,7,35,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(26,7,36,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(27,7,37,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(28,7,38,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(29,7,39,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(30,7,40,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(31,7,41,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(32,7,42,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(33,7,43,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(34,8,2,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(35,8,3,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(36,8,4,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(37,8,5,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(38,8,6,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(39,8,7,'Registered','2026-10-27T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(40,9,2,'Completed','2026-09-18T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(41,9,3,'Completed','2026-09-18T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(42,10,2,'Registered','2026-10-20T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(43,10,3,'Registered','2026-10-20T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(44,10,4,'Registered','2026-10-20T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(45,11,34,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(46,11,35,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(47,11,36,'Registered','2026-11-07T08:00',NULL,NULL,NULL);
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(48,12,8,'Cancelled','2026-10-20T08:00','2026-10-25T16:30:00',32,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(49,12,9,'Cancelled','2026-10-20T08:00','2026-10-25T16:30:00',32,'DEMO center refund obligation');
INSERT INTO class_enrollments(enrollment_id,class_id,member_id,status,enrolled_at,cancelled_at,cancelled_by,cancel_reason) VALUES(50,12,10,'Cancelled','2026-10-20T08:00','2026-10-25T16:30:00',32,'DEMO center refund obligation');
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(37,(SELECT subject_id FROM subjects WHERE slug='boi-loi'),14,'DEMO-AR01','Requested','2026-11-12T06:00',NULL,NULL,NULL,NULL,NULL);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(38,(SELECT subject_id FROM subjects WHERE slug='pickleball'),16,'DEMO-AR02','Accepted','2026-11-12T06:00',16,'2026-11-14T00:00',NULL,NULL,13);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(39,(SELECT subject_id FROM subjects WHERE slug='ban-cung'),18,'DEMO-AR03','Rejected','2026-11-12T06:00',18,'2026-11-13T07:00','DEMO Coach từ chối yêu cầu',NULL,NULL);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(3,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'DEMO-SUB01','Invoiced','2026-11-10T07:30',12,'2026-11-10T07:55',NULL,1,14);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(4,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'DEMO-SUB02','Invoiced','2026-11-09T07:30',12,'2026-11-09T07:55',NULL,2,15);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(5,(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,'DEMO-SUB03','Invoiced','2026-11-08T07:30',26,'2026-11-08T07:55',NULL,3,16);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(6,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'DEMO-SUB04','Invoiced','2026-10-05T07:30',12,'2026-10-05T07:55',NULL,4,17);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(7,(SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'),12,'DEMO-SUB05','Invoiced','2026-09-05T07:30',12,'2026-09-05T07:55',NULL,5,18);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(8,(SELECT subject_id FROM subjects WHERE slug='cau-long'),26,'DEMO-SUB06','Invoiced','2026-11-12T07:30',26,'2026-11-12T07:55',NULL,6,19);
INSERT INTO advanced_registrations(member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id,class_id) VALUES(9,(SELECT subject_id FROM subjects WHERE slug='boi-loi'),14,'DEMO-SUB07','Invoiced','2026-11-11T07:30',14,'2026-11-11T07:40',NULL,7,20);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(1,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-23','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(2,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-25','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(3,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-27','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(4,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-30','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(5,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-02','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(6,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-04','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(7,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-07','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(8,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-09','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(9,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-11','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(10,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-14','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(11,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-16','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(12,1,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-18','09:30','11:00',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(13,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-16','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(14,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-18','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(15,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-20','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(16,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-23','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(17,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-25','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(18,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-27','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(19,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-30','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(20,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-12-02','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(21,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-12-04','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(22,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-12-07','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(23,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-12-09','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(24,2,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-12-11','06:00','07:30',12,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(25,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-18','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(26,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-19','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(27,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-21','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(28,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-25','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(29,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-26','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(30,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-11-28','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(31,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-02','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(32,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-03','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(33,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-05','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(34,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-09','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(35,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-10','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(36,3,(SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'),'2026-12-12','06:00','07:30',14,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(37,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-11-23','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(38,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-11-25','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(39,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-11-27','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(40,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-11-30','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(41,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-02','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(42,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-04','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(43,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-07','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(44,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-09','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(45,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-11','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(46,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-14','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(47,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-16','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(48,4,(SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'),'2026-12-18','07:45','09:15',18,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(49,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-11-21','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(50,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-11-24','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(51,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-11-26','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(52,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-11-28','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(53,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-01','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(54,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-03','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(55,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-05','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(56,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-08','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(57,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-10','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(58,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-12','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(59,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-15','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(60,5,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-01'),'2026-12-17','09:30','11:00',16,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(61,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-11-23','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(62,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-11-25','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(63,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-11-27','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(64,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-11-30','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(65,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-02','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(66,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-04','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(67,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-07','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(68,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-09','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(69,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-11','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(70,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-14','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(71,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-16','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(72,6,(SELECT room_id FROM rooms WHERE room_code='C01-BR-01'),'2026-12-18','13:00','14:30',20,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(73,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-11-24','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(74,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-11-26','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(75,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-11-28','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(76,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-01','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(77,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-03','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(78,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-05','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(79,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-08','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(80,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-10','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(81,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-12','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(82,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-15','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(83,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-17','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(84,7,(SELECT room_id FROM rooms WHERE room_code='C01-FOOT-01'),'2026-12-19','07:45','09:15',22,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(85,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-03','13:00','14:30',24,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(86,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-05','13:00','14:30',24,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(87,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-07','13:00','14:30',24,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(88,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-10','13:00','14:30',24,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(89,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-12','13:00','14:30',24,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(90,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-14','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(91,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-17','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(92,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-19','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(93,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-21','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(94,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-24','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(95,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-26','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(96,8,(SELECT room_id FROM rooms WHERE room_code='C01-BC-01'),'2026-11-28','13:00','14:30',24,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(97,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-09-25','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(98,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-09-28','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(99,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-09-30','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(100,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-02','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(101,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-05','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(102,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-07','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(103,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-09','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(104,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-12','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(105,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-14','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(106,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-16','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(107,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-19','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(108,9,(SELECT room_id FROM rooms WHERE room_code='C01-TT-01'),'2026-10-21','14:45','16:15',28,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(109,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-27','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(110,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-29','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(111,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-31','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(112,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-03','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(113,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-05','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(114,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-07','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(115,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-10','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(116,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-12','14:45','16:15',26,NULL,NULL,'Completed',TRUE,FALSE,TRUE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(117,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-14','14:45','16:15',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(118,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-17','14:45','16:15',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(119,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-19','14:45','16:15',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(120,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-21','14:45','16:15',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(121,10,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-25','18:15','19:45',26,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,116,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(122,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-11-23','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(123,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-11-25','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(124,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-11-27','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(125,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-11-30','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(126,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-02','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(127,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-04','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(128,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-07','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(129,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-09','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(130,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-11','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(131,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-14','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(132,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-16','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(133,11,(SELECT room_id FROM rooms WHERE room_code='C01-PICK-02'),'2026-12-18','16:30','18:00',17,NULL,NULL,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(134,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-27','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(135,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-29','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(136,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-10-31','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(137,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-03','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(138,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-05','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(139,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-07','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(140,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-10','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(141,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-12','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(142,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-14','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(143,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-17','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(144,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-19','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(145,12,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-21','16:30','18:00',26,NULL,NULL,'Cancelled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(146,15,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-19','18:15','19:45',12,4,2,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(147,15,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-21','18:15','19:45',12,4,2,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(148,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-17','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(149,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-19','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(150,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-21','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(151,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-24','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(152,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-26','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(153,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-11-28','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(154,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-01','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(155,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-03','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(156,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-05','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(157,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-08','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(158,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-10','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(159,16,(SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'),'2026-12-12','18:15','19:45',26,5,3,'Scheduled',FALSE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(160,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-10','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(161,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-13','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(162,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-15','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(163,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-17','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(164,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-20','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(165,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-22','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(166,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-24','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(167,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-27','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(168,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-29','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(169,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-10-31','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(170,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-03','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(171,17,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-11-05','18:15','19:45',12,6,4,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(172,18,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-09-10','18:15','19:45',12,7,5,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO sessions(session_id,class_id,room_id,session_date,start_time,end_time,coach_id,booked_member_id,subscription_id,status,attendance_finalized,attendance_review_pending,center_invalidated,makeup_for_session_id,created_by) VALUES(173,18,(SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'),'2026-09-12','18:15','19:45',12,7,5,'Completed',TRUE,FALSE,FALSE,NULL,32);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(160,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(161,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(162,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(163,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(164,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(165,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(166,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(167,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(168,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(169,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(170,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(171,6,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(172,7,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(173,7,12,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,2,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,3,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,4,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,5,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,6,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(85,7,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,2,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,3,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,4,24,'Absent','DEMO cần ôn lại kỹ thuật');
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,5,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,6,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(86,7,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,2,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,3,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,4,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,5,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,6,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(87,7,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,2,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,3,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,4,24,'Absent','DEMO cần ôn lại kỹ thuật');
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,5,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,6,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(88,7,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,2,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,3,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,4,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,5,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,6,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(89,7,24,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(97,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(97,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(98,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(98,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(99,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(99,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(100,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(100,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(101,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(101,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(102,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(102,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(103,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(103,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(104,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(104,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(105,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(105,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(106,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(106,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(107,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(107,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(108,2,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(108,3,28,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(109,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(109,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(109,4,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(110,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(110,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(110,4,26,'Absent','DEMO cần ôn lại kỹ thuật');
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(111,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(111,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(111,4,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(112,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(112,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(112,4,26,'Absent','DEMO cần ôn lại kỹ thuật');
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(113,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(113,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(113,4,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(114,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(114,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(114,4,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(115,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(115,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(115,4,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(116,2,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(116,3,26,'Present',NULL);
INSERT INTO attendances(session_id,member_id,recorded_by,state,note) VALUES(116,4,26,'Present',NULL);
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(160,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-10 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(161,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-13 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(162,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-15 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(163,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-17 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(164,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-20 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(165,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-22 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(166,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-24 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(167,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-27 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(168,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-29 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(169,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-10-31 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(170,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-11-03 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(171,6,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-11-05 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(172,7,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-09-10 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(173,7,12,'DEMO bài tập cá nhân đã thực hiện','Present','2026-09-12 19:45');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,2,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,3,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,4,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,5,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,6,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(85,7,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(86,2,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(86,3,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(86,5,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(86,6,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(86,7,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,2,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,3,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,4,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,5,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,6,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(87,7,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(88,2,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(88,3,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(88,5,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(88,6,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(88,7,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,2,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,3,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,4,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,5,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,6,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(89,7,24,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-12 14:30');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(97,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-25 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(97,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-25 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(98,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-28 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(98,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-28 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(99,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-30 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(99,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-09-30 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(100,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-02 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(100,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-02 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(101,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-05 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(101,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-05 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(102,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-07 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(102,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-07 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(103,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-09 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(103,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-09 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(104,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-12 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(104,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-12 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(105,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-14 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(105,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-14 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(106,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-16 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(106,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-16 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(107,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-19 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(107,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-19 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(108,2,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-21 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(108,3,28,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-21 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(109,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-27 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(109,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-27 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(109,4,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-27 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(110,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-29 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(110,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-29 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(111,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-31 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(111,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-31 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(111,4,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-10-31 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(112,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(112,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-03 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(113,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(113,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(113,4,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-05 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(114,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(114,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(114,4,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-07 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(115,2,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(115,3,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 16:15');
INSERT INTO training_results(session_id,member_id,coach_id,content,attendance_status,recorded_at) VALUES(115,4,26,'DEMO thực hiện bài tập và được nhận xét tại buổi học','Present','2026-11-10 16:15');
INSERT INTO training_plans(coach_id,class_id,member_id,title,content,created_at) VALUES(12,NULL,4,'DEMO giáo án cá nhân Advanced','Thực hành kỹ thuật riêng cho Member M03','2026-11-04 08:00');
INSERT INTO training_plans(coach_id,class_id,member_id,title,content,created_at) VALUES(24,8,NULL,'DEMO giáo án lớp N08','Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng','2026-11-04 08:00');
INSERT INTO training_plans(coach_id,class_id,member_id,title,content,created_at) VALUES(28,9,NULL,'DEMO giáo án lớp N09','Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng','2026-11-04 08:00');
INSERT INTO training_plans(coach_id,class_id,member_id,title,content,created_at) VALUES(26,10,NULL,'DEMO giáo án lớp N10','Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng','2026-11-04 08:00');
INSERT INTO evaluations(member_id,coach_id,evaluation_date,progress_score,comment) VALUES(2,28,'2026-10-30',8,'DEMO tiến bộ sau khóa Bóng bàn');
INSERT INTO evaluations(member_id,coach_id,evaluation_date,progress_score,comment) VALUES(3,28,'2026-10-30',8,'DEMO tiến bộ sau khóa Bóng bàn');
INSERT INTO class_feedbacks(member_id,class_id,coach_id,class_rating,coach_rating,comment,created_at) VALUES(2,9,28,4,4,'DEMO phản hồi khóa đã hoàn tất','2026-11-13 08:00');
INSERT INTO class_feedbacks(member_id,class_id,coach_id,class_rating,coach_rating,comment,created_at) VALUES(3,9,28,4,4,'DEMO phản hồi khóa đã hoàn tất','2026-11-13 08:00');
INSERT INTO center_incidents(id,session_id,request_key,started_at,ended_at,reason,evidence,status,created_by,created_at,resolved_by,resolved_at,recovered_credits,session_compensated) VALUES(1,116,'INC-N10','2026-11-12 14:45','2026-11-12 14:45','DEMO buổi không tổ chức do trung tâm','DEMO xác minh buổi không tổ chức','Resolved',32,'2026-11-12 14:45',32,'2026-11-12 14:45',0,TRUE);
INSERT INTO incident_affected_members(incident_id,member_id) VALUES(1,2);
INSERT INTO incident_affected_members(incident_id,member_id) VALUES(1,3);
INSERT INTO incident_affected_members(incident_id,member_id) VALUES(1,4);
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(1,56,'CashCorrection','Approved',3000000,'DEMO COUNTER_MISTAKE',32,'2026-11-12T09:00:00',32,'2026-11-12T09:01:00',NULL,NULL,NULL,NULL);
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(2,15,'Refund','AwaitingReceiver',1800000,'DEMO MEMBER_REQUEST',32,'2026-11-11T00:00:00',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(2,5,'MemberChoice',0,NULL,NULL,NULL);
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=15;
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(3,17,'Refund','AwaitingReceiver',1200000,'DEMO CENTER_CANCELLATION',32,'2026-11-11T00:00:00',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(3,6,'CenterCancellation',0,NULL,NULL,NULL);
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=17;
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(4,18,'Refund','AwaitingApproval',1200000,'DEMO CENTER_CANCELLATION',32,'2026-11-11T00:00:00',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(4,6,'CenterCancellation',0,NULL,NULL,NULL);
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=18;
INSERT INTO normal_refund_receivers(id,refund_id,bank,account_number,account_name,confirmed_by,confirmed_at,verified_by,verified_at,verification_source) VALUES(4,4,'DEMO BANK','DEMOACCOUNTM05','DEMO M05',6,'2026-11-14T00:00',NULL,NULL,NULL);
UPDATE normal_refunds SET receiver_id=4,approved_receiver_id=NULL WHERE id=4;
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(5,19,'Refund','Approved',1200000,'DEMO CENTER_CANCELLATION',32,'2026-11-11T00:00:00',32,'2026-11-14T00:00',NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(5,6,'CenterCancellation',0,'2026-11-21T23:59:59.999','2026-11-21T23:59:59.999',NULL);
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=19;
INSERT INTO normal_refund_receivers(id,refund_id,bank,account_number,account_name,confirmed_by,confirmed_at,verified_by,verified_at,verification_source) VALUES(5,5,'DEMO BANK','DEMOACCOUNTM06','DEMO M06',7,'2026-11-14T00:00',32,'2026-11-14T00:00','DEMO Member and receiving account ownership verified');
UPDATE normal_refunds SET receiver_id=5,approved_receiver_id=5 WHERE id=5;
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(6,48,'Refund','AwaitingVerification',900000,'DEMO CENTER_CANCELLATION',32,'2026-10-25T16:30:00',32,'2026-11-11T00:00',NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(6,12,'CenterCancellation',0,'2026-11-18T23:59:59.999','2026-11-18T23:59:59.999',NULL);
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=48;
INSERT INTO normal_refund_receivers(id,refund_id,bank,account_number,account_name,confirmed_by,confirmed_at,verified_by,verified_at,verification_source) VALUES(6,6,'DEMO BANK','DEMOACCOUNTM07','DEMO M07',8,'2026-11-11T00:00',32,'2026-11-11T00:00','DEMO Member and receiving account ownership verified');
UPDATE normal_refunds SET receiver_id=6,approved_receiver_id=6 WHERE id=6;
INSERT INTO normal_refund_attempts(id,refund_id,receiver_id,claim_key,claimed_by,claimed_at,status,amount,currency,bank_reference,performed_at,verified_by,verified_at,bank_source,outcome_note) VALUES(6,6,6,'852316bf-6922-5326-a044-9f8a12e42a13',30,'2026-11-12 08:00','Submitted',900000,'VND','DEMO-TRANSFER-RF05','2026-11-13T08:00',NULL,NULL,NULL,NULL);
INSERT INTO normal_refund_documents(attempt_id,uploaded_by,uploaded_at,content_type,sha256,content) VALUES(6,30,'2026-11-13T08:00','application/pdf','9a029049d4856fbefd380fc68a54405316b806bce373fd486c04a82aefb2ba46',decode('255044462d312e330a25938c8b9e205265706f72744c61622047656e6572617465642050444620646f63756d656e7420286f70656e736f75726365290a312030206f626a0a3c3c0a2f4631203220302052202f46322033203020520a3e3e0a656e646f626a0a322030206f626a0a3c3c0a2f42617365466f6e74202f48656c766574696361202f456e636f64696e67202f57696e416e7369456e636f64696e67202f4e616d65202f4631202f53756274797065202f5479706531202f54797065202f466f6e740a3e3e0a656e646f626a0a332030206f626a0a3c3c0a2f42617365466f6e74202f48656c7665746963612d426f6c64202f456e636f64696e67202f57696e416e7369456e636f64696e67202f4e616d65202f4632202f53756274797065202f5479706531202f54797065202f466f6e740a3e3e0a656e646f626a0a342030206f626a0a3c3c0a2f436f6e74656e7473203820302052202f4d65646961426f78205b203020302035393520343230205d202f506172656e74203720302052202f5265736f7572636573203c3c0a2f466f6e74203120302052202f50726f63536574205b202f504446202f54657874202f496d61676542202f496d61676543202f496d61676549205d0a3e3e202f526f746174652030202f5472616e73203c3c0a0a3e3e200a20202f54797065202f506167650a3e3e0a656e646f626a0a352030206f626a0a3c3c0a2f506167654d6f6465202f5573654e6f6e65202f5061676573203720302052202f54797065202f436174616c6f670a3e3e0a656e646f626a0a362030206f626a0a3c3c0a2f417574686f722028616e6f6e796d6f757329202f4372656174696f6e446174652028443a32303030303130313030303030302b30302730302729202f43726561746f722028616e6f6e796d6f757329202f4b6579776f726473202829202f4d6f64446174652028443a32303030303130313030303030302b30302730302729202f50726f647563657220285265706f72744c616220504446204c696272617279202d205c286f70656e736f757263655c2929200a20202f5375626a6563742028756e73706563696669656429202f5469746c65202844454d4f20726566756e642065766964656e6365205246303529202f54726170706564202f46616c73650a3e3e0a656e646f626a0a372030206f626a0a3c3c0a2f436f756e742031202f4b696473205b203420302052205d202f54797065202f50616765730a3e3e0a656e646f626a0a382030206f626a0a3c3c0a2f46696c746572205b202f415343494938354465636f6465202f466c6174654465636f6465205d202f4c656e677468203337320a3e3e0a73747265616d0a4761733244597469316a274c64355b696638505240604d337341682e5e5a32283b2d4c472a42522854515a46573c3969413933212e35363445752a736b4d3275595f382a6b623f595c4c7269614b6d4436635e306e352b3d712d4f6965562d256855464c5c49632b42473c576b602c29316d75383c294140524e32715e35473152214863545a652b2a56463d2350656a547335572e745f574f3c6335214a6827532a55455f39577134343e483f395c514b6d3d2b2367636b394852252e2c45373f6e705c5a2a5a376f4a632a5929286c605e5c5058406e2e21402f313f4226213746666a45545c2d2b3c3f343f56665d4267637024274e43383a6d5254247226555b586b7223612235566b3b265d586a704173235b25754c532d4e3a695b532e4f2d41474b5e2457683e4e443159464b22292d5d635a6f3c463048293e2d543b5d276755573b7063745c4d4d73572645246a3e682846594d592a2a4b70266f373d2833442b5626613035654b283b2c60583e7e3e656e6473747265616d0a656e646f626a0a787265660a3020390a303030303030303030302036353533352066200a30303030303030303631203030303030206e200a30303030303030313032203030303030206e200a30303030303030323039203030303030206e200a30303030303030333231203030303030206e200a30303030303030353134203030303030206e200a30303030303030353832203030303030206e200a30303030303030383630203030303030206e200a30303030303030393139203030303030206e200a747261696c65720a3c3c0a2f4944200a5b3c61356261376339363235616661363135666437663731353931393438666239613e3c61356261376339363235616661363135666437663731353931393438666239613e5d0a25205265706f72744c61622067656e6572617465642050444620646f63756d656e74202d2d2064696765737420286f70656e736f75726365290a0a2f496e666f2036203020520a2f526f6f742035203020520a2f53697a6520390a3e3e0a7374617274787265660a313338310a2525454f460a','hex'));
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(7,49,'Refund','Completed',900000,'DEMO CENTER_CANCELLATION',32,'2026-10-25T16:30:00',32,'2026-11-11T00:00',33,'2026-11-14 00:00','2026-11-13 08:00','DEMO private bank evidence retained');
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(7,12,'CenterCancellation',0,'2026-11-18T23:59:59.999','2026-11-18T23:59:59.999',NULL);
UPDATE invoices SET resolution_status='Completed' WHERE invoice_id=49;
INSERT INTO normal_refund_receivers(id,refund_id,bank,account_number,account_name,confirmed_by,confirmed_at,verified_by,verified_at,verification_source) VALUES(7,7,'DEMO BANK','DEMOACCOUNTM08','DEMO M08',9,'2026-11-11T00:00',32,'2026-11-11T00:00','DEMO Member and receiving account ownership verified');
UPDATE normal_refunds SET receiver_id=7,approved_receiver_id=7 WHERE id=7;
INSERT INTO normal_refund_attempts(id,refund_id,receiver_id,claim_key,claimed_by,claimed_at,status,amount,currency,bank_reference,performed_at,verified_by,verified_at,bank_source,outcome_note) VALUES(7,7,7,'0a07aaa4-9db2-58bb-b9bf-06115213545d',30,'2026-11-12 08:00','Verified',900000,'VND','DEMO-TRANSFER-RF06','2026-11-13T08:00',33,'2026-11-14 00:00','DEMO independent center bank statement','DEMO independently matched success');
INSERT INTO normal_refund_documents(attempt_id,uploaded_by,uploaded_at,content_type,sha256,content) VALUES(7,30,'2026-11-13T08:00','application/pdf','39c32676ad95e0a855a2e84152ef8482dd4518b0259e60ca3eeb7655ffdcb383',decode('255044462d312e330a25938c8b9e205265706f72744c61622047656e6572617465642050444620646f63756d656e7420286f70656e736f75726365290a312030206f626a0a3c3c0a2f4631203220302052202f46322033203020520a3e3e0a656e646f626a0a322030206f626a0a3c3c0a2f42617365466f6e74202f48656c766574696361202f456e636f64696e67202f57696e416e7369456e636f64696e67202f4e616d65202f4631202f53756274797065202f5479706531202f54797065202f466f6e740a3e3e0a656e646f626a0a332030206f626a0a3c3c0a2f42617365466f6e74202f48656c7665746963612d426f6c64202f456e636f64696e67202f57696e416e7369456e636f64696e67202f4e616d65202f4632202f53756274797065202f5479706531202f54797065202f466f6e740a3e3e0a656e646f626a0a342030206f626a0a3c3c0a2f436f6e74656e7473203820302052202f4d65646961426f78205b203020302035393520343230205d202f506172656e74203720302052202f5265736f7572636573203c3c0a2f466f6e74203120302052202f50726f63536574205b202f504446202f54657874202f496d61676542202f496d61676543202f496d61676549205d0a3e3e202f526f746174652030202f5472616e73203c3c0a0a3e3e200a20202f54797065202f506167650a3e3e0a656e646f626a0a352030206f626a0a3c3c0a2f506167654d6f6465202f5573654e6f6e65202f5061676573203720302052202f54797065202f436174616c6f670a3e3e0a656e646f626a0a362030206f626a0a3c3c0a2f417574686f722028616e6f6e796d6f757329202f4372656174696f6e446174652028443a32303030303130313030303030302b30302730302729202f43726561746f722028616e6f6e796d6f757329202f4b6579776f726473202829202f4d6f64446174652028443a32303030303130313030303030302b30302730302729202f50726f647563657220285265706f72744c616220504446204c696272617279202d205c286f70656e736f757263655c2929200a20202f5375626a6563742028756e73706563696669656429202f5469746c65202844454d4f20726566756e642065766964656e6365205246303629202f54726170706564202f46616c73650a3e3e0a656e646f626a0a372030206f626a0a3c3c0a2f436f756e742031202f4b696473205b203420302052205d202f54797065202f50616765730a3e3e0a656e646f626a0a382030206f626a0a3c3c0a2f46696c746572205b202f415343494938354465636f6465202f466c6174654465636f6465205d202f4c656e677468203337310a3e3e0a73747265616d0a4761733244597469316a274c64355b696638505240604e3e5341682e5e5a32283b2d4c472a42522854515a46573c3969413933212e35363445752a736b4d3275595f382a6b623f595c4c7269614b6d4436635e306e352b3d712d4f6965562d25685a68213e5f6c505434212e6c40264f41656d383c294140524e32715e35473152214863545a652b2a56463d2350656a547335572e745f574e4a4949216b512c32715c644852396c47475b6f544f432f43684735263e36575070493b3b37694d5e676a462653324d6c23352e3c3151375b575c5058406e2e21402f313f3c735a316c3c3a5b31453d235a5e475e2e4e4364746c70272e42225254676c59746d2c505c446170636a69483a487375443e483d5f7126402a747333224e4e5c2f3540254f41474c393457683e4e443159464b2229326b4454712c67285f4e2d5a63586830303f6757545d4f3e3745316c4e333053344524505a32674e5c224d24712a3c522a5b5c5654412848355d22653c36217e3e656e6473747265616d0a656e646f626a0a787265660a3020390a303030303030303030302036353533352066200a30303030303030303631203030303030206e200a30303030303030313032203030303030206e200a30303030303030323039203030303030206e200a30303030303030333231203030303030206e200a30303030303030353134203030303030206e200a30303030303030353832203030303030206e200a30303030303030383630203030303030206e200a30303030303030393139203030303030206e200a747261696c65720a3c3c0a2f4944200a5b3c36363963616465663961663030333930343339653133366230303037653466393e3c36363963616465663961663030333930343339653133366230303037653466393e5d0a25205265706f72744c61622067656e6572617465642050444620646f63756d656e74202d2d2064696765737420286f70656e736f75726365290a0a2f496e666f2036203020520a2f526f6f742035203020520a2f53697a6520390a3e3e0a7374617274787265660a313338300a2525454f460a','hex'));
INSERT INTO payment_resolutions(id,invoice_id,resolution_type,status,amount,reason,created_by,created_at,approved_by,approved_at,completed_by,completed_at,performed_at,evidence) VALUES(8,50,'Refund','NeedsReview',900000,'DEMO CENTER_CANCELLATION',32,'2026-10-25T16:30:00',32,'2026-11-11T00:00',NULL,NULL,NULL,NULL);
INSERT INTO normal_refunds(id,class_id,origin,version,due_at,first_due_at,issue_note) VALUES(8,12,'CenterCancellation',0,'2026-11-18T23:59:59.999','2026-11-18T23:59:59.999','DEMO unknown bank outcome; do not retry transfer');
UPDATE invoices SET resolution_status='RefundPending' WHERE invoice_id=50;
INSERT INTO normal_refund_receivers(id,refund_id,bank,account_number,account_name,confirmed_by,confirmed_at,verified_by,verified_at,verification_source) VALUES(8,8,'DEMO BANK','DEMOACCOUNTM09','DEMO M09',10,'2026-11-11T00:00',32,'2026-11-11T00:00','DEMO Member and receiving account ownership verified');
UPDATE normal_refunds SET receiver_id=8,approved_receiver_id=8 WHERE id=8;
INSERT INTO normal_refund_attempts(id,refund_id,receiver_id,claim_key,claimed_by,claimed_at,status,amount,currency,bank_reference,performed_at,verified_by,verified_at,bank_source,outcome_note) VALUES(8,8,8,'f2f24d0c-a592-569f-8353-bf265375f65f',30,'2026-11-12 08:00','Claimed',900000,'VND','DEMO-TRANSFER-RF07','2026-11-13T08:00',NULL,NULL,NULL,NULL);
INSERT INTO normal_refund_verifications(attempt_id,verified_by,verified_at,outcome,bank_source,note) VALUES(7,33,'2026-11-14 00:00','Success','DEMO independent center bank statement','DEMO receipt and receiver matched independently');
INSERT INTO normal_proposals(id,class_id,target_class_id,kind,status,shift_days,version,fingerprint,schedule_json,reason,created_by,created_at) VALUES(1,5,11,'Transfer','Pending',NULL,1,'RECOMPUTE_AFTER_FIXTURE','[]','DEMO center cannot provide original class',32,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(1,14,2,'Waiting',1,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(1,15,3,'Refund',1,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(1,16,4,'Waiting',1,'2026-11-11 00:00');
UPDATE normal_proposals p SET schedule_json=(SELECT json_agg(json_build_object('sessionId',s.session_id,'date',s.session_date,'start',to_char(s.start_time,'HH24:MI'),'end',to_char(s.end_time,'HH24:MI'),'coachId',s.coach_id,'roomId',s.room_id) ORDER BY s.session_date,s.start_time)::text FROM sessions s WHERE s.class_id=coalesce(p.target_class_id,p.class_id) AND s.status<>'Cancelled' AND NOT s.center_invalidated) WHERE p.id=1;
UPDATE normal_proposals p SET fingerprint=encode(extensions.digest((SELECT string_agg(piece,'' ORDER BY class_id) FROM (SELECT c.class_id,c.class_id||':'||c.status||':'||c.tuition_fee||':'||c.max_capacity||':'||c.min_capacity||':'||c.coach_id||coalesce((SELECT string_agg('|'||s.session_id||':'||s.session_date||':'||to_char(s.start_time,'HH24:MI')||':'||to_char(s.end_time,'HH24:MI')||':'||s.room_id||':'||s.coach_id||':'||s.status,'' ORDER BY s.session_date,s.start_time) FROM sessions s WHERE s.class_id=c.class_id AND s.status<>'Cancelled' AND NOT s.center_invalidated),'') AS piece FROM classes c WHERE c.class_id=p.class_id OR c.class_id=p.target_class_id) x),'sha256'),'hex') WHERE p.id=1;
INSERT INTO normal_proposals(id,class_id,target_class_id,kind,status,shift_days,version,fingerprint,schedule_json,reason,created_by,created_at) VALUES(2,4,NULL,'Postpone','Applied',7,1,'RECOMPUTE_AFTER_FIXTURE','[]','DEMO insufficient enrollment; one postponement applied',32,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(2,11,2,'Accept',1,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(2,12,3,'Accept',1,'2026-11-11 00:00');
INSERT INTO normal_responses(proposal_id,invoice_id,member_id,choice,version,responded_at) VALUES(2,13,4,'Accept',1,'2026-11-11 00:00');
UPDATE normal_proposals p SET schedule_json=(SELECT json_agg(json_build_object('sessionId',s.session_id,'date',s.session_date,'start',to_char(s.start_time,'HH24:MI'),'end',to_char(s.end_time,'HH24:MI'),'coachId',s.coach_id,'roomId',s.room_id) ORDER BY s.session_date,s.start_time)::text FROM sessions s WHERE s.class_id=coalesce(p.target_class_id,p.class_id) AND s.status<>'Cancelled' AND NOT s.center_invalidated) WHERE p.id=2;
UPDATE normal_proposals p SET fingerprint=encode(extensions.digest((SELECT string_agg(piece,'' ORDER BY class_id) FROM (SELECT c.class_id,c.class_id||':'||c.status||':'||c.tuition_fee||':'||c.max_capacity||':'||c.min_capacity||':'||c.coach_id||coalesce((SELECT string_agg('|'||s.session_id||':'||s.session_date||':'||to_char(s.start_time,'HH24:MI')||':'||to_char(s.end_time,'HH24:MI')||':'||s.room_id||':'||s.coach_id||':'||s.status,'' ORDER BY s.session_date,s.start_time) FROM sessions s WHERE s.class_id=c.class_id AND s.status<>'Cancelled' AND NOT s.center_invalidated),'') AS piece FROM classes c WHERE c.class_id=p.class_id OR c.class_id=p.target_class_id) x),'sha256'),'hex') WHERE p.id=2;
INSERT INTO notifications(notification_id,user_id,title,content,type,notification_type,related_type,related_id,dedupe_key,is_read,created_at) VALUES(1,2,'DEMO workflow update','DEMO đề nghị chuyển N05→N11; chờ lựa chọn Member, không tự đồng ý','System','PAYMENT_RECONCILED','CLASS',5,'DEMO:NT01',FALSE,'2026-11-14 00:00');
INSERT INTO notification_recipients(notification_id,user_id,is_read,created_at) VALUES(1,2,FALSE,'2026-11-14 00:00');
INSERT INTO notifications(notification_id,user_id,title,content,type,notification_type,related_type,related_id,dedupe_key,is_read,created_at) VALUES(2,5,'DEMO workflow update','DEMO trung tâm hủy lớp; đã có nghĩa vụ hoàn, chờ thông tin nhận','System','PAYMENT_RECONCILED','CLASS',6,'DEMO:NT02',FALSE,'2026-11-14 00:00');
INSERT INTO notification_recipients(notification_id,user_id,is_read,created_at) VALUES(2,5,FALSE,'2026-11-14 00:00');
INSERT INTO notifications(notification_id,user_id,title,content,type,notification_type,related_type,related_id,dedupe_key,is_read,created_at) VALUES(3,9,'DEMO workflow update','DEMO đã xác minh hoàn900000đ; tham chiếu DEMO-TRANSFER-RF06','System','PAYMENT_RECONCILED','CLASS',12,'DEMO:NT03',TRUE,'2026-11-14 00:00');
INSERT INTO notification_recipients(notification_id,user_id,is_read,created_at) VALUES(3,9,TRUE,'2026-11-14 00:00');
INSERT INTO system_logs(user_id,action,target_entity,target_id,detail,timestamp,audit_category,center_id,scope_source) VALUES(32,'CANCEL_NORMAL_PRESTART','CLASS',6,'DEMO fixture: synthetic history, not observed provider operations','2026-11-14 00:00','BUSINESS',1,'TARGET_AT_EVENT');
INSERT INTO system_logs(user_id,action,target_entity,target_id,detail,timestamp,audit_category,center_id,scope_source) VALUES(32,'APPROVE_NORMAL_REFUND','CLASS',12,'DEMO fixture: synthetic history, not observed provider operations','2026-11-14 00:00','BUSINESS',1,'TARGET_AT_EVENT');
INSERT INTO system_logs(user_id,action,target_entity,target_id,detail,timestamp,audit_category,center_id,scope_source) VALUES(30,'SUBMIT_REFUND_PROOF','CLASS',12,'DEMO fixture: synthetic history, not observed provider operations','2026-11-14 00:00','BUSINESS',1,'TARGET_AT_EVENT');
INSERT INTO system_logs(user_id,action,target_entity,target_id,detail,timestamp,audit_category,center_id,scope_source) VALUES(33,'VERIFY_NORMAL_REFUND','CLASS',12,'DEMO fixture: synthetic history, not observed provider operations','2026-11-14 00:00','BUSINESS',1,'TARGET_AT_EVENT');
INSERT INTO chat_conversations(id,member_user_id,assigned_staff_user_id,status,handler_type,subject,created_at,updated_at,last_message_at) VALUES(1,9,30,'OPEN','STAFF','DEMO refund inquiry','2026-11-14 00:00','2026-11-14 00:00','2026-11-14 00:00');
INSERT INTO chat_messages(id,conversation_id,sender_type,sender_user_id,content,content_type,client_message_id,created_at) VALUES(1,1,'MEMBER',9,'DEMO tôi muốn kiểm tra khoản hoàn RF06','TEXT','DEMO-MSG-1','2026-11-14 00:00');
INSERT INTO chat_messages(id,conversation_id,sender_type,sender_user_id,content,content_type,client_message_id,created_at) VALUES(2,1,'STAFF',30,'DEMO trung tâm đã tiếp nhận để kiểm tra lại giao dịch','TEXT','DEMO-MSG-2','2026-11-14 00:00');
INSERT INTO chat_read_state(conversation_id,user_id,last_read_message_id,updated_at) VALUES(1,9,1,'2026-11-14 00:00');
INSERT INTO chat_read_state(conversation_id,user_id,last_read_message_id,updated_at) VALUES(1,30,2,'2026-11-14 00:00');
SET CONSTRAINTS ALL IMMEDIATE;
ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;
ALTER TABLE sessions ENABLE TRIGGER trg_sessions_validate;

DO $catalog$
BEGIN
 IF EXISTS(SELECT 1 FROM (VALUES (DATE '2026-01-01'),(DATE '2026-02-16'),(DATE '2026-02-17'),(DATE '2026-02-18'),(DATE '2026-02-19'),(DATE '2026-02-20'),(DATE '2026-04-26'),(DATE '2026-04-30'),(DATE '2026-05-01'),(DATE '2026-09-01'),(DATE '2026-09-02')) AS expected(day)
   LEFT JOIN public.holidays h ON h.holiday_date=expected.day
   WHERE h.holiday_id IS NULL OR h.is_active IS DISTINCT FROM FALSE)
 OR (SELECT count(*) FROM public.holidays WHERE is_active)<>1
 OR NOT EXISTS(SELECT 1 FROM public.holidays WHERE holiday_date=DATE '2026-11-14'+50 AND is_active) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: holiday catalog or demo closure mismatch';
 END IF;
END $catalog$;

-- Revoke direct browser/Data API access to SCMS data; backend owner/explicit service grants remain.
DO $acl$
DECLARE n TEXT; grantee_name TEXT; seq RECORD;
BEGIN
 FOREACH n IN ARRAY ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4'] LOOP
  IF to_regclass('public.'||n) IS NOT NULL THEN
   EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM PUBLIC',n);
   FOREACH grantee_name IN ARRAY ARRAY['anon','authenticated'] LOOP
    IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=grantee_name) THEN
     EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE public.%I FROM %I',n,grantee_name);
    END IF;
   END LOOP;
  END IF;
 END LOOP;
 FOR seq IN SELECT sequencename FROM pg_sequences WHERE schemaname='public' AND
   (sequencename='invoice_code_seq' OR EXISTS(SELECT 1 FROM pg_class c JOIN pg_depend d ON d.objid=c.oid
     JOIN pg_class t ON t.oid=d.refobjid WHERE c.oid=to_regclass('public.'||sequencename) AND t.relname=ANY(ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4']) AND d.deptype IN ('a','i'))) LOOP
  EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE public.%I FROM PUBLIC',seq.sequencename);
  FOREACH grantee_name IN ARRAY ARRAY['anon','authenticated'] LOOP
   IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname=grantee_name) THEN EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE public.%I FROM %I',seq.sequencename,grantee_name); END IF;
  END LOOP;
 END LOOP;
END $acl$;
-- Reset row IDs only where owned by recreated SCMS tables. Payment-reference sequence is separate and monotonic.
DO $ids$
DECLARE r RECORD; seq TEXT; last_id BIGINT;
BEGIN
 FOR r IN SELECT table_name,column_name FROM information_schema.columns WHERE table_schema='public'
   AND table_name=ANY(ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4']) AND (column_default LIKE 'nextval(%' OR is_identity='YES') LOOP
  seq:=pg_get_serial_sequence('public.'||r.table_name,r.column_name);
  IF seq IS NOT NULL AND seq<>'public.invoice_code_seq' AND seq<>'invoice_code_seq' THEN
   EXECUTE format('SELECT max(%I) FROM public.%I',r.column_name,r.table_name) INTO last_id;
   PERFORM setval(seq::regclass,coalesce(last_id,1),last_id IS NOT NULL);
  END IF;
 END LOOP;
 PERFORM setval('public.invoice_code_seq',greatest((SELECT value FROM _scms_code_floor),(SELECT last_value FROM public.invoice_code_seq)),true);
END $ids$;
INSERT INTO public.scms_data_migration_audit(migration_key,captured_at,before_data,after_data)
VALUES('20261110_demo_rebuild_v3',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',
 (SELECT coalesce(jsonb_object_agg(table_name,row_count),'{}'::jsonb)::text FROM _scms_reset_before),
 jsonb_build_object('mode','approved-synthetic-demo','accounts',47,'subjects',9,'rooms',20,'advanced_templates',0,'advanced_private_classes',8,
  'normal_classes',12,'synthetic_fixture',true,'anchor_date','2026-11-14',
  'business_timezone','Asia/Ho_Chi_Minh','fixture_version','normal-phase1-v3',
  'invoice_code_floor',(SELECT value FROM _scms_code_floor))::text);
INSERT INTO public.scms_data_migration_audit VALUES('20261106_retire_legacy_memberships_v1',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',
 '{"mode":"fresh-demo"}','{"mode":"fresh-demo","model_retired":true}');
DO $postflight$
DECLARE n TEXT; c BIGINT;
BEGIN
 IF (SELECT count(*) FROM users)<>47 OR (SELECT count(*) FROM subjects)<>9 OR (SELECT count(*) FROM rooms)<>20
   OR (SELECT count(*) FROM classes)<>20 OR (SELECT count(*) FROM classes WHERE class_type='Normal')<>12
   OR (SELECT count(*) FROM normal_refunds)<>7 OR (SELECT count(*) FROM normal_refund_documents)<>2
   OR EXISTS(SELECT 1 FROM rooms WHERE activity_type IS NULL) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: approved fixture counts mismatch';
 END IF;
 IF EXISTS(SELECT 1 FROM classes c WHERE c.class_type='Normal' AND
   (c.min_capacity<(2*c.max_capacity+2)/3 OR c.min_capacity>c.max_capacity OR
    (SELECT count(*) FROM sessions s WHERE s.class_id=c.class_id AND NOT s.center_invalidated)<>12)) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: Normal minimum or twelve valid sessions mismatch';
 END IF;
 IF EXISTS(SELECT 1 FROM classes WHERE class_type='Advanced' AND class_name LIKE '%(1:1)%') THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: Advanced class name contains removed suffix';
 END IF;
 IF EXISTS(SELECT 1 FROM classes c LEFT JOIN advanced_registrations r ON r.class_id=c.class_id
           WHERE c.class_type='Advanced' AND (r.id IS NULL OR r.status NOT IN ('Accepted','Invoiced') OR c.coach_id IS NULL))
 OR EXISTS(SELECT 1 FROM sessions s JOIN advanced_registrations r ON r.subscription_id=s.subscription_id WHERE s.class_id<>r.class_id) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: unowned Advanced class or wrong session owner';
 END IF;
 IF EXISTS(SELECT 1 FROM invoices i WHERE i.amount<=0 OR i.payment_status<>'Paid')
 OR EXISTS(SELECT 1 FROM normal_refunds f JOIN payment_resolutions r ON r.id=f.id JOIN invoices i ON i.invoice_id=r.invoice_id WHERE r.amount<>i.amount OR i.payment_status<>'Paid')
 OR EXISTS(SELECT 1 FROM normal_refunds f JOIN payment_resolutions r ON r.id=f.id JOIN invoices i ON i.invoice_id=r.invoice_id JOIN class_enrollments e ON e.member_id=i.member_id AND e.class_id=f.class_id WHERE e.status<>'Cancelled') THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: receipt/refund/right mismatch';
 END IF;
 FOREACH n IN ARRAY ARRAY['email_otps','vnpay_payment_attempts','membership_packages','private_session_proposals'] LOOP
  EXECUTE format('SELECT count(*) FROM public.%I',n) INTO c;
  IF c<>0 THEN RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: dynamic/retired data in %',n; END IF;
 END LOOP;
 IF EXISTS(SELECT 1 FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace ns ON ns.oid=t.relnamespace
   WHERE ns.nspname='public' AND t.relname=ANY(ARRAY['private_session_proposals','normal_refund_verifications','normal_refund_documents','normal_refund_attempts','normal_refund_receivers','normal_refunds','normal_transfers','normal_responses','normal_proposals','scms_data_migration_audit','payment_resolutions','incident_affected_members','center_incidents','session_changes','session_absence_notices','advanced_registrations','coach_days_off','coach_work_slots','coach_work_schedules','subscription_pauses','normal_class_requests','system_logs','ai_consultations','chat_read_state','chat_messages','chat_conversations','notification_recipients','notifications','support_requests','vnpay_payment_attempts','invoices','holidays','class_enrollments','member_packages','membership_packages','class_feedbacks','evaluations','attendances','training_results','sessions','training_plans','classes','rooms','coach_specializations','specializations','subjects','members','coaches','receptionists','center_managers','email_otps','users','centers','role_permissions','permissions','roles','attendances_backup_flow4','class_enrollments_backup_flow4','role_permissions_backup_n06','training_results_backup_flow4']) AND NOT c.convalidated) THEN
  RAISE EXCEPTION 'SCMS_DEMO_POSTFLIGHT: unvalidated constraint';
 END IF;
END $postflight$;
COMMIT;
SELECT 'PASS' AS result,migration_key,after_data::jsonb AS summary FROM public.scms_data_migration_audit WHERE migration_key='20261110_demo_rebuild_v3';

