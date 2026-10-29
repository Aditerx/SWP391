-- DATABASE VARIANT: OWNER-AUTHORIZED FULL SCMS REBUILD, INCLUDING AN EXISTING DATABASE.
-- Each execution REPLACES all rows in the 46 listed SCMS tables and restarts owned integer IDs.
-- This intentionally recreates reference business history; it does NOT migrate real transactions.
-- All history below is SYNTHETIC reference data, NOT evidence of actual payment or attendance.
-- Supabase auth/storage/realtime tables and unlisted application tables are never dropped.
-- DROP uses RESTRICT: an external FK/view dependency aborts the transaction instead of cascading.
-- SCMS PostgreSQL / Supabase - Owner-approved catalog and System Account.pdf.
-- Copy and run this WHOLE file in one SQL Editor execution.
-- Timezone: Asia/Ho_Chi_Minh. Never run the two schema variants in succession.
-- Owner-approved learning/demo accounts use SWP391, stored as salted BCrypt.
-- Only these 33 PDF identities skip the first-login password change.
-- ROOT corrects the previous random-password seed once, then preserves later password edits.
-- Subtype PKs equal users.user_id; composite/UUID/text keys are not integer sequences.
BEGIN;
SET LOCAL search_path = public, extensions, pg_catalog;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';
SELECT pg_advisory_xact_lock(391, 20261029);
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
SET LOCAL scms.rebuild = 'true';
DROP TABLE IF EXISTS public.scms_data_migration_audit,
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
 public.roles;
DROP SEQUENCE IF EXISTS public.invoice_code_seq;
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
    CONSTRAINT fk_member_receptionist
        FOREIGN KEY (registered_by) REFERENCES receptionists(user_id)
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
CREATE UNIQUE INDEX IF NOT EXISTS ux_attendance_session_member
    ON attendances(session_id, member_id)
    WHERE session_id IS NOT NULL;

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
-- Consolidated FK validation; no historical subscription is inferred or backfilled.

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
    related_id      INT,
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



-- Consolidated structure: 06_schedule_safety_postgres.sql
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

-- Consolidated structure: 08_advanced_entitlements_postgres.sql
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

-- Consolidated structure: 10_session_incidents_postgres.sql

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

-- Consolidated structure: 11_holiday_history_postgres.sql

ALTER TABLE holidays ADD COLUMN IF NOT EXISTS actual_closed_until TIMESTAMP;
-- No guessed backfill. An active holiday's actual past interval is captured on deactivation.

-- Consolidated structure: 12_payment_resolutions_postgres.sql

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

CREATE TABLE IF NOT EXISTS scms_data_migration_audit (
    migration_key VARCHAR(100) PRIMARY KEY,
    captured_at TIMESTAMP NOT NULL,
    before_data TEXT NOT NULL,
    after_data TEXT NOT NULL
);

-- TABLE PURPOSES: retained runtime, compatibility, audit and future tables.
COMMENT ON TABLE public.roles IS 'Vai trò hệ thống: Admin, Member, Coach, Receptionist, CenterManager.';
COMMENT ON TABLE public.permissions IS 'Danh mục quyền thao tác nghiệp vụ.';
COMMENT ON TABLE public.role_permissions IS 'Ma trận quyền của từng vai trò; khóa ghép, không có ID riêng.';
COMMENT ON TABLE public.centers IS 'Trung tâm và hàng khóa dùng để tuần tự hóa các thao tác tranh chấp lịch.';
COMMENT ON TABLE public.users IS 'Danh tính đăng nhập và hồ sơ chung; mật khẩu chỉ lưu hash.';
COMMENT ON TABLE public.email_otps IS 'OTP đăng ký/đặt lại mật khẩu, hạn dùng và chống thử lại.';
COMMENT ON TABLE public.center_managers IS 'Hồ sơ subtype CenterManager; dùng chung user_id với users.';
COMMENT ON TABLE public.receptionists IS 'Hồ sơ subtype Receptionist; dùng chung user_id với users.';
COMMENT ON TABLE public.coaches IS 'Hồ sơ Coach, mô tả và chứng chỉ; chuyên môn chuẩn ở coach_specializations.';
COMMENT ON TABLE public.members IS 'Hồ sơ Member, mục tiêu/sức khỏe và trạng thái QR/booking block.';
COMMENT ON TABLE public.subjects IS '9 môn chuẩn, giá 12 buổi Normal/12 credit Advanced và phân loại Outdoor.';
COMMENT ON TABLE public.specializations IS 'Danh mục chuyên môn Coach liên kết với môn học.';
COMMENT ON TABLE public.coach_specializations IS 'Liên kết nhiều-nhiều Coach và chuyên môn; khóa ghép.';
COMMENT ON TABLE public.rooms IS '8 tài nguyên sân/phòng; Bóng rổ và Bóng chuyền dùng chung một room_id.';
COMMENT ON TABLE public.classes IS 'Khóa Normal 12 buổi cố định và template Advanced theo môn.';
COMMENT ON TABLE public.training_plans IS 'Kế hoạch đào tạo cho một lớp hoặc một Member.';
COMMENT ON TABLE public.sessions IS 'Buổi học, tài nguyên, người học 1:1 và liên kết quyền học/buổi bù.';
COMMENT ON TABLE public.training_results IS 'Kết quả chuyên môn và trạng thái attendance đồng bộ cho từng buổi/Member.';
COMMENT ON TABLE public.attendances IS 'Lịch sử điểm danh từng buổi, người ghi và lý do; không trừ credit lần hai.';
COMMENT ON TABLE public.evaluations IS 'Đánh giá tiến bộ Member của Coach theo ngày.';
COMMENT ON TABLE public.class_feedbacks IS 'Phản hồi Member về lớp và Coach, mỗi Member một phản hồi/lớp.';
COMMENT ON TABLE public.membership_packages IS 'Danh mục gói chung/legacy còn được API và chứng từ tham chiếu; không seed gói tháng/VIP.';
COMMENT ON TABLE public.member_packages IS 'Quyền Advanced từng đăng ký: môn, Primary Coach, giá mua đóng băng, credit và hạn.';
COMMENT ON TABLE public.class_enrollments IS 'Ghi danh/waitlist Normal, trạng thái thanh toán và lịch sử hủy/thăng hạng.';
COMMENT ON TABLE public.holidays IS 'Ngày nghỉ trung tâm và phần thời gian đóng cửa thực tế đã xảy ra.';
COMMENT ON TABLE public.invoices IS 'Chứng từ thu tiền bất biến, holding, đối soát và liên kết lớp/quyền học.';
COMMENT ON TABLE public.support_requests IS 'Dự phòng: phiếu hỗ trợ Member có trạng thái và Receptionist phụ trách.';
COMMENT ON TABLE public.notifications IS 'Thông báo nghiệp vụ và khóa chống gửi trùng theo sự kiện.';
COMMENT ON TABLE public.notification_recipients IS 'Trạng thái đọc của từng người nhận thông báo.';
COMMENT ON TABLE public.chat_conversations IS 'Hội thoại hỗ trợ Member với nhân viên/AI.';
COMMENT ON TABLE public.chat_messages IS 'Nội dung hội thoại, người gửi và khóa chống gửi lặp.';
COMMENT ON TABLE public.chat_read_state IS 'Con trỏ tin nhắn đã đọc của mỗi người trong hội thoại.';
COMMENT ON TABLE public.ai_consultations IS 'Dự phòng: lịch sử yêu cầu và phản hồi tư vấn AI độc lập.';
COMMENT ON TABLE public.system_logs IS 'Audit actor/hành động/đối tượng; giữ nguyên lịch sử khi nâng cấp.';
COMMENT ON TABLE public.normal_class_requests IS 'UUID/hash yêu cầu tạo Normal để retry không sinh lớp/lịch lần hai.';
COMMENT ON TABLE public.subscription_pauses IS 'Khoảng gián đoạn có căn cứ dùng khi tính hạn Advanced.';
COMMENT ON TABLE public.coach_work_schedules IS 'Trạng thái nhận đăng ký mới của từng Coach.';
COMMENT ON TABLE public.coach_work_slots IS 'Các ca làm việc theo thứ/ngày của Coach; khóa ghép.';
COMMENT ON TABLE public.coach_days_off IS 'Ngày nghỉ riêng của Coach; khóa ghép.';
COMMENT ON TABLE public.advanced_registrations IS 'Yêu cầu chọn môn/Primary Coach, quyết định nhận và liên kết subscription.';
COMMENT ON TABLE public.session_absence_notices IS 'Báo vắng sát giờ và lịch sử supersede; không tự hoàn credit.';
COMMENT ON TABLE public.session_changes IS 'Audit dời/thay buổi với snapshot trước/sau, consent và idempotency.';
COMMENT ON TABLE public.center_incidents IS 'Hồ sơ sự cố trung tâm, evidence và kết quả bù/phục hồi.';
COMMENT ON TABLE public.incident_affected_members IS 'Danh sách Member bị ảnh hưởng bởi một sự cố.';
COMMENT ON TABLE public.payment_resolutions IS 'Hồ sơ Refund/CashCorrection, phê duyệt, chứng cứ và kết quả thực hiện.';
COMMENT ON TABLE public.scms_data_migration_audit IS 'Dấu migration một lần; replay không ghi đè quyết định giá/quyền đã thay sau đó.';
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
WHERE NOT EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_catalog_accounts_v1')
ON CONFLICT DO NOTHING;


-- The following IDs are reference-list ordinals, not assumptions about an upgraded DB.
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
(7, 'Bóng chuyền', 'bong-chuyen', 1000000, 3500000, FALSE, 5, 'volleyball'),
(8, 'Cầu lông', 'cau-long', 900000, 3000000, FALSE, 7, 'badminton'),
(9, 'Bóng bàn', 'bong-ban', 800000, 2500000, FALSE, 8, 'tabletennis');

CREATE TEMP TABLE _scms_rooms(ordinal INT PRIMARY KEY,name TEXT,location TEXT,capacity INT,facility_type TEXT) ON COMMIT DROP;
INSERT INTO _scms_rooms VALUES
(1, 'Sân Tennis 01', 'Khu Tennis ngoài trời', 4, 'Outdoor'),
(2, 'Hồ bơi 01', 'Khu bơi ngoài trời', 20, 'Outdoor'),
(3, 'Sân Pickleball 01', 'Khu Pickleball trong nhà', 12, 'Indoor'),
(4, 'Khu Bắn cung 01', 'Khu Bắn cung ngoài trời', 10, 'Outdoor'),
(5, 'Sân đa năng 01', 'Nhà thi đấu dùng chung Bóng rổ/Bóng chuyền', 30, 'Indoor'),
(6, 'Sân Bóng đá 01', 'Khu Bóng đá ngoài trời', 30, 'Outdoor'),
(7, 'Sân Cầu lông 01', 'Nhà thi đấu Cầu lông', 20, 'Indoor'),
(8, 'Khu Bóng bàn 01', 'Khu Bóng bàn trong nhà', 15, 'Indoor');

CREATE TEMP TABLE _scms_room_aliases(ordinal INT,old_name TEXT) ON COMMIT DROP;
INSERT INTO _scms_room_aliases VALUES
(1, 'Sân Tennis ngoài trời 1'),
(2, 'Hồ bơi ngoài trời tiêu chuẩn'),
(3, 'Sân Pickleball có mái che'),
(4, 'Trường bắn ngoài trời'),
(5, 'Nhà thi đấu đa năng (Bóng rổ & Chuyền)'),
(6, 'Sân cỏ ngoài trời (Bóng đá)'),
(7, 'Nhà thi đấu Cầu lông (Sàn thảm)'),
(8, 'Phòng Bóng bàn tiêu chuẩn');

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

-- The one-time root pass may add catalog/identity rows but never rewrite transaction rows.
-- PDF identities are matched by EMAIL and role; names never identify an old account.
DO $identities$
DECLARE a RECORD; u RECORD; new_id INT; n INT; rebuild BOOLEAN:=current_setting('scms.rebuild',true)='true';
BEGIN
 IF NOT rebuild AND EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_catalog_accounts_v1') THEN RETURN; END IF;
 FOR a IN SELECT * FROM _scms_accounts ORDER BY ordinal LOOP
  IF (SELECT count(*) FROM users WHERE lower(email)=a.email)>1 THEN RAISE EXCEPTION 'PDF email % has multiple case-insensitive matches; reconcile identities first',a.email; END IF;
  SELECT x.user_id,r.role_name INTO u FROM users x JOIN roles r ON r.role_id=x.role_id WHERE lower(x.email)=a.email;
  IF FOUND THEN
   IF u.role_name<>a.role_name THEN RAISE EXCEPTION 'PDF email % belongs to another role; reconcile identity instead of reusing an ID',a.email; END IF;
   UPDATE users SET full_name=a.name,phone=a.phone,address=a.address,gender=a.gender,date_of_birth=a.birth,
      status='Active',center_id=CASE WHEN a.role_name='Admin' THEN NULL ELSE 1 END WHERE user_id=u.user_id;
   new_id:=u.user_id;
  ELSE
   IF rebuild THEN
    INSERT INTO users(user_id,role_id,full_name,email,phone,password_hash,address,gender,date_of_birth,status,center_id,is_first_login)
    SELECT a.ordinal,role_id,a.name,a.email,a.phone,crypt('SWP391',gen_salt('bf',10)),a.address,a.gender,a.birth,'Active',CASE WHEN a.role_name='Admin' THEN NULL ELSE 1 END,FALSE FROM roles WHERE role_name=a.role_name
    RETURNING user_id INTO new_id;
   ELSE
    INSERT INTO users(role_id,full_name,email,phone,password_hash,address,gender,date_of_birth,status,center_id,is_first_login)
    SELECT role_id,a.name,a.email,a.phone,crypt('SWP391',gen_salt('bf',10)),a.address,a.gender,a.birth,'Active',CASE WHEN a.role_name='Admin' THEN NULL ELSE 1 END,FALSE FROM roles WHERE role_name=a.role_name
    RETURNING user_id INTO new_id;
   END IF;
  END IF;
  IF a.role_name='CenterManager' THEN INSERT INTO center_managers VALUES(new_id) ON CONFLICT DO NOTHING;
  ELSIF a.role_name='Receptionist' THEN INSERT INTO receptionists VALUES(new_id) ON CONFLICT DO NOTHING;
  ELSIF a.role_name='Coach' THEN INSERT INTO coaches(user_id) VALUES(new_id) ON CONFLICT DO NOTHING;
  ELSIF a.role_name='Member' THEN INSERT INTO members(user_id,goal,join_date,registered_by) VALUES(new_id,NULL,(CURRENT_DATE-30),NULL) ON CONFLICT DO NOTHING;
  END IF;
 END LOOP;
 UPDATE users SET status='Inactive' WHERE lower(email) NOT IN(SELECT email FROM _scms_accounts);
 FOR a IN SELECT * FROM _scms_subjects ORDER BY ordinal LOOP
  SELECT count(*) INTO n FROM subjects WHERE slug=a.slug;
  IF n=0 THEN
   IF rebuild THEN
    INSERT INTO subjects(subject_id,subject_name,slug,normal_price,advanced_price,outdoor,status) VALUES(a.ordinal,a.name,a.slug,a.normal_price,a.advanced_price,a.outdoor,'Active');
   ELSE
    INSERT INTO subjects(subject_name,slug,normal_price,advanced_price,outdoor,status) VALUES(a.name,a.slug,a.normal_price,a.advanced_price,a.outdoor,'Active');
   END IF;
  ELSE
   UPDATE subjects SET subject_name=a.name,normal_price=a.normal_price,advanced_price=a.advanced_price,outdoor=a.outdoor,status='Active' WHERE slug=a.slug;
  END IF;
 END LOOP;
END $identities$;

-- Owner correction for the earlier random-password seed, scoped to the PDF identities.
-- A separate key is required because catalog_accounts_v1 may already have been executed.
-- Subsequent ROOT executions preserve password changes, lock state and first-login policy.
DO $demo_passwords$
DECLARE account_row RECORD; identity_row RECORD;
BEGIN
 IF EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_demo_passwords_v2') THEN RETURN; END IF;
 FOR account_row IN SELECT * FROM _scms_accounts ORDER BY ordinal LOOP
  IF (SELECT count(*) FROM users WHERE lower(email)=account_row.email)<>1 THEN RAISE EXCEPTION 'Expected one PDF identity for %',account_row.email; END IF;
  SELECT x.user_id,r.role_name INTO identity_row FROM users x JOIN roles r ON r.role_id=x.role_id WHERE lower(x.email)=account_row.email;
  IF identity_row.role_name<>account_row.role_name THEN RAISE EXCEPTION 'Demo password correction: role mismatch for %',account_row.email; END IF;
  UPDATE users SET password_hash=crypt('SWP391',gen_salt('bf',10)),
   is_first_login=FALSE,failed_login_attempts=0,locked_until=NULL WHERE user_id=identity_row.user_id;
 END LOOP;
 IF EXISTS(SELECT 1 FROM _scms_accounts a JOIN users u ON lower(u.email)=a.email
  WHERE crypt('SWP391',u.password_hash)<>u.password_hash OR u.is_first_login OR u.failed_login_attempts<>0 OR u.locked_until IS NOT NULL) THEN
  RAISE EXCEPTION 'Demo password/first-login/lock postflight failed';
 END IF;
 INSERT INTO scms_data_migration_audit VALUES('20261029_demo_passwords_v2',localtimestamp,
  'Owner-approved correction of 33 PDF demo identities only; existing IDs and business history preserved',
  'Salted BCrypt; no forced first-login change for demo seed; replay preserves later account changes');
END $demo_passwords$;


-- Remove only unused, noncanonical subjects. Discover ALL actual FK references first.
DO $catalog$
DECLARE s RECORD; fk RECORD; used BOOLEAN;
BEGIN
 IF current_setting('scms.rebuild',true)<>'true' AND EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_catalog_accounts_v1') THEN RETURN; END IF;
 FOR s IN SELECT subject_id FROM subjects WHERE slug IS NULL OR slug NOT IN(SELECT slug FROM _scms_subjects) LOOP
  FOR fk IN SELECT c.conrelid::regclass AS tbl,a.attname AS col,cardinality(c.conkey) AS width FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
    WHERE c.contype='f' AND c.confrelid='subjects'::regclass LOOP
   IF fk.width<>1 THEN RAISE EXCEPTION 'Composite subject FK requires manual review'; END IF;
   EXECUTE format('SELECT EXISTS(SELECT 1 FROM %s WHERE %I=$1)',fk.tbl,fk.col) INTO used USING s.subject_id;
   IF used THEN RAISE EXCEPTION 'Noncanonical subject % referenced by %; transaction stopped without deleting history',s.subject_id,fk.tbl; END IF;
  END LOOP;
  DELETE FROM subjects WHERE subject_id=s.subject_id;
 END LOOP;
END $catalog$;

DO $resources$
DECLARE r RECORD; n INT; rid INT; sid INT; spid INT; rebuild BOOLEAN:=current_setting('scms.rebuild',true)='true';
BEGIN
 IF NOT rebuild AND EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_catalog_accounts_v1') THEN RETURN; END IF;
 FOR r IN SELECT * FROM _scms_rooms ORDER BY ordinal LOOP
  SELECT count(*),min(room_id) INTO n,rid FROM rooms WHERE center_id=1 AND (room_name=r.name OR room_name IN(SELECT old_name FROM _scms_room_aliases WHERE ordinal=r.ordinal));
  IF n>1 THEN RAISE EXCEPTION 'Room % has multiple old IDs; reconcile its sessions before consolidation',r.name; END IF;
  IF n=0 THEN
   IF rebuild THEN INSERT INTO rooms(room_id,room_name,location,capacity,status,facility_type,center_id) VALUES(r.ordinal,r.name,r.location,r.capacity,'Available',r.facility_type,1);
   ELSE INSERT INTO rooms(room_name,location,capacity,status,facility_type,center_id) VALUES(r.name,r.location,r.capacity,'Available',r.facility_type,1); END IF;
  ELSE UPDATE rooms SET room_name=r.name,location=r.location,facility_type=r.facility_type WHERE room_id=rid;
  END IF;
 END LOOP;
 -- Closing an obsolete room with future sessions is rejected by its normal business trigger.
 UPDATE rooms SET status='Closed' WHERE center_id=1 AND room_name NOT IN(SELECT name FROM _scms_rooms) AND status<>'Closed';
 FOR r IN SELECT * FROM _scms_subjects ORDER BY ordinal LOOP
  SELECT subject_id INTO sid FROM subjects WHERE slug=r.slug;
  SELECT specialization_id INTO spid FROM specializations WHERE lower(name)=lower(r.name);
  IF NOT FOUND AND r.ordinal=1 THEN SELECT specialization_id INTO spid FROM specializations WHERE name IN('Quần vợt (Tennis)','Quần vợt') ORDER BY specialization_id LIMIT 1; END IF;
  IF spid IS NULL THEN
   IF rebuild THEN INSERT INTO specializations(specialization_id,name,subject_id,status) VALUES(r.ordinal,r.name,sid,'Active') RETURNING specialization_id INTO spid;
   ELSE INSERT INTO specializations(name,subject_id,status) VALUES(r.name,sid,'Active') RETURNING specialization_id INTO spid; END IF;
  ELSE UPDATE specializations SET name=r.name,subject_id=sid,status='Active' WHERE specialization_id=spid; END IF;
  DELETE FROM coach_specializations cs USING users u WHERE cs.coach_id=u.user_id AND u.email IN('coach.'||r.coach_code||'1@gmail.com','coach.'||r.coach_code||'2@gmail.com');
  INSERT INTO coach_specializations SELECT user_id,spid FROM users WHERE email IN('coach.'||r.coach_code||'1@gmail.com','coach.'||r.coach_code||'2@gmail.com');
  UPDATE coaches c SET specialization=r.name,certification=NULL,bio='Huấn luyện viên môn '||r.name||'.'
   FROM users u WHERE c.user_id=u.user_id AND u.email IN('coach.'||r.coach_code||'1@gmail.com','coach.'||r.coach_code||'2@gmail.com');
  SELECT count(*) INTO n FROM classes WHERE subject_id=sid AND class_type='Advanced' AND center_id=1;
  IF n>1 THEN RAISE EXCEPTION 'Multiple Advanced templates for subject %; review class/session links',r.slug; END IF;
  IF n=0 THEN
   IF rebuild THEN INSERT INTO classes(class_id,class_name,subject_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id) VALUES(r.ordinal,r.name||' - Advanced (1:1)',sid,1,r.advanced_price,'Open','Advanced',12,1);
   ELSE INSERT INTO classes(class_name,subject_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id) VALUES(r.name||' - Advanced (1:1)',sid,1,r.advanced_price,'Open','Advanced',12,1); END IF;
  ELSE UPDATE classes SET class_name=r.name||' - Advanced (1:1)' WHERE subject_id=sid AND class_type='Advanced' AND center_id=1; END IF;
 END LOOP;
 -- Historical coach identity remains intact. Remove only the disallowed descriptive phrases.
 UPDATE coaches SET specialization=trim(both ' ,;' from regexp_replace(coalesce(specialization,''),'(fitness|thể hình|gym|yoga|cardio)','', 'gi')),
   bio=CASE WHEN coalesce(bio,'')~*'(fitness|thể hình|gym|yoga|cardio)' THEN NULL ELSE bio END
 WHERE coalesce(specialization,'')~*'(fitness|thể hình|gym|yoga|cardio)' OR coalesce(bio,'')~*'(fitness|thể hình|gym|yoga|cardio)';
 DELETE FROM coach_specializations WHERE specialization_id IN(SELECT specialization_id FROM specializations WHERE lower(name) IN('fitness','thể hình','gym','yoga','cardio'));
 UPDATE specializations SET status='Inactive' WHERE lower(name) IN('fitness','thể hình','gym','yoga','cardio');
 UPDATE membership_packages SET status='Inactive' WHERE package_name IN('Gói Cơ Bản 1 Tháng','Gói Tiêu Chuẩn 3 Tháng','Gói Cao Cấp 6 Tháng','Gói VIP 12 Tháng','Gói Khóa Học 1:1 (12 Buổi Linh Hoạt)');
 UPDATE classes c SET class_name=s.name||' - Normal - K01' FROM subjects su JOIN _scms_subjects s ON su.slug=s.slug
  WHERE c.subject_id=su.subject_id AND c.class_type='Normal' AND c.center_id=1 AND c.class_name IN('Lớp Cầu Lông Cơ Bản (K1)','Lớp Quần Vợt Phong Trào','Lớp Bơi Lội Căn Bản','Lớp Pickleball Nhập Môn','Lớp Bóng Đá Căn Bản','Lớp Bóng Bàn Căn Bản');
END $resources$;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS ck_approved_subject_slug;
ALTER TABLE subjects ADD CONSTRAINT ck_approved_subject_slug CHECK(slug IS NOT NULL AND slug IN('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban'));
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS ck_subject_positive_prices;
ALTER TABLE subjects ADD CONSTRAINT ck_subject_positive_prices CHECK(normal_price>0 AND advanced_price>0);
CREATE UNIQUE INDEX IF NOT EXISTS uq_scms_rooms_center_name ON rooms(center_id,lower(room_name)) WHERE status='Available';

-- REFERENCE BUSINESS DATA, recreated on every DATABASE-variant execution.
-- This is an explicitly synthetic SCMS dataset, not imported payment/attendance evidence.
-- IDs below use the approved PDF account order: Admin 1; Member 2-11; Coach 12-29;
-- Receptionist 30-31; CenterManager 32-33. Subtype IDs MUST remain those user IDs.
CREATE TEMP TABLE _scms_dates ON COMMIT DROP AS
SELECT localtimestamp AS now_at, CURRENT_DATE AS today,
       (CURRENT_DATE-(extract(isodow FROM CURRENT_DATE)::int-1)-21) AS course_anchor;

-- Whole-day holiday closures only. Payment activation below occurs at midnight;
-- the 45 usable-day deadline therefore remains exact even when crossing a holiday.
CREATE OR REPLACE FUNCTION pg_temp.scms_reference_deadline(activated TIMESTAMP) RETURNS TIMESTAMP
LANGUAGE plpgsql AS $$
DECLARE deadline TIMESTAMP:=activated; used INT:=0;
BEGIN
 WHILE used<45 LOOP
  IF NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=deadline::date AND is_active) THEN used:=used+1; END IF;
  deadline:=deadline+interval '1 day';
 END LOOP;
 RETURN deadline;
END $$;

-- Retain the project's configured closure dates without recreating old transactional rows.
INSERT INTO holidays(holiday_id,holiday_date,holiday_name,tag,is_active,description)
SELECT row_number() OVER(ORDER BY holiday_date),holiday_date,holiday_name,tag,TRUE,
       'Ngày đóng cửa trong lịch tham chiếu SCMS; lịch học và hạn Advanced bỏ qua ngày này.'
FROM (VALUES
 (DATE '2026-01-01','Tết Dương lịch 2026','NATIONAL_FIXED'),
 (DATE '2026-02-15','Nghỉ Tết Nguyên đán 2026 - ngày 1','LUNAR_FESTIVAL'),
 (DATE '2026-02-16','Nghỉ Tết Nguyên đán 2026 - ngày 2','LUNAR_FESTIVAL'),
 (DATE '2026-02-17','Nghỉ Tết Nguyên đán 2026 - ngày 3','LUNAR_FESTIVAL'),
 (DATE '2026-02-18','Nghỉ Tết Nguyên đán 2026 - ngày 4','LUNAR_FESTIVAL'),
 (DATE '2026-02-19','Nghỉ Tết Nguyên đán 2026 - ngày 5','LUNAR_FESTIVAL'),
 (DATE '2026-02-20','Nghỉ Tết Nguyên đán 2026 - ngày 6','LUNAR_FESTIVAL'),
 (DATE '2026-02-21','Nghỉ Tết Nguyên đán 2026 - ngày 7','LUNAR_FESTIVAL'),
 (DATE '2026-04-26','Giỗ Tổ Hùng Vương 2026','LUNAR_FESTIVAL'),
 (DATE '2026-04-30','Ngày Giải phóng miền Nam 2026','NATIONAL_FIXED'),
 (DATE '2026-05-01','Ngày Quốc tế Lao động 2026','NATIONAL_FIXED'),
 (DATE '2026-09-02','Quốc khánh 2026','NATIONAL_FIXED'),
 (DATE '2026-09-03','Nghỉ liền kề Quốc khánh 2026','NATIONAL_FIXED'),
 (DATE '2027-01-01','Tết Dương lịch 2027','NATIONAL_FIXED'),
 (DATE '2027-04-30','Ngày Giải phóng miền Nam 2027','NATIONAL_FIXED'),
 (DATE '2027-05-01','Ngày Quốc tế Lao động 2027','NATIONAL_FIXED'),
 (DATE '2027-09-02','Quốc khánh 2027','NATIONAL_FIXED'),
 (DATE '2027-09-03','Nghỉ liền kề Quốc khánh 2027','NATIONAL_FIXED')
) AS closures(holiday_date,holiday_name,tag);

UPDATE users SET created_at=(SELECT course_anchor-120 FROM _scms_dates)::timestamp;
UPDATE members SET join_date=(SELECT course_anchor-120 FROM _scms_dates),registered_by=30;
INSERT INTO coach_work_schedules SELECT user_id,TRUE FROM coaches ORDER BY user_id;
INSERT INTO coach_work_slots(coach_id,slot_key)
SELECT c.user_id,d.day||':'||slot FROM coaches c
CROSS JOIN (VALUES('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY'),('SUNDAY')) d(day)
CROSS JOIN generate_series(1,7) slot;

-- Nine Normal courses, each with twelve VALID sessions and an independent subject price.
-- Tennis capacity is four; Archery ten; no Normal capacity exceeds its physical room.
INSERT INTO classes(class_id,class_name,subject_id,coach_id,max_capacity,tuition_fee,start_date,end_date,status,class_type,total_sessions,center_id)
SELECT s.ordinal+9,s.name||' - Normal - K01',s.ordinal,12+2*(s.ordinal-1),least(15,r.capacity),
 s.normal_price,d.course_anchor,d.course_anchor+60,'Ongoing','Normal',12,1
FROM _scms_subjects s JOIN _scms_rooms r ON r.ordinal=s.room_ordinal CROSS JOIN _scms_dates d ORDER BY s.ordinal;

INSERT INTO sessions(session_id,class_id,room_id,coach_id,session_date,start_time,end_time,status,attendance_finalized,created_by)
SELECT row_number() OVER(ORDER BY subject_ordinal,lesson),subject_ordinal+9,room_ordinal,coach_id,lesson_date,
 start_at,start_at+interval '90 minutes',
 CASE WHEN lesson_date+start_at+interval '90 minutes'<d.now_at THEN 'Completed' ELSE 'Scheduled' END,
 lesson_date+start_at+interval '90 minutes'<d.now_at,32
FROM (
 SELECT s.ordinal AS subject_ordinal,s.room_ordinal,12+2*(s.ordinal-1) AS coach_id,
        calendar.lesson,calendar.lesson_date,CASE WHEN s.ordinal=7 THEN TIME '07:45' ELSE TIME '06:00' END AS start_at
 FROM _scms_subjects s CROSS JOIN LATERAL (
  SELECT row_number() OVER(ORDER BY x.day) AS lesson,x.day::date AS lesson_date
  FROM generate_series((SELECT course_anchor FROM _scms_dates),(SELECT course_anchor+100 FROM _scms_dates),interval '1 day') x(day)
  WHERE extract(isodow FROM x.day) IN(1,4) AND NOT EXISTS(SELECT 1 FROM holidays h WHERE h.holiday_date=x.day::date AND h.is_active)
  ORDER BY x.day LIMIT 12
 ) calendar
) lessons CROSS JOIN _scms_dates d ORDER BY subject_ordinal,lesson;
UPDATE classes c SET start_date=x.first_day,end_date=x.last_day FROM
 (SELECT class_id,min(session_date) first_day,max(session_date) last_day FROM sessions GROUP BY class_id) x WHERE c.class_id=x.class_id;

-- Cash Paid is a synthetic reference event; no gateway reference or real remittance is fabricated.
INSERT INTO invoices(invoice_id,member_id,class_id,receptionist_id,amount,invoice_code,payment_method,payment_status,payment_date,created_at,expires_at,cash_note)
SELECT s.ordinal,s.ordinal+1,s.ordinal+9,30,s.normal_price,'INV-SCMS-N-'||lpad(s.ordinal::text,3,'0'),
 'Cash','Paid',(d.course_anchor-2)+TIME '08:02',(d.course_anchor-2)+TIME '08:00',
 (d.course_anchor-2)+TIME '08:05','Dữ liệu tham chiếu được dựng lại; không phải xác nhận thu tiền thực tế.'
FROM _scms_subjects s CROSS JOIN _scms_dates d ORDER BY s.ordinal;
INSERT INTO class_enrollments(enrollment_id,member_id,class_id,enrolled_at,status)
SELECT s.ordinal,s.ordinal+1,s.ordinal+9,(d.course_anchor-2)+TIME '08:02','Registered'
FROM _scms_subjects s CROSS JOIN _scms_dates d ORDER BY s.ordinal;

-- D15: a center-cancelled Normal session is retained and linked to exactly one makeup.
UPDATE sessions SET status='Cancelled',center_invalidated=TRUE,attendance_finalized=TRUE,
 support_reason='Sân Tennis bảo trì đột xuất; buổi gốc không tổ chức, trung tâm xếp buổi bù.' WHERE session_id=1;
INSERT INTO center_incidents(id,session_id,request_key,started_at,ended_at,reason,evidence,status,created_by,created_at,resolved_by,resolved_at,session_compensated)
SELECT 1,1,'normal-tennis-maintenance-01',s.session_date+s.start_time-interval '1 hour',s.session_date+s.end_time,
 'Sân Tennis bảo trì đột xuất, buổi học không tổ chức.',
 'SCMS_REFERENCE: hồ sơ tham chiếu sự cố sân, xác minh không có hoạt động học trong buổi gốc.',
 'Resolved',32,s.session_date+s.start_time-interval '1 hour',32,s.session_date+s.end_time,TRUE FROM sessions s WHERE s.session_id=1;
INSERT INTO incident_affected_members VALUES(1,2);
INSERT INTO sessions(session_id,class_id,room_id,coach_id,session_date,start_time,end_time,status,makeup_for_session_id,created_by,support_reason)
SELECT 109,10,1,12,x.day::date,TIME '06:00',TIME '07:30','Scheduled',1,32,
 'Buổi bù cho buổi Tennis không tổ chức do sân bảo trì; giữ nguyên quyền Normal.'
FROM classes c CROSS JOIN LATERAL (
 SELECT day FROM generate_series(c.end_date+1,c.end_date+20,interval '1 day') day
 WHERE extract(isodow FROM day)=5 AND NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=day::date AND is_active)
 ORDER BY day LIMIT 1
) x WHERE c.class_id=10;
UPDATE classes SET end_date=(SELECT session_date FROM sessions WHERE session_id=109) WHERE class_id=10;

-- Four Advanced registrations: two Paid/Active, one Pending/zero credit, one Expired.
INSERT INTO member_packages(subscription_id,member_id,subject_id,primary_coach_id,purchase_amount,remaining_credit,validity_days,status,start_date,end_date,activated_at,expires_at,expiry_revoked_credit,expiry_processed_at)
SELECT 1,11,8,26,3000000,9,45,'Active',d.course_anchor-1,
 pg_temp.scms_reference_deadline((d.course_anchor-1)::timestamp)::date,
 (d.course_anchor-1)::timestamp,pg_temp.scms_reference_deadline((d.course_anchor-1)::timestamp),0,NULL::timestamp FROM _scms_dates d
UNION ALL
SELECT 2,2,1,12,8000000,11,45,'Active',d.course_anchor-1,
 pg_temp.scms_reference_deadline((d.course_anchor-1)::timestamp)::date,
 (d.course_anchor-1)::timestamp,pg_temp.scms_reference_deadline((d.course_anchor-1)::timestamp),0,NULL FROM _scms_dates d
UNION ALL
SELECT 3,3,3,16,5000000,0,45,'Pending',NULL,NULL,NULL,NULL,0,NULL FROM _scms_dates
UNION ALL
SELECT 4,4,5,20,3500000,0,45,'Expired',d.today-80,
 pg_temp.scms_reference_deadline((d.today-80)::timestamp)::date,
 (d.today-80)::timestamp,pg_temp.scms_reference_deadline((d.today-80)::timestamp),12,
 pg_temp.scms_reference_deadline((d.today-80)::timestamp) FROM _scms_dates d;
INSERT INTO advanced_registrations(id,member_id,subject_id,coach_id,request_key,status,created_at,accepted_by,accepted_at,reason,subscription_id)
SELECT p.subscription_id,p.member_id,p.subject_id,p.primary_coach_id,'advanced-reference-'||p.subscription_id,
 'Invoiced',coalesce(p.activated_at,d.now_at)-interval '1 day',p.primary_coach_id,
 coalesce(p.activated_at,d.now_at)-interval '1 hour','Primary Coach nhận đăng ký đúng môn trước khi tạo hóa đơn.',p.subscription_id
FROM member_packages p CROSS JOIN _scms_dates d ORDER BY p.subscription_id;
INSERT INTO invoices(invoice_id,member_id,subscription_id,receptionist_id,amount,invoice_code,payment_method,payment_status,payment_date,created_at,expires_at,cash_note)
SELECT p.subscription_id+9,p.member_id,p.subscription_id,30,p.purchase_amount,
 'INV-SCMS-A-'||lpad(p.subscription_id::text,3,'0'),'Cash',CASE WHEN p.status='Pending' THEN 'Pending' ELSE 'Paid' END,
 CASE WHEN p.status='Pending' THEN NULL ELSE p.activated_at END,
 coalesce(p.activated_at-interval '2 minutes',d.now_at),coalesce(p.activated_at-interval '2 minutes',d.now_at)+interval '5 minutes',
 'Dữ liệu tham chiếu được dựng lại; giá mua được chốt độc lập với thay đổi bảng giá sau này.'
FROM member_packages p CROSS JOIN _scms_dates d ORDER BY p.subscription_id;

-- Two past Cau long sessions and one future booking; total credit consumed = three.
INSERT INTO sessions(session_id,class_id,room_id,coach_id,booked_member_id,subscription_id,booking_key,booking_hash,
 session_date,start_time,end_time,status,attendance_finalized,created_by,agreement_confirmed_at)
SELECT 109+row_number() OVER(ORDER BY x.day),8,7,26,11,1,'badminton-reference-'||row_number() OVER(ORDER BY x.day),repeat('a',64),
 x.day::date,TIME '07:45',TIME '09:15','Completed',TRUE,26,(d.course_anchor-1)::timestamp
FROM _scms_dates d CROSS JOIN LATERAL (
 SELECT day FROM generate_series(d.course_anchor+1,d.today-1,interval '1 day') day
 WHERE extract(isodow FROM day)=2 AND NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=day::date AND is_active)
 ORDER BY day LIMIT 2
) x;
INSERT INTO sessions(session_id,class_id,room_id,coach_id,booked_member_id,subscription_id,booking_key,booking_hash,
 session_date,start_time,end_time,status,created_by,agreement_confirmed_at)
SELECT 112,8,7,26,11,1,'badminton-reference-3',repeat('b',64),x.day::date,TIME '07:45',TIME '09:15','Scheduled',26,d.now_at
FROM _scms_dates d CROSS JOIN LATERAL (
 SELECT day FROM generate_series(d.today+2,d.today+20,interval '1 day') day
 WHERE extract(isodow FROM day)=2 AND NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=day::date AND is_active)
 ORDER BY day LIMIT 1
) x;

-- A timely Member cancellation returns one credit; one other Tennis booking remains.
INSERT INTO sessions(session_id,class_id,room_id,coach_id,booked_member_id,subscription_id,booking_key,booking_hash,
 session_date,start_time,end_time,status,credit_returned,attendance_finalized,created_by,agreement_confirmed_at,support_reason)
SELECT 113,1,1,12,2,2,'tennis-reference-cancelled',repeat('c',64),x.day::date,TIME '07:45',TIME '09:15','Cancelled',TRUE,TRUE,12,
 (d.course_anchor-1)::timestamp,'Member hủy trước ít nhất 12 giờ; hoàn đúng 1 credit vào đăng ký Tennis gốc.'
FROM _scms_dates d CROSS JOIN LATERAL (
 SELECT day FROM generate_series(d.course_anchor+1,d.today-1,interval '1 day') day
 WHERE extract(isodow FROM day)=3 AND NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=day::date AND is_active)
 ORDER BY day LIMIT 1
) x;
INSERT INTO sessions(session_id,class_id,room_id,coach_id,booked_member_id,subscription_id,booking_key,booking_hash,
 session_date,start_time,end_time,status,created_by,agreement_confirmed_at)
SELECT 114,1,1,12,2,2,'tennis-reference-next',repeat('d',64),x.day::date,TIME '07:45',TIME '09:15','Scheduled',12,d.now_at
FROM _scms_dates d CROSS JOIN LATERAL (
 SELECT day FROM generate_series(d.today+2,d.today+20,interval '1 day') day
 WHERE extract(isodow FROM day)=3 AND NOT EXISTS(SELECT 1 FROM holidays WHERE holiday_date=day::date AND is_active)
 ORDER BY day LIMIT 1
) x;
INSERT INTO session_absence_notices(id,session_id,member_id,reported_at,original_start,original_end,reason,active)
SELECT 1,111,11,session_date+start_time-interval '6 hours',session_date+start_time,session_date+end_time,
 'Member báo không thể đến; Coach xác nhận vắng trong buổi. Không hoàn credit vì báo sát giờ.',TRUE FROM sessions WHERE session_id=111;

-- Attendance is recorded DURING completed sessions, only Present/Absent; no check-in/out shortcut.
INSERT INTO attendances(attendance_id,session_id,member_id,recorded_by,state,note)
SELECT row_number() OVER(ORDER BY x.session_id,x.member_id),x.session_id,x.member_id,x.coach_id,
 CASE WHEN x.session_id=111 OR x.member_id%3=0 AND x.session_id%5=0 THEN 'Absent' ELSE 'Present' END,
 CASE WHEN x.session_id=111 THEN 'Đã báo vắng sát giờ; giữ credit đã dùng để đặt buổi.'
      WHEN x.member_id%3=0 AND x.session_id%5=0 THEN 'Member vắng vì lý do cá nhân; không phát sinh buổi bù.'
      ELSE 'Coach xác nhận Member tham gia buổi học.' END
FROM (
 SELECT s.session_id,e.member_id,s.coach_id FROM sessions s JOIN class_enrollments e ON e.class_id=s.class_id
  WHERE s.status='Completed' AND NOT s.center_invalidated
 UNION ALL
 SELECT session_id,booked_member_id,coach_id FROM sessions WHERE booked_member_id IS NOT NULL AND status='Completed' AND NOT center_invalidated
) x;
INSERT INTO training_results(result_id,session_id,member_id,coach_id,content,attendance_status,recorded_at)
SELECT a.attendance_id,a.session_id,a.member_id,a.recorded_by,
 CASE WHEN a.state='Present' THEN 'Hoàn thành nội dung kỹ thuật theo kế hoạch của môn '||su.subject_name||'.'
      ELSE 'Không tham gia buổi học; Coach lưu vắng theo tình hình thực tế.' END,
 a.state,s.session_date+s.start_time+interval '20 minutes'
FROM attendances a JOIN sessions s ON s.session_id=a.session_id JOIN classes c ON c.class_id=s.class_id JOIN subjects su ON su.subject_id=c.subject_id
ORDER BY a.attendance_id;

INSERT INTO training_plans(plan_id,coach_id,class_id,title,content,goal,start_date,end_date,created_at)
SELECT s.ordinal,c.coach_id,c.class_id,'Kế hoạch '||s.name||' - Normal - K01',
 CASE s.ordinal WHEN 1 THEN 'Giao bóng, bộ chân và đánh thuận/trái tay.' WHEN 2 THEN 'An toàn dưới nước, thở và kỹ thuật bơi căn bản.'
 WHEN 3 THEN 'Luật chơi, dink và phối hợp đánh đôi.' WHEN 4 THEN 'An toàn trường bắn, tư thế và kiểm soát cung.'
 WHEN 5 THEN 'Dẫn bóng, chuyền và ném rổ.' WHEN 6 THEN 'Chuyền, kiểm soát bóng và phối hợp đội.'
 WHEN 7 THEN 'Đệm, chuyền và phát bóng.' WHEN 8 THEN 'Cầm vợt, di chuyển và cầu cao sâu.' ELSE 'Giao bóng, đỡ giao bóng và kiểm soát xoáy.' END,
 'Hoàn thành 12 buổi hợp lệ theo khóa Normal.',c.start_date,c.end_date,(c.start_date-1)::timestamp
FROM _scms_subjects s JOIN classes c ON c.class_id=s.ordinal+9 ORDER BY s.ordinal;
INSERT INTO evaluations(evaluation_id,member_id,coach_id,evaluation_date,comment,progress_score)
SELECT e.enrollment_id,e.member_id,c.coach_id,max(s.session_date),'Đã có tiến bộ qua các buổi đã tham gia; tiếp tục theo kế hoạch môn '||su.subject_name||'.',7.0
FROM class_enrollments e JOIN classes c ON c.class_id=e.class_id JOIN subjects su ON su.subject_id=c.subject_id
JOIN sessions s ON s.class_id=c.class_id AND s.status='Completed' AND NOT s.center_invalidated
GROUP BY e.enrollment_id,e.member_id,c.coach_id,su.subject_name ORDER BY e.enrollment_id;

INSERT INTO notifications(notification_id,user_id,sender_id,title,content,type,notification_type,related_type,related_id,dedupe_key,created_at)
SELECT row_number() OVER(ORDER BY invoice_id),member_id,30,'Đăng ký đã được thanh toán',
 CASE WHEN class_id IS NOT NULL THEN 'Khóa Normal đã được ghi danh. Vui lòng xem đủ lịch 12 buổi hợp lệ.'
 ELSE 'Đăng ký Advanced đã được kích hoạt theo môn và Primary Coach đã chọn.' END,
 'Payment','PAYMENT_SUCCESS','INVOICE',invoice_id,'PAYMENT_SUCCESS:INVOICE:'||invoice_id,payment_date
FROM invoices WHERE payment_status='Paid' ORDER BY invoice_id;
INSERT INTO notification_recipients(notification_id,user_id,is_read,created_at)
SELECT notification_id,user_id,FALSE,created_at FROM notifications ORDER BY notification_id;
INSERT INTO chat_conversations(id,member_user_id,assigned_staff_user_id,handler_type,subject,created_at,updated_at,last_message_at)
SELECT 1,11,30,'STAFF','Trao đổi về lịch Cầu lông Advanced',now_at-interval '2 hours',now_at-interval '1 hour',now_at-interval '1 hour' FROM _scms_dates;
INSERT INTO chat_messages(id,conversation_id,sender_type,sender_user_id,content,created_at)
SELECT 1,1,'MEMBER',11,'Tôi muốn xác nhận lịch Cầu lông 1:1 sắp tới với Coach.',now_at-interval '2 hours' FROM _scms_dates
UNION ALL
SELECT 2,1,'STAFF',30,'Lịch đã đặt được hiển thị trong lịch cá nhân. Nếu cần dời, vui lòng trao đổi với Primary Coach trước ít nhất 12 giờ.',now_at-interval '1 hour' FROM _scms_dates;
INSERT INTO chat_read_state(conversation_id,user_id,last_read_message_id,updated_at)
SELECT 1,u,2,now_at FROM _scms_dates CROSS JOIN (VALUES(11),(30)) readers(u);

INSERT INTO system_logs(log_id,user_id,action,target_entity,target_id,detail,"timestamp")
SELECT row_number() OVER(ORDER BY invoice_id),30,'CONFIRM_CASH','INVOICE',invoice_id,
 'SCMS_REFERENCE: chứng từ tham chiếu; giá chốt='||amount||'; không đại diện khoản thu thực tế.',payment_date
FROM invoices WHERE payment_status='Paid' ORDER BY invoice_id;
INSERT INTO system_logs(log_id,user_id,action,target_entity,target_id,detail,"timestamp")
SELECT 13,32,'CREATE_LINKED_NORMAL_MAKEUP','SESSION',109,'SCMS_REFERENCE: original=1; incident=1; giữ buổi gốc không hợp lệ và đúng một buổi bù.',now_at-interval '1 day' FROM _scms_dates
UNION ALL
SELECT 14,2,'CANCEL_PRIVATE_SESSION','SESSION',113,'SCMS_REFERENCE: hủy sớm hơn 12 giờ, hoàn một credit vào subscription=2.',session_date+start_time-interval '1 day' FROM sessions WHERE session_id=113;

INSERT INTO scms_data_migration_audit VALUES
 ('20261029_catalog_accounts_v1',localtimestamp,'Owner-authorized replacement of SCMS reference dataset','DATABASE variant: 33 PDF accounts; 9 subjects; 8 rooms; integer IDs rebuilt from one'),
 ('09_approved_catalog',localtimestamp,'Consolidated approved catalog','Historical price migration superseded; future replay preserves price edits');

-- Rebuild invariants cover relationships and business state, not just row counts.
DO $reference_checks$
DECLARE seq_row RECORD; n BIGINT; lo BIGINT; hi BIGINT;
BEGIN
 IF (SELECT count(*) FROM users)<>33 OR (SELECT count(*) FROM coaches)<>18 OR (SELECT count(*) FROM members)<>10 OR (SELECT count(*) FROM rooms)<>8 THEN RAISE EXCEPTION 'Reference identity/resource counts differ from the approved lists'; END IF;
 IF EXISTS(SELECT 1 FROM member_packages p JOIN invoices i ON i.subscription_id=p.subscription_id WHERE p.purchase_amount<>i.amount OR p.status='Pending' AND (p.remaining_credit<>0 OR p.activated_at IS NOT NULL OR p.expires_at IS NOT NULL)) THEN RAISE EXCEPTION 'Advanced purchase amount or Pending entitlement is inconsistent'; END IF;
 IF EXISTS(SELECT 1 FROM invoices i JOIN member_packages p ON p.subscription_id=i.subscription_id WHERE i.member_id<>p.member_id) THEN RAISE EXCEPTION 'Invoice and Advanced subscription owners differ'; END IF;
 IF EXISTS(SELECT 1 FROM member_packages p WHERE p.status='Active' AND p.remaining_credit<>12-(SELECT count(*) FROM sessions s WHERE s.subscription_id=p.subscription_id AND s.status<>'Cancelled' AND NOT s.center_invalidated)) THEN RAISE EXCEPTION 'Advanced credit ledger does not balance'; END IF;
 IF EXISTS(SELECT 1 FROM classes c WHERE c.class_type='Normal' AND 12<>(SELECT count(*) FROM sessions s WHERE s.class_id=c.class_id AND NOT s.center_invalidated AND s.status<>'Cancelled')) THEN RAISE EXCEPTION 'Normal must have exactly twelve valid sessions'; END IF;
 IF EXISTS(SELECT 1 FROM class_enrollments e WHERE e.status='Registered' AND NOT EXISTS(SELECT 1 FROM invoices i WHERE i.class_id=e.class_id AND i.member_id=e.member_id AND i.payment_status='Paid' AND NOT i.reconciliation_required AND NOT i.cash_confirmation_reversed)) THEN RAISE EXCEPTION 'Registered Normal enrollment has no eligible Paid invoice'; END IF;
 IF EXISTS(SELECT 1 FROM sessions a JOIN sessions b ON a.session_id<b.session_id AND a.session_date=b.session_date AND a.start_time<b.end_time AND b.start_time<a.end_time WHERE a.status<>'Cancelled' AND b.status<>'Cancelled' AND NOT a.center_invalidated AND NOT b.center_invalidated AND (a.room_id=b.room_id OR a.coach_id=b.coach_id)) THEN RAISE EXCEPTION 'Room/Coach overlap in reference schedule'; END IF;
 IF EXISTS(WITH participants AS(SELECT session_id,booked_member_id AS member_id FROM sessions WHERE booked_member_id IS NOT NULL UNION SELECT s.session_id,e.member_id FROM sessions s JOIN class_enrollments e ON e.class_id=s.class_id WHERE e.status='Registered')
  SELECT 1 FROM participants a JOIN participants b ON a.member_id=b.member_id AND a.session_id<b.session_id JOIN sessions x ON x.session_id=a.session_id JOIN sessions y ON y.session_id=b.session_id WHERE x.session_date=y.session_date AND x.start_time<y.end_time AND y.start_time<x.end_time AND x.status<>'Cancelled' AND y.status<>'Cancelled' AND NOT x.center_invalidated AND NOT y.center_invalidated) THEN RAISE EXCEPTION 'Member overlap in reference schedule'; END IF;
 IF EXISTS(SELECT 1 FROM sessions s JOIN rooms r ON r.room_id=s.room_id JOIN classes c ON c.class_id=s.class_id JOIN subjects su ON su.subject_id=c.subject_id WHERE c.max_capacity>r.capacity OR s.status<>'Cancelled' AND NOT s.center_invalidated AND (EXISTS(SELECT 1 FROM holidays h WHERE h.holiday_date=s.session_date AND h.is_active) OR su.outdoor AND s.start_time IN(TIME '09:30',TIME '13:00'))) THEN RAISE EXCEPTION 'Capacity/holiday/outdoor rule mismatch'; END IF;
 IF EXISTS(SELECT 1 FROM sessions s JOIN member_packages p ON p.subscription_id=s.subscription_id JOIN classes c ON c.class_id=s.class_id WHERE s.booked_member_id<>p.member_id OR c.subject_id<>p.subject_id OR s.coach_id<>p.primary_coach_id OR s.status<>'Cancelled' AND s.session_date+s.end_time>p.expires_at) THEN RAISE EXCEPTION 'Private session owner/subject/Coach/deadline mismatch'; END IF;
 IF EXISTS(SELECT 1 FROM attendances a JOIN training_results t ON t.session_id=a.session_id AND t.member_id=a.member_id JOIN sessions s ON s.session_id=a.session_id WHERE a.state<>t.attendance_status OR t.recorded_at<s.session_date+s.start_time OR t.recorded_at>=s.session_date+s.end_time OR s.center_invalidated OR a.recorded_by<>s.coach_id) THEN RAISE EXCEPTION 'Attendance/result history is inconsistent'; END IF;
 FOR seq_row IN SELECT table_name,column_name FROM information_schema.columns WHERE table_schema='public' AND column_default LIKE 'nextval(%' AND table_name=ANY(ARRAY['roles','permissions','role_permissions','centers','users','email_otps','center_managers','receptionists','coaches','members','subjects','specializations','coach_specializations','rooms','classes','training_plans','sessions','training_results','attendances','evaluations','class_feedbacks','membership_packages','member_packages','class_enrollments','holidays','invoices','support_requests','notifications','notification_recipients','chat_conversations','chat_messages','chat_read_state','ai_consultations','system_logs','normal_class_requests','subscription_pauses','coach_work_schedules','coach_work_slots','coach_days_off','advanced_registrations','session_absence_notices','session_changes','center_incidents','incident_affected_members','payment_resolutions','scms_data_migration_audit']) LOOP
  EXECUTE format('SELECT count(*),min(%I),max(%I) FROM public.%I',seq_row.column_name,seq_row.column_name,seq_row.table_name) INTO n,lo,hi;
  IF n>0 AND (lo<>1 OR hi<>n) THEN RAISE EXCEPTION 'Rebuilt integer ID sequence is not contiguous: %.%',seq_row.table_name,seq_row.column_name; END IF;
 END LOOP;
END $reference_checks$;
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

-- Validate every FK in these SCMS tables, including unvalidated preexisting FKs.
DO $foreign_keys$
DECLARE r RECORD;
BEGIN
 FOR r IN SELECT conrelid::regclass AS tbl,conname FROM pg_constraint WHERE contype='f' AND NOT convalidated
  AND conrelid IN(SELECT oid FROM pg_class WHERE relnamespace='public'::regnamespace AND relname=ANY(ARRAY['roles','permissions','role_permissions','centers','users','email_otps','center_managers','receptionists','coaches','members','subjects','specializations','coach_specializations','rooms','classes','training_plans','sessions','training_results','attendances','evaluations','class_feedbacks','membership_packages','member_packages','class_enrollments','holidays','invoices','support_requests','notifications','notification_recipients','chat_conversations','chat_messages','chat_read_state','ai_consultations','system_logs','normal_class_requests','subscription_pauses','coach_work_schedules','coach_work_slots','coach_days_off','advanced_registrations','session_absence_notices','session_changes','center_incidents','incident_affected_members','payment_resolutions','scms_data_migration_audit'])) LOOP
  EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I',r.tbl,r.conname);
 END LOOP;
 IF (SELECT count(*) FROM subjects)<>9 THEN RAISE EXCEPTION 'Expected exactly nine canonical subjects'; END IF;
 IF (current_setting('scms.rebuild',true)='true' OR NOT EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261029_catalog_accounts_v1')) AND EXISTS(SELECT 1 FROM _scms_accounts a LEFT JOIN users u ON lower(u.email)=a.email LEFT JOIN roles role_row ON role_row.role_id=u.role_id WHERE u.user_id IS NULL OR u.status<>'Active' OR role_row.role_name<>a.role_name OR (a.role_name='Admin' AND u.center_id IS NOT NULL) OR (a.role_name<>'Admin' AND u.center_id IS DISTINCT FROM 1)) THEN RAISE EXCEPTION 'PDF account/role/center postflight failed'; END IF;
END $foreign_keys$;

-- Synchronize every owned SERIAL/IDENTITY sequence, including tables with zero rows.
-- Empty table => next ID 1. Nonempty table => next ID MAX(id)+1 (no per-role renumbering).
DO $sequences$
DECLARE r RECORD; top_id BIGINT; seq_name TEXT; seq_last BIGINT; seq_called BOOLEAN;
BEGIN
 FOR r IN SELECT table_name,column_name FROM information_schema.columns
          WHERE table_schema='public' AND table_name=ANY(ARRAY['roles','permissions','role_permissions','centers','users','email_otps','center_managers','receptionists','coaches','members','subjects','specializations','coach_specializations','rooms','classes','training_plans','sessions','training_results','attendances','evaluations','class_feedbacks','membership_packages','member_packages','class_enrollments','holidays','invoices','support_requests','notifications','notification_recipients','chat_conversations','chat_messages','chat_read_state','ai_consultations','system_logs','normal_class_requests','subscription_pauses','coach_work_schedules','coach_work_slots','coach_days_off','advanced_registrations','session_absence_notices','session_changes','center_incidents','incident_affected_members','payment_resolutions','scms_data_migration_audit'])
            AND (column_default LIKE 'nextval(%' OR is_identity='YES')
          ORDER BY table_name,column_name LOOP
  seq_name:=pg_get_serial_sequence(format('public.%I',r.table_name),r.column_name);
  IF seq_name IS NOT NULL THEN
   EXECUTE format('SELECT max(%I) FROM public.%I',r.column_name,r.table_name) INTO top_id;
   EXECUTE format('SELECT last_value,is_called FROM %s',seq_name) INTO seq_last,seq_called;
   IF current_setting('scms.rebuild',true)='true' OR top_id IS NOT NULL AND (seq_last<top_id OR seq_last=top_id AND NOT seq_called) THEN
    PERFORM setval(seq_name,coalesce(top_id,1),top_id IS NOT NULL);
   END IF;
  END IF;
 END LOOP;
END $sequences$;

COMMIT;
-- Last statement deliberately returns the review result AFTER a successful commit.
-- Demo seed password is SWP391; no passwords/hashes are returned by this review query.
SELECT 'COMMITTED' AS result,u.user_id,r.role_name,u.full_name,u.email,u.status,u.center_id,
       u.is_first_login,
       (SELECT count(*) FROM subjects) AS subject_count,
       (SELECT count(*) FROM rooms WHERE status='Available' AND center_id=1) AS available_room_count
FROM users u JOIN roles r ON r.role_id=u.role_id
WHERE lower(u.email) IN('admin@gmail.com','an@gmail.com','linh@gmail.com','huy@gmail.com','mai@gmail.com','khang@gmail.com','vy@gmail.com','bao@gmail.com','nhi@gmail.com','kiet@gmail.com','ha@gmail.com','coach.tennis1@gmail.com','coach.tennis2@gmail.com','coach.swimming1@gmail.com','coach.swimming2@gmail.com','coach.pickleball1@gmail.com','coach.pickleball2@gmail.com','coach.archery1@gmail.com','coach.archery2@gmail.com','coach.basketball1@gmail.com','coach.basketball2@gmail.com','coach.football1@gmail.com','coach.football2@gmail.com','coach.volleyball1@gmail.com','coach.volleyball2@gmail.com','coach.badminton1@gmail.com','coach.badminton2@gmail.com','coach.tabletennis1@gmail.com','coach.tabletennis2@gmail.com','receptionist1@gmail.com','receptionist2@gmail.com','manager1@gmail.com','manager2@gmail.com')
ORDER BY u.user_id;
