-- Production structure, roles/permissions, default center and nine approved subjects only.
-- No accounts, Paid invoices, active subscriptions, sessions or demo transactions.
-- Empty public schema required. Existing databases must use reviewed incremental migrations.
BEGIN;
DO $empty$
BEGIN
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE') THEN
        RAISE EXCEPTION 'SCMS bootstrap requires an empty public schema; use reviewed incremental migrations';
    END IF;
END $empty$;
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

COMMIT;
