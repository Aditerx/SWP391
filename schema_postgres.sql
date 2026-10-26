-- schema_postgres.sql
-- PostgreSQL version of SQLQuery3_2609.sql (SQL Server original kept as-is).
-- Run this in Supabase SQL Editor (Project → SQL Editor → New query).
--
-- Changes from SQL Server:
--   INT IDENTITY(1,1)              → SERIAL
--   NVARCHAR(n) / NVARCHAR(MAX)   → VARCHAR(n) / TEXT
--   DATETIME                       → TIMESTAMP
--   GETDATE()                      → CURRENT_TIMESTAMP
--   CAST(GETDATE() AS DATE)        → CURRENT_DATE
--   BIT                            → BOOLEAN
--   N'...' string literals         → '...' (no N prefix)
--   [timestamp] reserved word     → "timestamp" (quoted)
--   GO                             → removed
--   CREATE OR ALTER TRIGGER        → CREATE OR REPLACE FUNCTION + trigger
--   DECLARE @var / SET @var        → standard SQL
--   HASHBYTES (SHA2_256)           → passwords inserted as BCrypt hash of '12345678'
--   THROW                          → RAISE EXCEPTION
--   IDENTITY INSERT / SET NOCOUNT  → not needed in PostgreSQL
--
-- Password for all demo accounts: Scms@2026
-- BCrypt hash used below: $2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi

-- =====================================================================
-- 1. ROLES – PERMISSIONS
-- =====================================================================

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
-- PostgreSQL/public schema only. Additive upgrade; never run the demo reset.
-- Run after 04 and 06. No historical subscription is inferred or backfilled.
BEGIN;
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
COMMIT;


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
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_noti_user   FOREIGN KEY (user_id)   REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_noti_sender FOREIGN KEY (sender_id) REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_noti_user ON notifications(user_id);

CREATE TABLE IF NOT EXISTS notification_recipients (
    notification_id INT NOT NULL,
    user_id         INT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP NULL,
    CONSTRAINT pk_notification_recipients PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_nr_notification FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_nr_user         FOREIGN KEY (user_id)         REFERENCES users(user_id)
);

CREATE INDEX IF NOT EXISTS idx_nr_user ON notification_recipients(user_id);

-- =====================================================================
-- 10. AI CONSULTATIONS
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
    ('Coach', 'MANAGE_TRAINING_PLAN'),
    ('Coach', 'RECORD_RESULT'),
    ('Receptionist', 'RECORD_RESULT'),
    ('Receptionist', 'PROCESS_PAYMENT'),
    ('Receptionist', 'HANDLE_SUPPORT'),
    ('Receptionist', 'REGISTER_MEMBER'),
    ('Receptionist', 'MANAGE_SUBSCRIPTIONS'),
    ('Receptionist', 'MANAGE_INVOICES'),
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

-- =====================================================================
-- SEED DATA (Chuẩn hóa toàn diện SCMS - Demo & Production Ready)
-- Mật khẩu mặc định toàn hệ thống: Scms@2026
-- BCrypt hash (Cost = 10): $2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi
-- =====================================================================

-- Users (28 tài khoản: 5 Role đại diện + 18 HLV 9 môn + 5 Hội viên theo kịch bản)
INSERT INTO users (user_id, role_id, full_name, email, phone, password_hash, address, gender, date_of_birth, status, center_id) VALUES
-- 1. Tài khoản đại diện cốt lõi (5 Roles)
(1,  (SELECT role_id FROM roles WHERE role_name = 'CenterManager'), 'Nguyễn Văn An',          'manager@scms.com',          '0901234501', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '12 Nguyễn Huệ, Quận 1, TP.HCM',          'Male',   '1985-03-12', 'Active', 1),
(2,  (SELECT role_id FROM roles WHERE role_name = 'Receptionist'),  'Phạm Thị Thu',           'receptionist@scms.com',     '0901234502', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '45 Lê Lợi, Quận 1, TP.HCM',              'Female', '1998-04-22', 'Active', 1),
(3,  (SELECT role_id FROM roles WHERE role_name = 'Coach'),         'Nguyễn Văn Hùng',        'coach@scms.com',            '0901234503', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '78 CMT8, Quận 3, TP.HCM',                'Male',   '1990-05-15', 'Active', 1),
(4,  (SELECT role_id FROM roles WHERE role_name = 'Member'),        'Nguyễn Văn Thành',       'member@scms.com',           '0901234504', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '23 NTM Khai, Quận 1, TP.HCM',            'Male',   '1995-08-20', 'Active', 1),
(5,  (SELECT role_id FROM roles WHERE role_name = 'Admin'),         'Admin Hệ Thống',         'admin@scms.com',            '0901234500', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Văn phòng Trung tâm, Q1, TP.HCM',        'Other',  '1980-01-01', 'Active', 1),

-- 2. Lễ tân phụ trợ (Khớp mock frontend thu.reception@scms.com)
(6,  (SELECT role_id FROM roles WHERE role_name = 'Receptionist'),  'Phạm Thị Thu (Ca Chiều)','thu.reception@scms.com',   '0901234506', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '56 Điện Biên Phủ, Bình Thạnh, TP.HCM',  'Female', '1998-04-22', 'Active', 1),

-- 3. Đội ngũ HLV cho 9 bộ môn chuẩn (Mỗi môn 2 HLV: coach.<sport>@scms.com & coach.<sport>2@scms.com)
-- Môn 1: Bóng đá (Football - Outdoor)
(7,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Trần Phi',           'coach.football@scms.com',   '0901234507', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân bóng đá trung tâm',                 'Male',   '1988-11-20', 'Active', 1),
(8,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Tuấn Vũ',        'coach.football2@scms.com',  '0901234508', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân bóng đá trung tâm',                 'Male',   '1992-06-14', 'Active', 1),

-- Môn 2: Quần vợt (Tennis - Outdoor)
(9,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Huỳnh Diễm My',         'coach.tennis@scms.com',     '0901234509', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu sân Tennis ngoài trời',              'Female', '1993-09-12', 'Active', 1),
(10, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Quốc Bảo',         'coach.tennis2@scms.com',    '0901234510', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu sân Tennis ngoài trời',              'Male',   '1989-03-25', 'Active', 1),

-- Môn 3: Bơi lội (Swimming - Outdoor)
(11, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Phương Anh',         'coach.swimming@scms.com',   '0901234511', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu thể thao dưới nước',                'Female', '1995-07-08', 'Active', 1),
(12, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Đỗ Hải Đăng',           'coach.swimming2@scms.com',  '0901234512', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu thể thao dưới nước',                'Male',   '1991-12-30', 'Active', 1),

-- Môn 4: Bắn cung (Archery - Outdoor)
(13, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Nguyễn Thiên Phúc',  'coach.archery@scms.com',    '0901234513', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Trường bắn ngoài trời',                  'Male',   '1994-04-18', 'Active', 1),
(14, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Hoàng Minh Tâm',        'coach.archery2@scms.com',   '0901234514', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Trường bắn ngoài trời',                  'Male',   '1990-10-05', 'Active', 1),

-- Môn 5: Cầu lông (Badminton - Indoor)
(15, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Thị Hương',        'coach.badminton@scms.com',  '0901234515', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu Cầu lông',                  'Female', '1992-07-20', 'Active', 1),
(16, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Minh Khang',         'coach.badminton2@scms.com', '0901234516', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu Cầu lông',                  'Male',   '1996-01-22', 'Active', 1),

-- Môn 6: Bóng bàn (Table Tennis - Indoor)
(17, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Minh Khoa',          'coach.tabletennis@scms.com','0901234517', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Phòng Bóng bàn tiêu chuẩn',              'Male',   '1990-01-15', 'Active', 1),
(18, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Đức Trí',        'coach.tabletennis2@scms.com','0901234518','$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Phòng Bóng bàn tiêu chuẩn',              'Male',   '1993-08-19', 'Active', 1),

-- Môn 7: Bóng rổ (Basketball - Indoor)
(19, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Anh Dũng',         'coach.basketball@scms.com', '0901234519', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1987-05-11', 'Active', 1),
(20, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Phan Hoàng Nam',        'coach.basketball2@scms.com','0901234520', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1994-09-02', 'Active', 1),

-- Môn 8: Bóng chuyền (Volleyball - Indoor)
(21, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Văn Hoàng',      'coach.volleyball@scms.com', '0901234521', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1989-12-10', 'Active', 1),
(22, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Đặng Tuấn Anh',         'coach.volleyball2@scms.com','0901234522', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1995-03-17', 'Active', 1),

-- Môn 9: Pickleball (Indoor)
(23, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Phan Thị Lan',          'coach.pickleball@scms.com', '0901234523', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân Pickleball có mái che',              'Female', '1994-11-05', 'Active', 1),
(24, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Vũ Thị Thảo',           'coach.pickleball2@scms.com','0901234524', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân Pickleball có mái che',              'Female', '1997-04-28', 'Active', 1),

-- 4. Hội viên đại diện theo các kịch bản demo
(25, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Hoàng Thị Oanh',        'member.normal@scms.com',    '0901234525', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '12 Trần Hưng Đạo, Q5, TP.HCM',       'Female', '1996-06-18', 'Active', 1),
(26, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Bùi Văn Phúc',          'member.new@scms.com',       '0901234526', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '34 Nguyễn Trãi, Q5, TP.HCM',         'Male',   '1993-02-27', 'Active', 1),
(27, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Đặng Văn Sơn',          'member.expired@scms.com',   '0901234527', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '90 Ba Tháng Hai, Q10, TP.HCM',       'Male',   '1988-08-08', 'Active', 1),
(28, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Ngô Thị Quyên',         'member.active2@scms.com',   '0901234528', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '67 Lý Thường Kiệt, Q10, TP.HCM',     'Female', '1999-12-01', 'Active', 1)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

SELECT setval(pg_get_serial_sequence('users', 'user_id'),
              GREATEST((SELECT COALESCE(MAX(user_id), 1) FROM users), 1));

-- Subtypes phân quyền
INSERT INTO center_managers (user_id) VALUES (1) ON CONFLICT (user_id) DO NOTHING;
INSERT INTO receptionists   (user_id) VALUES (2), (6) ON CONFLICT (user_id) DO NOTHING;

INSERT INTO coaches (user_id, specialization, certification, bio) VALUES
(3,  'Fitness, Thể hình, Cầu lông', 'NASM-CPT',         'Hơn 8 năm kinh nghiệm huấn luyện thể hình và thể lực tổng quát.'),
(7,  'Bóng đá',                     'AFC Pro Diploma',  'Cựu trợ lý HLV CLB V-League, chuyên chiến thuật và thể lực bóng đá.'),
(8,  'Bóng đá',                     'AFC B Certificate','Cựu cầu thủ chuyên nghiệp, đào tạo bóng đá trẻ và kỹ năng căn bản.'),
(9,  'Quần vợt (Tennis)',           'ITF Level 2',      'Hơn 6 năm đào tạo quần vợt phong trào và bán chuyên.'),
(10, 'Quần vợt (Tennis)',           'ITF Level 1',      'Vận động viên thi đấu quốc gia, kỹ thuật giao bóng và bộ chân hiện đại.'),
(11, 'Bơi lội',                     'FINA Coach L2',    'Kiện tướng bơi lội, chuyên kỹ thuật 4 kiểu bơi và bơi sinh tồn.'),
(12, 'Bơi lội',                     'Cứu hộ & Bơi L1',  'Kinh nghiệm 5 năm dạy bơi trẻ em và người lớn sợ nước.'),
(13, 'Bắn cung',                    'Kiện tướng QG',    'Vận động viên bắn cung quốc gia, chuyên cung 1 dây và cung 3 dây.'),
(14, 'Bắn cung',                    'HLV Cấp 1',        'Đào tạo tư thế căn bản, rèn luyện sự tập trung và kiểm soát hơi thở.'),
(15, 'Cầu lông',                    'BWF Level 2',      'Hơn 7 năm giảng dạy cầu lông, đào tạo phản xạ và kỹ thuật đập cầu.'),
(16, 'Cầu lông',                    'BWF Level 1',      'Cựu VĐV đội tuyển trẻ TP.HCM, phong cách năng động nhiệt tình.'),
(17, 'Bóng bàn',                    'ITTF Level 2',     'Chuyên kỹ thuật xoáy và phản xạ nhịp độ cao trong bóng bàn hiện đại.'),
(18, 'Bóng bàn',                    'Kiện tướng BB',    'Kinh nghiệm huấn luyện đội tuyển trường đại học.'),
(19, 'Bóng rổ',                     'FIBA Coach L1',    'Đào tạo ném rổ, chiến thuật chạy bài 5x5 và 3x3.'),
(20, 'Bóng rổ',                     'VBA Veteran',      'Cựu VĐV giải bóng rổ chuyên nghiệp VBA.'),
(21, 'Bóng chuyền',                 'FIVB Level 1',     'Chuyên kỹ thuật đệm bóng, phát bóng tấn công và chắn bóng trên lưới.'),
(22, 'Bóng chuyền',                 'HLV Thể lực BC',   'Tập trung phát triển sức bật và phối hợp chiến thuật đồng đội.'),
(23, 'Pickleball',                  'IPTPA Level 2',    'Huấn luyện viên Pickleball chuẩn quốc tế, kỹ thuật dink và third shot drop.'),
(24, 'Pickleball',                  'PPR Certified',    'Đào tạo luật chơi, chiến thuật đôi và phản xạ lưới Pickleball.')
ON CONFLICT (user_id) DO UPDATE SET
    specialization = EXCLUDED.specialization,
    certification  = EXCLUDED.certification,
    bio            = EXCLUDED.bio;

INSERT INTO specializations(name, status)
SELECT DISTINCT trim(value), 'Active'
FROM coaches c
CROSS JOIN LATERAL regexp_split_to_table(c.specialization, '[,;]') AS value
WHERE c.specialization IS NOT NULL AND trim(value) <> ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO coach_specializations(coach_id, specialization_id)
SELECT c.user_id, s.specialization_id
FROM coaches c
CROSS JOIN LATERAL regexp_split_to_table(c.specialization, '[,;]') AS value
JOIN specializations s ON lower(s.name) = lower(trim(value))
WHERE c.specialization IS NOT NULL AND trim(value) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO members (user_id, goal, health_note, join_date, registered_by) VALUES
(4,  'Tập luyện nâng cao 1:1, cải thiện thể lực toàn diện', NULL, '2026-08-01', 2),
(25, 'Rèn luyện phản xạ, giảm căng thẳng qua môn Cầu lông', NULL, '2026-08-15', 2),
(26, 'Làm quen môi trường thể thao, đăng ký môn mới', NULL, CURRENT_DATE, 2),
(27, 'Hồi phục thể lực sau chấn thương nhẹ', 'Cần khởi động kỹ khớp gối', '2026-06-01', 2),
(28, 'Yêu thích Pickleball, muốn thi đấu phong trào', NULL, '2026-09-01', 6)
ON CONFLICT (user_id) DO UPDATE SET
    goal = EXCLUDED.goal,
    health_note = EXCLUDED.health_note;

-- Bảng giá chuẩn 9 môn thể thao Member.pdf & 01-API-CONTRACT-FE-BE.md
INSERT INTO subjects (subject_name, description, normal_price, advanced_price, status, slug) VALUES
('Bóng đá',            'Môn thể thao vua ngoài trời, rèn luyện thể lực và phối hợp',         900000,  8000000, 'Active', 'bong-da'),
('Quần vợt (Tennis)',  'Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực',             1500000, 10000000, 'Active', 'quan-vot-tennis'),
('Bơi lội',            'Môn thể thao dưới nước ngoài trời, phát triển toàn diện cơ thể',     1200000,  9000000, 'Active', 'boi-loi'),
('Bắn cung',           'Môn ngoài trời, rèn luyện sự tập trung cao độ và tính kiên nhẫn',   1400000,  9500000, 'Active', 'ban-cung'),
('Cầu lông',           'Môn thể thao nhanh nhẹn, linh hoạt trong nhà',                        900000,  7500000, 'Active', 'cau-long'),
('Bóng bàn',           'Phản xạ nhanh, khéo léo với nhịp độ cao trong nhà',                   800000,  6500000, 'Active', 'bong-ban'),
('Bóng rổ',            'Môn thể thao đồng đội tốc độ và sức bật trong nhà',                  1000000,  8000000, 'Active', 'bong-ro'),
('Bóng chuyền',        'Phối hợp đồng đội và kỹ thuật phản xạ trên lưới trong nhà',          900000,  7000000, 'Active', 'bong-chuyen'),
('Pickleball',         'Môn thể thao thời thượng trong nhà, kết hợp tennis, bóng bàn',       1100000,  8500000, 'Active', 'pickleball')
ON CONFLICT (slug) DO UPDATE SET
    subject_name = EXCLUDED.subject_name,
    normal_price = EXCLUDED.normal_price,
    advanced_price = EXCLUDED.advanced_price,
    description = EXCLUDED.description,
    status = EXCLUDED.status;

INSERT INTO rooms (room_name, location, capacity, status, facility_type, center_id) VALUES
('Sân cỏ ngoài trời (Bóng đá)',    'Khu sân bóng đá cỏ nhân tạo',   30, 'Available', 'Outdoor', 1),
('Sân Tennis ngoài trời 1',        'Khu thể thao ngoài trời',        4, 'Available', 'Outdoor', 1),
('Hồ bơi ngoài trời tiêu chuẩn',   'Khu thể thao dưới nước',        20, 'Available', 'Outdoor', 1),
('Trường bắn ngoài trời',          'Khu vực an toàn ngoài trời',    10, 'Available', 'Outdoor', 1),
('Nhà thi đấu Cầu lông (Sàn thảm)','Tầng 1 Nhà thi đấu',            20, 'Available', 'Indoor',  1),
('Phòng Bóng bàn tiêu chuẩn',      'Tầng 2 Khu trong nhà',          15, 'Available', 'Indoor',  1),
('Nhà thi đấu đa năng (Bóng rổ & Chuyền)', 'Tầng 1 Nhà thi đấu lớn', 30, 'Available', 'Indoor',  1),
('Sân Pickleball có mái che',      'Khu liên hợp thể thao',         12, 'Available', 'Indoor',  1)
ON CONFLICT DO NOTHING;

-- Gói hội viên
INSERT INTO membership_packages (package_id, package_name, price, duration_days, benefits, package_type, total_credit) VALUES
(1, 'Gói Cơ Bản 1 Tháng',                   500000,   30,  'Tập tự do không giới hạn khung giờ, không kèm HLV', 'Normal', 12),
(2, 'Gói Tiêu Chuẩn 3 Tháng',               1350000,  90,  'Tập tự do + tham gia tối đa 2 lớp nhóm/tuần', 'Normal', 12),
(3, 'Gói Cao Cấp 6 Tháng',                  2500000,  180, 'Toàn quyền sử dụng tiện ích + tặng 2 buổi PT riêng/tháng', 'Normal', 12),
(4, 'Gói VIP 12 Tháng',                     4800000,  365, 'Thẻ hội viên VIP 1 năm toàn hệ thống SCMS', 'Normal', 12),
(5, 'Gói Khóa Học 1:1 (12 Buổi Linh Hoạt)', 3500000,  45,  '12 Credit đặt lịch linh hoạt 1:1 với HLV trong 45 ngày', 'Advanced', 12)
ON CONFLICT (package_id) DO UPDATE SET
    package_name = EXCLUDED.package_name,
    price = EXCLUDED.price,
    duration_days = EXCLUDED.duration_days,
    package_type = EXCLUDED.package_type,
    total_credit = EXCLUDED.total_credit;

SELECT setval(pg_get_serial_sequence('membership_packages', 'package_id'),
              GREATEST((SELECT COALESCE(MAX(package_id), 1) FROM membership_packages), 1));

INSERT INTO member_packages (member_id, package_id, start_date, end_date, remaining_credit, validity_days, status) VALUES
(4,  5, CURRENT_DATE, CURRENT_DATE + INTERVAL '45 days', 12, 45, 'Active'),
(27, 5, CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE - INTERVAL '15 days', 0, 45, 'Expired'),
(28, 2, '2026-09-01', '2026-11-30', 0, 90, 'Active')
ON CONFLICT DO NOTHING;

-- Lớp học mẫu
ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;

INSERT INTO classes (class_name, subject_id, coach_id, max_capacity, tuition_fee, start_date, end_date, status, class_type, total_sessions, center_id)
SELECT 'Lớp Cầu Lông Cơ Bản (K1)', s.subject_id, 15, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Ongoing', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'cau-long'
UNION ALL
SELECT 'Lớp Quần Vợt Phong Trào', s.subject_id, 9, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Ongoing', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'quan-vot-tennis'
UNION ALL
SELECT 'Lớp Bơi Lội Căn Bản', s.subject_id, 11, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Open', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'boi-loi'
UNION ALL
SELECT 'Lớp Pickleball Nhập Môn', s.subject_id, 23, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Ongoing', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'pickleball'
UNION ALL
SELECT 'Lớp Bóng Đá Căn Bản', s.subject_id, 7, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Open', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'bong-da'
UNION ALL
SELECT 'Lớp Bóng Bàn Căn Bản', s.subject_id, 17, 15, s.normal_price, CURRENT_DATE, CURRENT_DATE + INTERVAL '40 days', 'Open', 'Normal', 12, 1
FROM subjects s WHERE s.slug = 'bong-ban'
ON CONFLICT DO NOTHING;

ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;

-- Ghi danh học viên
INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 25, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 28, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Pickleball Nhập Môn'
ON CONFLICT DO NOTHING;

-- Buổi học mẫu
ALTER TABLE sessions DISABLE TRIGGER trg_sessions_validate;

INSERT INTO sessions (class_id, room_id, session_date, start_time, end_time, status)
SELECT c.class_id, r.room_id, CURRENT_DATE + INTERVAL '1 day', '07:45:00', '09:15:00', 'Scheduled'
FROM classes c, rooms r
WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)' AND r.room_name = 'Nhà thi đấu Cầu lông (Sàn thảm)'
LIMIT 1;

INSERT INTO sessions (class_id, room_id, session_date, start_time, end_time, status)
SELECT c.class_id, r.room_id, CURRENT_DATE + INTERVAL '3 days', '07:45:00', '09:15:00', 'Scheduled'
FROM classes c, rooms r
WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)' AND r.room_name = 'Nhà thi đấu Cầu lông (Sàn thảm)'
LIMIT 1;

ALTER TABLE sessions ENABLE TRIGGER trg_sessions_validate;

-- Hóa đơn mẫu
INSERT INTO invoices (member_id, package_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref) VALUES
(4,  5, 2, 3500000, 'VNPay',        'Paid',    CURRENT_TIMESTAMP, 'VNPAY-TXN-20261001'),
(28, 2, 2, 1350000, 'BankTransfer', 'Paid',    CURRENT_TIMESTAMP - INTERVAL '15 days', 'BANK-TXN-20261002'),
(26, 1, 2, 500000,  NULL,           'Pending', NULL,              NULL)
ON CONFLICT DO NOTHING;

INSERT INTO invoices (member_id, class_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref)
SELECT 25, c.class_id, 2, c.tuition_fee, 'Cash', 'Paid', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

-- Đồng bộ sequence
SELECT setval(pg_get_serial_sequence('classes', 'class_id'), GREATEST((SELECT COALESCE(MAX(class_id), 1) FROM classes), 1));
SELECT setval(pg_get_serial_sequence('sessions', 'session_id'), GREATEST((SELECT COALESCE(MAX(session_id), 1) FROM sessions), 1));
SELECT setval(pg_get_serial_sequence('invoices', 'invoice_id'), GREATEST((SELECT COALESCE(MAX(invoice_id), 1) FROM invoices), 1));
SELECT setval(pg_get_serial_sequence('member_packages', 'subscription_id'), GREATEST((SELECT COALESCE(MAX(subscription_id), 1) FROM member_packages), 1));


-- =====================================================================
-- 13. HOLIDAYS SEED DATA (DANH MỤC NGÀY NGHỈ LỄ/TẾT 2026 - 2027)
-- =====================================================================

INSERT INTO holidays (holiday_date, holiday_name, tag, is_active, description) VALUES
-- Ngày lễ dương lịch cố định (NATIONAL_FIXED) năm 2026 & 2027
('2026-01-01', 'Tết Dương Lịch 2026', 'NATIONAL_FIXED', TRUE, 'Nghỉ Tết Dương Lịch toàn quốc'),
('2026-04-30', 'Ngày Giải phóng miền Nam 2026', 'NATIONAL_FIXED', TRUE, 'Kỷ niệm ngày thống nhất đất nước'),
('2026-05-01', 'Ngày Quốc tế Lao động 2026', 'NATIONAL_FIXED', TRUE, 'Ngày Quốc tế Lao động'),
('2026-09-02', 'Ngày Quốc khánh 2026', 'NATIONAL_FIXED', TRUE, 'Kỷ niệm Quốc khánh Việt Nam'),
('2026-09-03', 'Nghỉ liền kề Quốc khánh 2026', 'NATIONAL_FIXED', TRUE, 'Ngày nghỉ liền kề quy định'),
('2027-01-01', 'Tết Dương Lịch 2027', 'NATIONAL_FIXED', TRUE, 'Nghỉ Tết Dương Lịch toàn quốc'),
('2027-04-30', 'Ngày Giải phóng miền Nam 2027', 'NATIONAL_FIXED', TRUE, 'Kỷ niệm ngày thống nhất đất nước'),
('2027-05-01', 'Ngày Quốc tế Lao động 2027', 'NATIONAL_FIXED', TRUE, 'Ngày Quốc tế Lao động'),
('2027-09-02', 'Ngày Quốc khánh 2027', 'NATIONAL_FIXED', TRUE, 'Kỷ niệm Quốc khánh Việt Nam'),
('2027-09-03', 'Nghỉ liền kề Quốc khánh 2027', 'NATIONAL_FIXED', TRUE, 'Ngày nghỉ liền kề quy định'),

-- Lễ hội âm lịch (LUNAR_FESTIVAL) năm 2026 & 2027
('2026-04-26', 'Giỗ Tổ Hùng Vương 2026 (10/3 AL)', 'LUNAR_FESTIVAL', TRUE, 'Ngày Giỗ Tổ truyền thống'),
('2026-02-15', 'Tết Nguyên Đán Bính Ngọ (28 Tết)', 'LUNAR_FESTIVAL', TRUE, 'Chuỗi nghỉ Tết Âm Lịch'),
('2026-02-16', 'Tết Nguyên Đán Bính Ngọ (29 Tết)', 'LUNAR_FESTIVAL', TRUE, 'Chuỗi nghỉ Tết Âm Lịch'),
('2026-02-17', 'Tết Nguyên Đán Bính Ngọ (Mùng 1)', 'LUNAR_FESTIVAL', TRUE, 'Tết Nguyên Đán'),
('2026-02-18', 'Tết Nguyên Đán Bính Ngọ (Mùng 2)', 'LUNAR_FESTIVAL', TRUE, 'Tết Nguyên Đán'),
('2026-02-19', 'Tết Nguyên Đán Bính Ngọ (Mùng 3)', 'LUNAR_FESTIVAL', TRUE, 'Tết Nguyên Đán'),
('2026-02-20', 'Tết Nguyên Đán Bính Ngọ (Mùng 4)', 'LUNAR_FESTIVAL', TRUE, 'Chuỗi nghỉ Tết Âm Lịch'),
('2026-02-21', 'Tết Nguyên Đán Bính Ngọ (Mùng 5)', 'LUNAR_FESTIVAL', TRUE, 'Chuỗi nghỉ Tết Âm Lịch')
ON CONFLICT (holiday_date) DO UPDATE SET
    holiday_name = EXCLUDED.holiday_name,
    tag = EXCLUDED.tag,
    is_active = EXCLUDED.is_active;

-- =====================================================================
-- 14. TEMPLATE LỚP HỌC 1:1 CHO CÁC BỘ MÔN (ADVANCED CLASSES)
-- =====================================================================

INSERT INTO classes (class_name, subject_id, coach_id, max_capacity, tuition_fee, status, center_id, class_type, total_sessions)
SELECT 
    s.subject_name || ' (1:1 Nâng Cao)',
    s.subject_id,
    NULL,
    1,
    s.advanced_price,
    'Open',
    1,
    'Advanced',
    12
FROM subjects s
WHERE s.slug IN (
    'quan-vot-tennis', 'boi-loi', 'pickleball', 'ban-cung', 
    'bong-ro', 'bong-da', 'bong-chuyen', 'cau-long', 'bong-ban'
)
AND NOT EXISTS (
    SELECT 1 FROM classes c 
    WHERE c.subject_id = s.subject_id AND c.class_type = 'Advanced'
);
