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
-- Password for all demo accounts: 12345678
-- BCrypt hash used below: $2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi

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
        CHECK (status IN ('Active', 'Inactive', 'Locked')),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role_id);

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
    CONSTRAINT fk_member_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_receptionist
        FOREIGN KEY (registered_by) REFERENCES receptionists(user_id)
);

-- =====================================================================
-- 3. SUBJECTS – ROOMS – CLASSES
-- =====================================================================

CREATE TABLE IF NOT EXISTS subjects (
    subject_id   SERIAL PRIMARY KEY,
    subject_name VARCHAR(100) NOT NULL,
    description  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id   SERIAL PRIMARY KEY,
    room_name VARCHAR(50)  NOT NULL,
    location  VARCHAR(100),
    capacity  INT,
    status    VARCHAR(20)  NOT NULL DEFAULT 'Available'
        CHECK (status IN ('Available', 'Maintenance', 'Closed')),
    CONSTRAINT chk_room_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE IF NOT EXISTS classes (
    class_id     SERIAL PRIMARY KEY,
    class_name   VARCHAR(100) NOT NULL,
    subject_id   INT NOT NULL,
    coach_id     INT NULL,
    max_capacity INT,
    start_date   DATE,
    end_date     DATE,
    status       VARCHAR(20) NOT NULL DEFAULT 'Open'
        CHECK (status IN ('Open', 'Ongoing', 'Closed', 'Cancelled')),
    CONSTRAINT chk_class_capacity CHECK (max_capacity IS NULL OR max_capacity > 0),
    CONSTRAINT chk_class_dates    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT fk_class_subject   FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
    CONSTRAINT fk_class_coach     FOREIGN KEY (coach_id)   REFERENCES coaches(user_id)
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
    session_id   SERIAL PRIMARY KEY,
    class_id     INT NOT NULL,
    room_id      INT NOT NULL,
    session_date DATE NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'Scheduled'
        CHECK (status IN ('Scheduled', 'Completed', 'Cancelled')),
    CONSTRAINT chk_session_time  CHECK (end_time > start_time),
    CONSTRAINT fk_session_class  FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_room   FOREIGN KEY (room_id)  REFERENCES rooms(room_id)
);

CREATE INDEX IF NOT EXISTS idx_session_class ON sessions(class_id);
CREATE INDEX IF NOT EXISTS idx_session_room  ON sessions(room_id);

CREATE TABLE IF NOT EXISTS training_results (
    result_id         SERIAL PRIMARY KEY,
    session_id        INT NOT NULL,
    member_id         INT NOT NULL,
    coach_id          INT NOT NULL,
    content           TEXT,
    attendance_status VARCHAR(10) NOT NULL DEFAULT 'Present'
        CHECK (attendance_status IN ('Present', 'Absent', 'Late')),
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
        CHECK (state IN ('Present', 'Absent', 'Late', 'CheckedIn')),
    check_in_time  TIMESTAMP NULL,
    check_out_time TIMESTAMP NULL,
    CONSTRAINT fk_attendance_session  FOREIGN KEY (session_id)  REFERENCES sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_member   FOREIGN KEY (member_id)   REFERENCES members(user_id),
    CONSTRAINT fk_attendance_recorder FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

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
-- 6. MEMBERSHIP PACKAGES + SUBSCRIPTIONS + CLASS ENROLLMENTS
-- =====================================================================

CREATE TABLE IF NOT EXISTS membership_packages (
    package_id    SERIAL PRIMARY KEY,
    package_name  VARCHAR(100)    NOT NULL,
    price         DECIMAL(12,2)   NOT NULL,
    duration_days INT             NOT NULL,
    benefits      TEXT,
    status        VARCHAR(20)     NOT NULL DEFAULT 'Active'
        CHECK (status IN ('Active', 'Inactive'))
);

CREATE TABLE IF NOT EXISTS member_packages (
    subscription_id SERIAL PRIMARY KEY,
    member_id       INT NOT NULL,
    package_id      INT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'Active'
        CHECK (status IN ('Active', 'Expired', 'Cancelled')),
    CONSTRAINT fk_mp_member  FOREIGN KEY (member_id)  REFERENCES members(user_id),
    CONSTRAINT fk_mp_package FOREIGN KEY (package_id) REFERENCES membership_packages(package_id)
);

CREATE INDEX IF NOT EXISTS idx_mp_member  ON member_packages(member_id);
CREATE INDEX IF NOT EXISTS idx_mp_package ON member_packages(package_id);

CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    member_id     INT NOT NULL,
    class_id      INT NOT NULL,
    enrolled_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(20) NOT NULL DEFAULT 'Registered'
        CHECK (status IN ('Registered', 'Cancelled', 'Completed')),
    CONSTRAINT fk_enroll_member FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_enroll_class  FOREIGN KEY (class_id)  REFERENCES classes(class_id),
    CONSTRAINT uq_enrollment UNIQUE (member_id, class_id)
);

-- =====================================================================
-- 7. INVOICES
-- =====================================================================

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id              SERIAL PRIMARY KEY,
    member_id               INT NOT NULL,
    package_id              INT NULL,
    receptionist_id         INT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    payment_method          VARCHAR(20)
        CHECK (payment_method IN ('Cash', 'BankTransfer', 'CreditCard', 'EWallet')),
    payment_status          VARCHAR(20) NOT NULL DEFAULT 'Pending'
        CHECK (payment_status IN ('Pending', 'Paid', 'Failed', 'Refunded')),
    payment_date            TIMESTAMP,
    gateway_transaction_ref VARCHAR(100),
    CONSTRAINT fk_invoice_member       FOREIGN KEY (member_id)       REFERENCES members(user_id),
    CONSTRAINT fk_invoice_package      FOREIGN KEY (package_id)      REFERENCES membership_packages(package_id),
    CONSTRAINT fk_invoice_receptionist FOREIGN KEY (receptionist_id) REFERENCES receptionists(user_id)
);

CREATE INDEX IF NOT EXISTS idx_invoice_member       ON invoices(member_id);
CREATE INDEX IF NOT EXISTS idx_invoice_receptionist ON invoices(receptionist_id);

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
    IF NEW.status = 'Open' AND NEW.start_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'A new Open class cannot start in the past. (code 51001)';
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
('MANAGE_SUBSCRIPTIONS', 'Dang ky va gia han goi tap')
ON CONFLICT (permission_name) DO NOTHING;

-- Role → Permission assignments
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (1,1),(1,2),(1,3),(1,4),(1,7),(1,8),(1,9),
    (2,5),(2,6),
    (4,6),(4,7),(4,8),(4,11),(4,12),
    (5,1),(5,4),(5,9),(5,10)
ON CONFLICT DO NOTHING;

-- Users (password: 12345678, stored as BCrypt hash)
-- BCrypt hash: $2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi
INSERT INTO users (role_id, full_name, email, phone, password_hash, address, gender, date_of_birth) VALUES
(1, 'Nguyen Van An',    'manager@scms.com',        '0901234501', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '12 Nguyen Hue, Q1, TP.HCM',          'Male',   '1985-03-12'),
(2, 'Tran Thi Huong',   'huong.tran@fitzone.vn',   '0901234502', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '45 Le Loi, Q1, TP.HCM',              'Female', '1992-07-20'),
(2, 'Le Minh Khoa',     'khoa.le@fitzone.vn',      '0901234503', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '78 CMT8, Q3, TP.HCM',                'Male',   '1990-01-15'),
(2, 'Pham Thi Lan',     'lan.pham@fitzone.vn',     '0901234504', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '23 NTM Khai, Q1, TP.HCM',            'Female', '1994-11-05'),
(4, 'Do Thi Mai',       'mai.do@fitzone.vn',       '0901234505', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '56 Dien Bien Phu, Binh Thanh',       'Female', '1998-04-22'),
(4, 'Vu Van Nam',       'nam.vu@fitzone.vn',       '0901234506', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '89 Phan Xich Long, Phu Nhuan',       'Male',   '1997-09-30'),
(3, 'Hoang Thi Oanh',   'oanh.hoang@fitzone.vn',   '0901234507', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '12 Tran Hung Dao, Q5, TP.HCM',       'Female', '1996-06-18'),
(3, 'Bui Van Phuc',     'phuc.bui@fitzone.vn',     '0901234508', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '34 Nguyen Trai, Q5, TP.HCM',         'Male',   '1993-02-27'),
(3, 'Ngo Thi Quyen',    'quyen.ngo@fitzone.vn',    '0901234509', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '67 Ly Thuong Kiet, Q10, TP.HCM',     'Female', '1999-12-01'),
(3, 'Dang Van Son',     'son.dang@fitzone.vn',     '0901234510', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '90 Ba Thang Hai, Q10, TP.HCM',       'Male',   '1988-08-08'),
(3, 'Ly Thi Thu',       'thu.ly@fitzone.vn',       '0901234511', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '11 Hoang Van Thu, Tan Binh, TP.HCM', 'Female', '2000-05-14'),
(3, 'Trinh Van Vinh',   'vinh.trinh@fitzone.vn',   '0901234512', '$2a$10$4GZCdOj0TOi2rNMWm17FVeEVBIXhEGmJCEeYM2VDqEqeYJAqT9sFi', '22 Cong Hoa, Tan Binh, TP.HCM',      'Male',   '1991-10-09')
ON CONFLICT (email) DO NOTHING;

INSERT INTO center_managers (user_id) VALUES (1) ON CONFLICT DO NOTHING;
INSERT INTO receptionists   (user_id) VALUES (5),(6) ON CONFLICT DO NOTHING;

INSERT INTO coaches (user_id, specialization, certification, bio) VALUES
(2, 'Yoga, Pilates',        'RYT-200',       'Hon 6 nam kinh nghiem giang day Yoga cho moi trinh do.'),
(3, 'Gym, Tang co giam mo', 'NASM-CPT',      'Chuyen huan luyen the hinh, dong hanh cung nhieu hoc vien.'),
(4, 'Boxing, Kickboxing',   'Boxing cap 2',  'Cuu van dong vien boxing phong trao.')
ON CONFLICT DO NOTHING;

INSERT INTO members (user_id, goal, health_note, join_date, registered_by) VALUES
(7,  'Giam can, tang su deo dai',     NULL,                  '2026-08-01', 5),
(8,  'Tang co',                       NULL,                  '2026-09-01', 6),
(9,  'Cai thien suc khoe tong the',   'Co tien su dau lung', '2026-06-15', 5),
(10, 'Giam can, tang suc ben',        NULL,                  '2026-09-05', 6),
(11, 'Lam quen voi tap luyen',        NULL,                  '2026-08-15', 5),
(12, 'Tang suc ben, hoc boxing',      NULL,                  '2026-01-10', NULL)
ON CONFLICT DO NOTHING;

-- Subjects, Rooms, Classes
INSERT INTO subjects (subject_name, description) VALUES
('Yoga',             'Cac lop Yoga cho moi trinh do'),
('Gym co ban',       'Tap luyen suc manh, tang co giam mo'),
('Boxing/Kickboxing','Ren luyen the luc va ky thuat vo thuat'),
('Cardio/Zumba',     'Cac lop van dong cuong do cao')
ON CONFLICT DO NOTHING;

INSERT INTO rooms (room_name, location, capacity, status) VALUES
('Phong Yoga A',   'Tang 2', 20, 'Available'),
('Phong Gym B',    'Tang 1', 30, 'Available'),
('Phong Vo C',     'Tang 3', 15, 'Available'),
('Phong Cardio D', 'Tang 2', 25, 'Available')
ON CONFLICT DO NOTHING;

-- Temporarily disable class trigger to allow inserting historical demo data
ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;

INSERT INTO classes (class_name, subject_id, coach_id, max_capacity, start_date, end_date, status) VALUES
('Yoga Buoi Sang', 1, 2, 20, '2026-09-01', '2026-12-01', 'Ongoing'),
('Gym Suc Manh',   2, 3, 25, '2026-09-01', '2026-12-01', 'Ongoing'),
('Boxing Co Ban',  3, 4, 15, '2026-09-15', '2026-12-15', 'Open'),
('Zumba Cardio',   4, 2, 25, '2026-09-10', '2026-12-10', 'Ongoing')
ON CONFLICT DO NOTHING;

ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;

-- Training plans
INSERT INTO training_plans (coach_id, class_id, member_id, title, content, goal, start_date, end_date) VALUES
(2, 1, NULL, 'Giao trinh Yoga 4 tuan cho nguoi moi',  'Tuan 1-2: tu the co ban; Tuan 3-4: nang cao', 'Tang do deo dai',    '2026-09-01', '2026-09-28'),
(3, 2, NULL, 'Giao trinh tang co 8 tuan',              'Chia lich tap theo nhom co',                  'Tang khoi luong co', '2026-09-01', '2026-10-27'),
(3, NULL, 10,'Ke hoach giam can ca nhan - Son',        'Ket hop cardio va tap ta nhe',                'Giam 5kg',           '2026-09-05', '2026-11-05'),
(4, NULL, 12,'Ke hoach tang suc ben Boxing - Vinh',    'Tang dan cuong do dam bao cat',               'Cai thien suc ben',  '2026-09-15', '2026-11-15')
ON CONFLICT DO NOTHING;

-- Sessions (disable trigger for past dates)
ALTER TABLE sessions DISABLE TRIGGER trg_sessions_validate;

INSERT INTO sessions (class_id, room_id, session_date, start_time, end_time, status) VALUES
(1, 1, '2026-09-10', '06:00', '07:00', 'Completed'),
(1, 1, '2026-09-24', '06:00', '07:00', 'Scheduled'),
(2, 2, '2026-09-11', '18:00', '19:30', 'Completed'),
(2, 2, '2026-09-25', '18:00', '19:30', 'Scheduled'),
(3, 3, '2026-09-12', '19:00', '20:00', 'Completed'),
(3, 3, '2026-09-26', '19:00', '20:00', 'Scheduled'),
(4, 4, '2026-09-13', '17:00', '18:00', 'Completed'),
(4, 4, '2026-09-27', '17:00', '18:00', 'Scheduled')
ON CONFLICT DO NOTHING;

ALTER TABLE sessions ENABLE TRIGGER trg_sessions_validate;

-- Training results
INSERT INTO training_results (session_id, member_id, coach_id, content, attendance_status) VALUES
(1, 7,  2, 'Thuc hien tot cac tu the co ban', 'Present'),
(1, 9,  2, 'Tham gia tich cuc, tu the can bang tot', 'Present'),
(3, 8,  3, 'Hoan thanh bai tap nguc - vai', 'Present'),
(3, 10, 3, 'Den tre 15 phut', 'Late'),
(5, 10, 4, 'Tap ky thuat dam co ban', 'Present'),
(5, 12, 4, 'Vang buoi tap khong bao truoc', 'Absent'),
(7, 7,  2, 'Nang luong tot, theo kip nhip do Zumba', 'Present'),
(7, 11, 2, 'Buoi dau tham gia, con bor ngo', 'Present')
ON CONFLICT DO NOTHING;

-- Membership packages
INSERT INTO membership_packages (package_name, price, duration_days, benefits) VALUES
('Goi Co Ban 1 Thang',      500000,  30,  'Tap gym tu do, khong kem HLV'),
('Goi Tieu Chuan 3 Thang',  1350000, 90,  'Tap gym tu do + toi da 2 lop nhom/tuan'),
('Goi Cao Cap 6 Thang',     2500000, 180, 'Khong gioi han lop hoc + 2 buoi PT rieng/thang'),
('Goi VIP 12 Thang',        4800000, 365, 'Toan quyen su dung dich vu + PT rieng khong gioi han')
ON CONFLICT DO NOTHING;

INSERT INTO member_packages (member_id, package_id, start_date, end_date, status) VALUES
(7,  2, '2026-08-01', '2026-10-30', 'Active'),
(8,  1, '2026-09-01', '2026-10-01', 'Active'),
(9,  3, '2026-06-15', '2026-12-12', 'Active'),
(10, 2, '2026-09-05', '2026-12-04', 'Active'),
(11, 1, '2026-08-15', '2026-09-14', 'Expired'),
(12, 4, '2026-01-10', '2027-01-10', 'Active')
ON CONFLICT DO NOTHING;

INSERT INTO class_enrollments (member_id, class_id, status) VALUES
(7,  1, 'Registered'), (7,  4, 'Registered'),
(8,  2, 'Registered'), (9,  1, 'Registered'),
(10, 2, 'Registered'), (10, 3, 'Registered'),
(11, 4, 'Registered'), (12, 3, 'Registered')
ON CONFLICT DO NOTHING;

-- Invoices
INSERT INTO invoices (member_id, package_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref) VALUES
(7,  2, 5, 1350000, 'BankTransfer', 'Paid',    '2026-08-01 00:00:00', 'TXN-70001'),
(8,  1, 6, 500000,  'Cash',         'Paid',    '2026-09-01 00:00:00', NULL),
(9,  3, 5, 2500000, 'CreditCard',   'Paid',    '2026-06-15 00:00:00', 'TXN-70002'),
(10, 2, 6, 1350000, 'EWallet',      'Paid',    '2026-09-05 00:00:00', 'TXN-70003'),
(11, 1, 5, 500000,  'Cash',         'Paid',    '2026-08-15 00:00:00', NULL),
(12, 4, 6, 4800000, 'BankTransfer', 'Paid',    '2026-01-10 00:00:00', 'TXN-70004'),
(11, 2, 5, 1350000, NULL,           'Pending', NULL,                   NULL)
ON CONFLICT DO NOTHING;

-- Support requests
INSERT INTO support_requests (member_id, receptionist_id, content, status, created_at, resolved_at) VALUES
(8,  6,    'Hoi ve lich tap bu khi nghi om',       'Resolved',   '2026-09-10 09:15:00', '2026-09-11 14:00:00'),
(11, NULL, 'Muon doi sang goi tap co kem HLV rieng', 'Open',     '2026-09-20 10:30:00', NULL),
(9,  5,    'Yeu cau xuat lai hoa don co VAT',      'InProgress', '2026-09-21 16:45:00', NULL)
ON CONFLICT DO NOTHING;

-- Notifications (individual)
INSERT INTO notifications (user_id, sender_id, title, content, type, created_at) VALUES
(7,  2,    'Nhac lich tap Yoga',          'Ban co lop Yoga Buoi Sang vao 6:00 ngay mai.',    'Schedule', '2026-09-23 20:00:00'),
(10, NULL, 'Goi tap sap het han',         'Goi Tieu Chuan 3 Thang cua ban het han 04/12.',   'Payment',  '2026-09-20 08:00:00'),
(11, NULL, 'Goi tap da het han',          'Goi Co Ban 1 Thang da het han, vui long gia han.','Payment',  '2026-09-15 08:00:00'),
(12, 4,    'Bai tap ve nha tuan nay',     'Tap footwork 15 phut moi ngay.',                  'Other',    '2026-09-13 21:00:00'),
(8,  6,    'Xac nhan thanh toan',         'Da nhan thanh toan cho Goi Co Ban 1 Thang.',      'Payment',  '2026-09-01 10:05:00')
ON CONFLICT DO NOTHING;

-- Broadcast notifications (user_id = NULL)
INSERT INTO notifications (user_id, sender_id, title, content, type, created_at) VALUES
(NULL, 1, 'Lich nghi Le Quoc Khanh 2/9',        'Trung tam nghi ngay 02/09, cac lop day bu tuan sau.', 'System',   '2026-08-28 09:00:00'),
(NULL, 2, 'Lop Yoga tam nghi ngay 24/09',        'Doi sang 25/09 cung khung gio do bao tri phong tap.', 'Schedule', '2026-09-22 18:00:00')
ON CONFLICT DO NOTHING;

INSERT INTO notification_recipients (notification_id, user_id, is_read, read_at)
SELECT n.notification_id, u.user_id, FALSE, NULL
FROM notifications n, (VALUES (2),(3),(4),(5),(6)) AS u(user_id)
WHERE n.title = 'Lich nghi Le Quoc Khanh 2/9'
ON CONFLICT DO NOTHING;

INSERT INTO notification_recipients (notification_id, user_id, is_read, read_at)
SELECT n.notification_id, u.user_id, FALSE, NULL
FROM notifications n, (VALUES (7),(9)) AS u(user_id)
WHERE n.title = 'Lop Yoga tam nghi ngay 24/09'
ON CONFLICT DO NOTHING;

-- AI Consultations
INSERT INTO ai_consultations (user_id, request_content, response_content, created_at) VALUES
(7,  'Lich tap Yoga tuan nay cua toi nhu the nao?', 'Ban co lop Yoga Buoi Sang vao 6:00. Buoi gan nhat la 24/09.', '2026-09-22 19:30:00'),
(10, 'Toi nen an gi truoc khi tap gym buoi toi?',   'An nhe giau carb va protein khoang 1-2 gio truoc buoi tap.',  '2026-09-24 17:00:00'),
(2,  'Goi y bai khoi dong gian co cho hoc vien Yoga moi', 'Bat dau voi tu the Meo-Bo, gap nguoi nhe nhang 5-7 phut.', '2026-09-09 15:00:00')
ON CONFLICT DO NOTHING;

-- Evaluations
INSERT INTO evaluations (member_id, coach_id, evaluation_date, comment, progress_score) VALUES
(7,  2, '2026-09-20', 'Tien bo ro ret ve do deo dai sau 3 tuan.', 7.5),
(8,  3, '2026-09-22', 'Tang deu muc ta qua tung buoi.',           8.0),
(9,  2, '2026-09-20', 'Giu duoc nhip tap deu dan.',               7.0),
(10, 3, '2026-09-23', 'Can cai thien viec di tre.',               6.5),
(10, 4, '2026-09-23', 'Phan xa dam bao cat nhanh.',               7.0),
(12, 4, '2026-09-18', 'Moi tham gia nhung tinh than rat tot.',    6.0)
ON CONFLICT DO NOTHING;

-- Attendances
INSERT INTO attendances (session_id, member_id, recorded_by, state, check_in_time, check_out_time) VALUES
(1,    7,  2, 'Present',   '2026-09-10 05:55:00', '2026-09-10 07:05:00'),
(1,    9,  2, 'Present',   '2026-09-10 06:02:00', '2026-09-10 07:00:00'),
(3,    8,  3, 'Present',   '2026-09-11 17:58:00', '2026-09-11 19:35:00'),
(5,    12, 4, 'Absent',    NULL,                   NULL),
(NULL, 10, 5, 'CheckedIn', '2026-09-21 17:30:00', '2026-09-21 19:00:00'),
(NULL, 11, 6, 'CheckedIn', '2026-09-19 06:10:00', '2026-09-19 07:20:00'),
(NULL, 7,  6, 'CheckedIn', '2026-09-15 06:00:00', NULL)
ON CONFLICT DO NOTHING;

-- System logs
INSERT INTO system_logs (user_id, action, target_entity, target_id, detail, "timestamp") VALUES
(1, 'CREATE_CLASS',    'classes',             1, 'Tao lop Yoga Buoi Sang',           '2026-08-25 09:00:00'),
(1, 'ASSIGN_COACH',    'classes',             3, 'Phan cong HLV Pham Thi Lan',       '2026-09-14 10:00:00'),
(5, 'PROCESS_PAYMENT', 'invoices',            1, 'Ghi nhan thanh toan hoa don #1',   '2026-08-01 09:30:00'),
(1, 'UPDATE_PACKAGE',  'membership_packages', 3, 'Cap nhat mo ta goi Cao Cap 6 T',   '2026-07-01 11:00:00')
ON CONFLICT DO NOTHING;
