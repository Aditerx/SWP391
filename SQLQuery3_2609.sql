-- SQLQuery3_2609.sql
-- Fresh demo bootstrap for SQL Server Express.
-- WARNING: this script intentionally resets gym_management_system. Back up any
-- existing data before running it. It is not executed by Spring/Hibernate.
-- For an existing database, run only reviewed ALTER/INSERT migration steps.
USE master;
GO

IF DB_ID(N'gym_management_system') IS NOT NULL
BEGIN
    ALTER DATABASE gym_management_system SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE gym_management_system;
END
GO

CREATE DATABASE gym_management_system;
GO

USE gym_management_system;
GO

-- =====================================================================
-- 1. PHAN QUYEN: ROLE - PERMISSION
-- =====================================================================

CREATE TABLE roles (
    role_id     INT IDENTITY(1,1) PRIMARY KEY,
    role_name   NVARCHAR(50)  NOT NULL UNIQUE,   -- CenterManager, Coach, Member, Receptionist...
    description NVARCHAR(255)
);

CREATE TABLE permissions (
    permission_id   INT IDENTITY(1,1) PRIMARY KEY,
    permission_name NVARCHAR(100) NOT NULL UNIQUE,
    description     NVARCHAR(255)
);

-- Quan he N-N giua Role va Permission
CREATE TABLE role_permissions (
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
-- 2. USER (bang goc) + cac bang vai tro con (subtype: is-a)
-- =====================================================================

CREATE TABLE users (
    user_id       INT IDENTITY(1,1) PRIMARY KEY,
    role_id       INT NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    email         NVARCHAR(100) NOT NULL UNIQUE,
    phone         NVARCHAR(20),
    password_hash NVARCHAR(255) NOT NULL,
    address       NVARCHAR(255),
    gender        NVARCHAR(10) CHECK (gender IN (N'Male', N'Female', N'Other')),
    date_of_birth DATE,
    status        NVARCHAR(20) NOT NULL DEFAULT N'Active'
        CHECK (status IN (N'Active', N'Inactive', N'Locked')),
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_user_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE INDEX idx_users_role ON users(role_id);

-- Center Manager (subtype cua User)
CREATE TABLE center_managers (
    user_id INT PRIMARY KEY,
    CONSTRAINT fk_cm_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- Receptionist (subtype cua User)
CREATE TABLE receptionists (
    user_id INT PRIMARY KEY,
    CONSTRAINT fk_receptionist_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- Coach (subtype cua User)
CREATE TABLE coaches (
    user_id        INT PRIMARY KEY,
    specialization NVARCHAR(100),
    certification  NVARCHAR(150),
    bio            NVARCHAR(MAX),
    CONSTRAINT fk_coach_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- Member (subtype cua User)
CREATE TABLE members (
    user_id       INT PRIMARY KEY,
    goal          NVARCHAR(255),
    health_note   NVARCHAR(MAX),
    join_date     DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    registered_by INT NULL,  -- receptionist dang ky tai quay (co the null neu tu dang ky online)
    CONSTRAINT fk_member_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_receptionist
        FOREIGN KEY (registered_by) REFERENCES receptionists(user_id)
        -- NO ACTION (mac dinh): tranh loi "multiple cascade paths" trung voi fk_member_user
);

-- =====================================================================
-- 3. SUBJECT - ROOM - CLASS
-- =====================================================================

CREATE TABLE subjects (
    subject_id   INT IDENTITY(1,1) PRIMARY KEY,
    subject_name NVARCHAR(100) NOT NULL,
    description  NVARCHAR(255)
);

CREATE TABLE rooms (
    room_id   INT IDENTITY(1,1) PRIMARY KEY,
    room_name NVARCHAR(50) NOT NULL,
    location  NVARCHAR(100),
    capacity  INT,
    status    NVARCHAR(20) NOT NULL DEFAULT N'Available'
        CHECK (status IN (N'Available', N'Maintenance', N'Closed')),
    CONSTRAINT chk_room_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE classes (
    class_id     INT IDENTITY(1,1) PRIMARY KEY,
    class_name   NVARCHAR(100) NOT NULL,
    subject_id   INT NOT NULL,
    coach_id     INT NULL,          -- HLV phu trach lop
    max_capacity INT,
    start_date   DATE,
    end_date     DATE,
    status       NVARCHAR(20) NOT NULL DEFAULT N'Open'
        CHECK (status IN (N'Open', N'Ongoing', N'Closed', N'Cancelled')),
    CONSTRAINT chk_class_capacity CHECK (max_capacity IS NULL OR max_capacity > 0),
    CONSTRAINT chk_class_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT fk_class_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id),
    CONSTRAINT fk_class_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id)
        -- NO ACTION: giu classes doc lap voi cay cascade cua users
);

CREATE INDEX idx_classes_subject ON classes(subject_id);
CREATE INDEX idx_classes_coach ON classes(coach_id);

-- =====================================================================
-- 4. TRAINING PLAN (ke hoach tap luyen: cho lop hoac ca nhan)
-- =====================================================================

CREATE TABLE training_plans (
    plan_id    INT IDENTITY(1,1) PRIMARY KEY,
    coach_id   INT NOT NULL,
    class_id   INT NULL,   -- ke hoach cho ca lop
    member_id  INT NULL,   -- ke hoach ca nhan
    title      NVARCHAR(150) NOT NULL,
    content    NVARCHAR(MAX),
    goal       NVARCHAR(255),
    start_date DATE,
    end_date   DATE,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_plan_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id),
    CONSTRAINT fk_plan_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id),
    CONSTRAINT fk_plan_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT chk_plan_target CHECK (
        (class_id IS NOT NULL AND member_id IS NULL)
        OR (class_id IS NULL AND member_id IS NOT NULL)
    ),
    CONSTRAINT chk_plan_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_plan_coach ON training_plans(coach_id);
CREATE INDEX idx_plan_class ON training_plans(class_id);
CREATE INDEX idx_plan_member ON training_plans(member_id);

-- =====================================================================
-- 5. SESSION (buoi tap cu the cua 1 lop) + TRAINING RESULT (diem danh/ket qua)
-- =====================================================================

CREATE TABLE sessions (
    session_id   INT IDENTITY(1,1) PRIMARY KEY,
    class_id     INT NOT NULL,
    room_id      INT NOT NULL,
    session_date DATE NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    status       NVARCHAR(20) NOT NULL DEFAULT N'Scheduled'
        CHECK (status IN (N'Scheduled', N'Completed', N'Cancelled')),
    CONSTRAINT chk_session_time CHECK (end_time > start_time),
    CONSTRAINT fk_session_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_session_room
        FOREIGN KEY (room_id) REFERENCES rooms(room_id)
);

CREATE INDEX idx_session_class ON sessions(class_id);
CREATE INDEX idx_session_room ON sessions(room_id);

-- Ket qua tap luyen cua tung hoc vien trong tung buoi (kiem diem danh)
CREATE TABLE training_results (
    result_id         INT IDENTITY(1,1) PRIMARY KEY,
    session_id        INT NOT NULL,
    member_id         INT NOT NULL,
    coach_id          INT NOT NULL,
    content            NVARCHAR(MAX),        -- nhan xet / ket qua tap luyen
    attendance_status  NVARCHAR(10) NOT NULL DEFAULT N'Present'
        CHECK (attendance_status IN (N'Present', N'Absent', N'Late')),
    recorded_at        DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_result_session
        FOREIGN KEY (session_id) REFERENCES sessions(session_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_result_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_result_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id),
    CONSTRAINT uq_result_session_member UNIQUE (session_id, member_id)
);

-- =====================================================================
-- 5b. ATTENDANCE (diem danh - gan voi 1 buoi tap HOAC check-in truc tiep
-- tai quay, dung entity "Attendance" rieng trong ERD)
-- =====================================================================
-- Luu y thiet ke: training_results o tren da co cot attendance_status
-- rieng cho diem danh GAN VOI 1 BUOI TAP cu the (do Coach ghi nhan luc
-- nhan xet ket qua). Bang attendances ben duoi bo sung cho dung voi ERD
-- va dung chung duoc cho ca 2 truong hop:
--   - session_id co gia tri: diem danh trong buoi tap (Coach ghi)
--   - session_id = NULL    : check-in/check-out truc tiep tai quay, KHONG
--     gan lop/buoi nao (Receptionist ghi - dung cho use case
--     "Take Member Attendance" ma ban ERD truoc con thieu)
-- Neu ve sau muon gom ve 1 nguon du lieu duy nhat, co the bo cot
-- attendance_status trong training_results va dung han bang nay.

CREATE TABLE attendances (
    attendance_id  INT IDENTITY(1,1) PRIMARY KEY,
    session_id     INT NULL,
    member_id      INT NOT NULL,
    recorded_by    INT NULL,     -- user_id cua Coach hoac Receptionist ghi nhan
    state          NVARCHAR(20) NOT NULL DEFAULT N'Present'
        CHECK (state IN (N'Present', N'Absent', N'Late', N'CheckedIn')),
    check_in_time  DATETIME NULL,
    check_out_time DATETIME NULL,
    CONSTRAINT fk_attendance_session
        FOREIGN KEY (session_id) REFERENCES sessions(session_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_attendance_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_attendance_recorder
        FOREIGN KEY (recorded_by) REFERENCES users(user_id)
);

-- Trong 1 buoi tap, 1 hoc vien chi co 1 dong diem danh (khong ap dung
-- cho check-in tai quay vi session_id = NULL)
CREATE UNIQUE INDEX ux_attendance_session_member
    ON attendances(session_id, member_id)
    WHERE session_id IS NOT NULL;

CREATE INDEX idx_attendance_member ON attendances(member_id);

-- =====================================================================
-- 5c. EVALUATION (Coach danh gia TIEN DO TONG THE cua hoc vien - dinh ky,
-- khac voi training_results la ket qua/diem danh cua TUNG buoi tap)
-- =====================================================================

CREATE TABLE evaluations (
    evaluation_id   INT IDENTITY(1,1) PRIMARY KEY,
    member_id       INT NOT NULL,
    coach_id        INT NOT NULL,
    evaluation_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    comment         NVARCHAR(MAX),
    progress_score  DECIMAL(3,1)
        CHECK (progress_score BETWEEN 0 AND 10),
    CONSTRAINT fk_eval_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_eval_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id)
);

CREATE INDEX idx_eval_member ON evaluations(member_id);
CREATE INDEX idx_eval_coach  ON evaluations(coach_id);

-- =====================================================================
-- 6. MEMBERSHIP PACKAGE + DANG KY GOI + DANG KY LOP
-- =====================================================================

CREATE TABLE membership_packages (
    package_id    INT IDENTITY(1,1) PRIMARY KEY,
    package_name  NVARCHAR(100) NOT NULL,
    price         DECIMAL(12,2) NOT NULL,
    duration_days INT NOT NULL,
    benefits      NVARCHAR(MAX),
    status        NVARCHAR(20) NOT NULL DEFAULT N'Active'
        CHECK (status IN (N'Active', N'Inactive'))
);

-- Member dang ky MembershipPackage (N-N theo thoi gian su dung)
CREATE TABLE member_packages (
    subscription_id INT IDENTITY(1,1) PRIMARY KEY,
    member_id       INT NOT NULL,
    package_id      INT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          NVARCHAR(20) NOT NULL DEFAULT N'Active'
        CHECK (status IN (N'Active', N'Expired', N'Cancelled')),
    CONSTRAINT fk_mp_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_mp_package
        FOREIGN KEY (package_id) REFERENCES membership_packages(package_id)
);

CREATE INDEX idx_mp_member ON member_packages(member_id);
CREATE INDEX idx_mp_package ON member_packages(package_id);

-- Member dang ky / huy dang ky lop hoc
CREATE TABLE class_enrollments (
    enrollment_id INT IDENTITY(1,1) PRIMARY KEY,
    member_id     INT NOT NULL,
    class_id      INT NOT NULL,
    enrolled_at   DATETIME NOT NULL DEFAULT GETDATE(),
    status        NVARCHAR(20) NOT NULL DEFAULT N'Registered'
        CHECK (status IN (N'Registered', N'Cancelled', N'Completed')),
    CONSTRAINT fk_enroll_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_enroll_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id),
    CONSTRAINT uq_enrollment UNIQUE (member_id, class_id)
);

-- =====================================================================
-- 7. INVOICE (hoa don / thanh toan)
-- =====================================================================

CREATE TABLE invoices (
    invoice_id              INT IDENTITY(1,1) PRIMARY KEY,
    member_id               INT NOT NULL,
    package_id              INT NULL,
    receptionist_id         INT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    payment_method           NVARCHAR(20)
        CHECK (payment_method IN (N'Cash', N'BankTransfer', N'CreditCard', N'EWallet')),
    payment_status           NVARCHAR(20) NOT NULL DEFAULT N'Pending'
        CHECK (payment_status IN (N'Pending', N'Paid', N'Failed', N'Refunded')),
    payment_date             DATETIME,
    gateway_transaction_ref  NVARCHAR(100),
    CONSTRAINT fk_invoice_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_invoice_package
        FOREIGN KEY (package_id) REFERENCES membership_packages(package_id),
    CONSTRAINT fk_invoice_receptionist
        FOREIGN KEY (receptionist_id) REFERENCES receptionists(user_id)
);

CREATE INDEX idx_invoice_member ON invoices(member_id);
CREATE INDEX idx_invoice_receptionist ON invoices(receptionist_id);

-- =====================================================================
-- 8. SUPPORT REQUEST (yeu cau ho tro tu thanh vien)
-- =====================================================================

CREATE TABLE support_requests (
    request_id       INT IDENTITY(1,1) PRIMARY KEY,
    member_id        INT NOT NULL,
    receptionist_id  INT NULL,
    content          NVARCHAR(MAX) NOT NULL,
    status           NVARCHAR(20) NOT NULL DEFAULT N'Open'
        CHECK (status IN (N'Open', N'InProgress', N'Resolved', N'Closed')),
    created_at       DATETIME NOT NULL DEFAULT GETDATE(),
    resolved_at      DATETIME NULL,
    CONSTRAINT fk_support_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_support_receptionist
        FOREIGN KEY (receptionist_id) REFERENCES receptionists(user_id)
);

-- =====================================================================
-- 9. NOTIFICATION
-- =====================================================================

CREATE TABLE notifications (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT NOT NULL,   -- nguoi nhan
    sender_id       INT NULL,       -- nguoi gui (coach, receptionist, system...)
    title           NVARCHAR(150) NOT NULL,
    content         NVARCHAR(MAX),
    type            NVARCHAR(20) NOT NULL DEFAULT N'Other'
        CHECK (type IN (N'Schedule', N'Payment', N'System', N'Promotion', N'Other')),
    is_read         BIT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_noti_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_noti_sender
        FOREIGN KEY (sender_id) REFERENCES users(user_id)
        -- NO ACTION: tranh loi cascade path doi voi "sender/receiver" cung tro ve users
);

CREATE INDEX idx_noti_user ON notifications(user_id);

-- =====================================================================
-- 9b. NOTIFICATION_RECIPIENT (1 thong bao gui cho NHIEU nguoi nhan cung
-- luc - dung entity "NotificationRecipient" rieng trong ERD)
-- =====================================================================
-- notifications.user_id van giu nguyen, dung cho thong bao gui RIENG cho
-- 1 nguoi (nhu du lieu mau hien co). Khi can gui 1 thong bao cho NHIEU
-- nguoi cung luc (vd: thong bao chung toan HLV, thong bao huy 1 lop cho
-- tat ca hoc vien da dang ky...), de user_id = NULL va luu danh sach
-- nguoi nhan + trang thai da doc RIENG cho tung nguoi trong bang
-- notification_recipients.

ALTER TABLE notifications ALTER COLUMN user_id INT NULL;

CREATE TABLE notification_recipients (
    notification_id INT NOT NULL,
    user_id          INT NOT NULL,
    is_read          BIT NOT NULL DEFAULT 0,
    read_at          DATETIME NULL,
    CONSTRAINT pk_notification_recipients PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_nr_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(notification_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_nr_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        -- NO ACTION: users -> notifications -> notification_recipients da la
        -- 1 duong CASCADE roi, de CASCADE them o day se bi loi
        -- "multiple cascade paths" (giong ly do fk_noti_sender o tren)
);

CREATE INDEX idx_nr_user ON notification_recipients(user_id);

-- =====================================================================
-- 10. AI CONSULTATION (Member/Coach hoi AI)
-- =====================================================================

CREATE TABLE ai_consultations (
    consultation_id   INT IDENTITY(1,1) PRIMARY KEY,
    user_id           INT NOT NULL,  -- member hoac coach
    request_content   NVARCHAR(MAX) NOT NULL,
    response_content  NVARCHAR(MAX),
    created_at        DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_ai_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
);

-- =====================================================================
-- 11. SYSTEM LOG (nhat ky thao tac quan trong)
-- =====================================================================

CREATE TABLE system_logs (
    log_id        INT IDENTITY(1,1) PRIMARY KEY,
    user_id       INT NULL,
    action        NVARCHAR(100) NOT NULL,
    target_entity NVARCHAR(100),
    target_id     INT,
    detail        NVARCHAR(MAX),
    [timestamp]   DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_log_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_log_user ON system_logs(user_id);
CREATE INDEX idx_log_timestamp ON system_logs([timestamp]);
GO

-- =====================================================================
-- 12. DU LIEU MAU (seed) CHO ROLES/PERMISSIONS
-- =====================================================================

INSERT INTO roles (role_name, description) VALUES
(N'CenterManager', N'Quan ly trung tam - toan quyen he thong'),
(N'Coach', N'Huan luyen vien'),
(N'Member', N'Thanh vien / hoc vien'),
(N'Receptionist', N'Nhan vien le tan');

INSERT INTO permissions (permission_name, description) VALUES
(N'MANAGE_USERS', N'Quan ly nguoi dung'),
(N'MANAGE_CLASSES', N'Quan ly lop hoc'),
(N'MANAGE_PACKAGES', N'Quan ly goi thanh vien'),
(N'VIEW_REPORTS', N'Xem bao cao'),
(N'MANAGE_TRAINING_PLAN', N'Tao/sua ke hoach tap luyen'),
(N'RECORD_RESULT', N'Ghi nhan ket qua buoi tap'),
(N'PROCESS_PAYMENT', N'Ghi nhan thanh toan, xuat hoa don'),
(N'HANDLE_SUPPORT', N'Xu ly yeu cau ho tro'),
(N'VIEW_AUDIT_LOG', N'Xem lich su thao tac he thong');
GO

-- =====================================================================
-- 13. PHAN QUYEN CHO TUNG VAI TRO (role_permissions)
-- =====================================================================

INSERT INTO role_permissions (role_id, permission_id) VALUES
    (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9), -- CenterManager: toan quyen
(2,5),(2,6),                                        -- Coach: quan ly ke hoach tap, ghi ket qua
(4,7),(4,8);                                        -- Receptionist: thanh toan, ho tro
GO

-- =====================================================================
-- 14. NGUOI DUNG MAU (users) + cac bang vai tro con
-- Mat khau demo cho tat ca: 12345678 (da hash bang SHA2_256).
-- Tai khoan Center Manager dung cho FE demo: manager@scms.com / 12345678.
-- Thu tu insert: 1=CenterManager, 2-4=Coach, 5-6=Receptionist, 7-12=Member
-- =====================================================================

DECLARE @pwd NVARCHAR(255) = CONVERT(NVARCHAR(255), HASHBYTES('SHA2_256', N'12345678'), 2);

INSERT INTO users (role_id, full_name, email, phone, password_hash, address, gender, date_of_birth) VALUES
(1, N'Nguyễn Văn An',    N'manager@scms.com',        N'0901234501', @pwd, N'12 Nguyễn Huệ, Quận 1, TP.HCM',        N'Male',   '1985-03-12'),
(2, N'Trần Thị Hương',   N'huong.tran@fitzone.vn',   N'0901234502', @pwd, N'45 Lê Lợi, Quận 1, TP.HCM',            N'Female', '1992-07-20'),
(2, N'Lê Minh Khoa',     N'khoa.le@fitzone.vn',      N'0901234503', @pwd, N'78 Cách Mạng Tháng 8, Quận 3, TP.HCM', N'Male',   '1990-01-15'),
(2, N'Phạm Thị Lan',     N'lan.pham@fitzone.vn',     N'0901234504', @pwd, N'23 Nguyễn Thị Minh Khai, Quận 1, TP.HCM', N'Female', '1994-11-05'),
(4, N'Đỗ Thị Mai',       N'mai.do@fitzone.vn',       N'0901234505', @pwd, N'56 Điện Biên Phủ, Bình Thạnh, TP.HCM', N'Female', '1998-04-22'),
(4, N'Vũ Văn Nam',       N'nam.vu@fitzone.vn',       N'0901234506', @pwd, N'89 Phan Xích Long, Phú Nhuận, TP.HCM', N'Male',   '1997-09-30'),
(3, N'Hoàng Thị Oanh',   N'oanh.hoang@fitzone.vn',   N'0901234507', @pwd, N'12 Trần Hưng Đạo, Quận 5, TP.HCM',     N'Female', '1996-06-18'),
(3, N'Bùi Văn Phúc',     N'phuc.bui@fitzone.vn',     N'0901234508', @pwd, N'34 Nguyễn Trãi, Quận 5, TP.HCM',       N'Male',   '1993-02-27'),
(3, N'Ngô Thị Quyên',    N'quyen.ngo@fitzone.vn',    N'0901234509', @pwd, N'67 Lý Thường Kiệt, Quận 10, TP.HCM',   N'Female', '1999-12-01'),
(3, N'Đặng Văn Sơn',     N'son.dang@fitzone.vn',     N'0901234510', @pwd, N'90 Ba Tháng Hai, Quận 10, TP.HCM',     N'Male',   '1988-08-08'),
(3, N'Lý Thị Thu',       N'thu.ly@fitzone.vn',       N'0901234511', @pwd, N'11 Hoàng Văn Thụ, Tân Bình, TP.HCM',   N'Female', '2000-05-14'),
(3, N'Trịnh Văn Vinh',   N'vinh.trinh@fitzone.vn',   N'0901234512', @pwd, N'22 Cộng Hòa, Tân Bình, TP.HCM',        N'Male',   '1991-10-09');
GO

INSERT INTO center_managers (user_id) VALUES (1);

INSERT INTO receptionists (user_id) VALUES (5), (6);

INSERT INTO coaches (user_id, specialization, certification, bio) VALUES
(2, N'Yoga, Pilates',           N'Chứng chỉ HLV Yoga quốc tế RYT-200', N'Hơn 6 năm kinh nghiệm giảng dạy Yoga cho mọi trình độ.'),
(3, N'Gym, Tăng cơ giảm mỡ',    N'NASM-CPT',                          N'Chuyên huấn luyện thể hình, đồng hành cùng nhiều học viên đạt mục tiêu tăng cơ.'),
(4, N'Boxing, Kickboxing',      N'Chứng chỉ HLV Boxing cấp 2',        N'Cựu vận động viên boxing phong trào, đam mê truyền lửa cho học viên mới.');

INSERT INTO members (user_id, goal, health_note, join_date, registered_by) VALUES
(7,  N'Giảm cân, tăng sự dẻo dai',        NULL,                             '2026-08-01', 5),
(8,  N'Tăng cơ',                          NULL,                             '2026-09-01', 6),
(9,  N'Cải thiện sức khỏe tổng thể',      N'Có tiền sử đau lưng nhẹ',       '2026-06-15', 5),
(10, N'Giảm cân, tăng sức bền',           NULL,                             '2026-09-05', 6),
(11, N'Làm quen với tập luyện',           NULL,                             '2026-08-15', 5),
(12, N'Tăng sức bền, học boxing',         NULL,                             '2026-01-10', NULL); -- tu dang ky online
GO

-- =====================================================================
-- 15. SUBJECT - ROOM - CLASS MAU
-- =====================================================================

INSERT INTO subjects (subject_name, description) VALUES
(N'Yoga',               N'Các lớp Yoga cho mọi trình độ, tăng sự dẻo dai và thư giãn tinh thần'),
(N'Gym cơ bản',          N'Tập luyện sức mạnh, tăng cơ giảm mỡ với thiết bị phòng gym'),
(N'Boxing/Kickboxing',   N'Rèn luyện thể lực và kỹ thuật võ thuật đối kháng'),
(N'Cardio/Zumba',        N'Các lớp vận động cường độ cao giúp đốt calo, cải thiện tim mạch');

INSERT INTO rooms (room_name, location, capacity, status) VALUES
(N'Phòng Yoga A',    N'Tầng 2', 20, N'Available'),
(N'Phòng Gym B',     N'Tầng 1', 30, N'Available'),
(N'Phòng Võ C',      N'Tầng 3', 15, N'Available'),
(N'Phòng Cardio D',  N'Tầng 2', 25, N'Available');

INSERT INTO classes (class_name, subject_id, coach_id, max_capacity, start_date, end_date, status) VALUES
(N'Yoga Buổi Sáng',   1, 2, 20, '2026-09-01', '2026-12-01', N'Ongoing'),
(N'Gym Sức Mạnh',     2, 3, 25, '2026-09-01', '2026-12-01', N'Ongoing'),
(N'Boxing Cơ Bản',    3, 4, 15, '2026-09-15', '2026-12-15', N'Open'),
(N'Zumba Cardio',     4, 2, 25, '2026-09-10', '2026-12-10', N'Ongoing');
GO

-- =====================================================================
-- 16. TRAINING PLAN MAU
-- =====================================================================

INSERT INTO training_plans (coach_id, class_id, member_id, title, content, goal, start_date, end_date) VALUES
(2, 1,    NULL, N'Giáo trình Yoga 4 tuần cho người mới',   N'Tuần 1-2: các tư thế cơ bản; Tuần 3-4: nâng cao độ khó', N'Tăng độ dẻo dai, giảm căng thẳng', '2026-09-01', '2026-09-28'),
(3, 2,    NULL, N'Giáo trình tăng cơ 8 tuần',              N'Chia lịch tập theo nhóm cơ, kết hợp dinh dưỡng',         N'Tăng khối lượng cơ nạc',           '2026-09-01', '2026-10-27'),
(3, NULL, 10,   N'Kế hoạch giảm cân cá nhân - Sơn',        N'Kết hợp cardio và tập tạ nhẹ, kiểm soát calo',          N'Giảm 5kg trong 2 tháng',           '2026-09-05', '2026-11-05'),
(4, NULL, 12,   N'Kế hoạch tăng sức bền Boxing - Vinh',    N'Tăng dần cường độ đấm bao cát, bài tập footwork',       N'Cải thiện sức bền và kỹ thuật',    '2026-09-15', '2026-11-15');
GO

-- =====================================================================
-- 17. SESSION + TRAINING RESULT MAU
-- =====================================================================

INSERT INTO sessions (class_id, room_id, session_date, start_time, end_time, status) VALUES
(1, 1, '2026-09-10', '06:00', '07:00', N'Completed'),
(1, 1, '2026-09-24', '06:00', '07:00', N'Scheduled'),
(2, 2, '2026-09-11', '18:00', '19:30', N'Completed'),
(2, 2, '2026-09-25', '18:00', '19:30', N'Scheduled'),
(3, 3, '2026-09-12', '19:00', '20:00', N'Completed'),
(3, 3, '2026-09-26', '19:00', '20:00', N'Scheduled'),
(4, 4, '2026-09-13', '17:00', '18:00', N'Completed'),
(4, 4, '2026-09-27', '17:00', '18:00', N'Scheduled');
GO

INSERT INTO training_results (session_id, member_id, coach_id, content, attendance_status) VALUES
(1, 7,  2, N'Thực hiện tốt các tư thế cơ bản, cần cải thiện nhịp thở', N'Present'),
(1, 9,  2, N'Tham gia tích cực, tư thế cân bằng tốt',                  N'Present'),
(3, 8,  3, N'Hoàn thành bài tập ngực - vai, tăng mức tạ nhẹ',          N'Present'),
(3, 10, 3, N'Đến trễ 15 phút, hoàn thành phần còn lại của buổi tập',   N'Late'),
(5, 10, 4, N'Tập kỹ thuật đấm cơ bản, phản xạ tốt',                    N'Present'),
(5, 12, 4, N'Vắng buổi tập không báo trước',                           N'Absent'),
(7, 7,  2, N'Năng lượng tốt, theo kịp nhịp độ Zumba',                  N'Present'),
(7, 11, 2, N'Buổi đầu tham gia, còn bỡ ngỡ nhưng nhiệt tình',          N'Present');
GO

-- =====================================================================
-- 18. MEMBERSHIP PACKAGE + DANG KY GOI + DANG KY LOP MAU
-- =====================================================================

INSERT INTO membership_packages (package_name, price, duration_days, benefits) VALUES
(N'Gói Cơ Bản 1 Tháng',    500000,  30,  N'Tập gym tự do, không kèm HLV'),
(N'Gói Tiêu Chuẩn 3 Tháng', 1350000, 90, N'Tập gym tự do + tối đa 2 lớp nhóm/tuần'),
(N'Gói Cao Cấp 6 Tháng',    2500000, 180, N'Không giới hạn lớp học + 2 buổi PT riêng/tháng'),
(N'Gói VIP 12 Tháng',       4800000, 365, N'Toàn quyền sử dụng dịch vụ + PT riêng không giới hạn');

INSERT INTO member_packages (member_id, package_id, start_date, end_date, status) VALUES
(7,  2, '2026-08-01', '2026-10-30', N'Active'),
(8,  1, '2026-09-01', '2026-10-01', N'Active'),
(9,  3, '2026-06-15', '2026-12-12', N'Active'),
(10, 2, '2026-09-05', '2026-12-04', N'Active'),
(11, 1, '2026-08-15', '2026-09-14', N'Expired'),
(12, 4, '2026-01-10', '2027-01-10', N'Active');

INSERT INTO class_enrollments (member_id, class_id, status) VALUES
(7,  1, N'Registered'),
(7,  4, N'Registered'),
(8,  2, N'Registered'),
(9,  1, N'Registered'),
(10, 2, N'Registered'),
(10, 3, N'Registered'),
(11, 4, N'Registered'),
(12, 3, N'Registered');
GO

-- =====================================================================
-- 19. INVOICE MAU
-- =====================================================================

INSERT INTO invoices (member_id, package_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref) VALUES
(7,  2, 5, 1350000, N'BankTransfer', N'Paid',    '2026-08-01', N'TXN-70001'),
(8,  1, 6, 500000,  N'Cash',         N'Paid',    '2026-09-01', NULL),
(9,  3, 5, 2500000, N'CreditCard',   N'Paid',    '2026-06-15', N'TXN-70002'),
(10, 2, 6, 1350000, N'EWallet',      N'Paid',    '2026-09-05', N'TXN-70003'),
(11, 1, 5, 500000,  N'Cash',         N'Paid',    '2026-08-15', NULL),
(12, 4, 6, 4800000, N'BankTransfer', N'Paid',    '2026-01-10', N'TXN-70004'),
(11, 2, 5, 1350000, NULL,            N'Pending', NULL,         NULL); -- hoa don gia han dang cho thanh toan
GO

-- =====================================================================
-- 20. SUPPORT REQUEST / NOTIFICATION / AI CONSULTATION MAU
-- =====================================================================

INSERT INTO support_requests (member_id, receptionist_id, content, status, created_at, resolved_at) VALUES
(8,  6,    N'Hỏi về lịch tập bù khi nghỉ ốm tuần trước',       N'Resolved',   '2026-09-10T09:15:00', '2026-09-11T14:00:00'),
(11, NULL, N'Muốn đổi sang gói tập có kèm huấn luyện viên riêng', N'Open',    '2026-09-20T10:30:00', NULL),
(9,  5,    N'Yêu cầu xuất lại hóa đơn có VAT',                  N'InProgress','2026-09-21T16:45:00', NULL);

INSERT INTO notifications (user_id, sender_id, title, content, type, created_at) VALUES
(7,  2,    N'Nhắc lịch tập Yoga',            N'Bạn có lớp Yoga Buổi Sáng vào 6:00 ngày mai, đừng quên nhé!',                      N'Schedule', '2026-09-23T20:00:00'),
(10, NULL, N'Gói tập sắp hết hạn',           N'Gói Tiêu Chuẩn 3 Tháng của bạn sẽ hết hạn vào 04/12/2026.',                        N'Payment',  '2026-09-20T08:00:00'),
(11, NULL, N'Gói tập đã hết hạn',            N'Gói Cơ Bản 1 Tháng của bạn đã hết hạn, vui lòng gia hạn để tiếp tục sử dụng.',      N'Payment',  '2026-09-15T08:00:00'),
(12, 4,    N'Bài tập về nhà tuần này',       N'Tập footwork 15 phút mỗi ngày trước buổi học tiếp theo nhé.',                      N'Other',    '2026-09-13T21:00:00'),
(8,  6,    N'Xác nhận thanh toán thành công', N'Chúng tôi đã nhận được thanh toán cho Gói Cơ Bản 1 Tháng của bạn.',               N'Payment',  '2026-09-01T10:05:00');

INSERT INTO ai_consultations (user_id, request_content, response_content, created_at) VALUES
(7,  N'Lịch tập Yoga tuần này của tôi như thế nào?', N'Theo lịch hiện tại, bạn có lớp Yoga Buổi Sáng vào các buổi 6:00 sáng. Buổi gần nhất là ngày 24/09.', '2026-09-22T19:30:00'),
(10, N'Tôi nên ăn gì trước khi tập gym buổi tối?',   N'Bạn nên ăn nhẹ giàu carb và protein khoảng 1-2 giờ trước buổi tập, ví dụ chuối kèm sữa chua hoặc bánh mì nguyên cám với trứng.', '2026-09-24T17:00:00'),
(2,  N'Gợi ý bài khởi động giãn cơ cho học viên Yoga mới bắt đầu', N'Có thể bắt đầu với tư thế Mèo-Bò, gập người nhẹ nhàng và xoay khớp cổ tay, cổ chân trong 5-7 phút trước buổi tập chính.', '2026-09-09T15:00:00');
GO

-- =====================================================================
-- 21. SYSTEM LOG MAU
-- =====================================================================

INSERT INTO system_logs (user_id, action, target_entity, target_id, detail, [timestamp]) VALUES
(1, N'CREATE_CLASS',    N'classes',              1, N'Tạo lớp Yoga Buổi Sáng',                                        '2026-08-25T09:00:00'),
(1, N'ASSIGN_COACH',    N'classes',              3, N'Phân công HLV Phạm Thị Lan phụ trách lớp Boxing Cơ Bản',        '2026-09-14T10:00:00'),
(5, N'PROCESS_PAYMENT', N'invoices',             1, N'Ghi nhận thanh toán hóa đơn #1 cho học viên Hoàng Thị Oanh',    '2026-08-01T09:30:00'),
(1, N'UPDATE_PACKAGE',  N'membership_packages',  3, N'Cập nhật mô tả quyền lợi gói Cao Cấp 6 Tháng',                  '2026-07-01T11:00:00');
GO

-- =====================================================================
-- 22. EVALUATION + ATTENDANCE MAU
-- =====================================================================

INSERT INTO evaluations (member_id, coach_id, evaluation_date, comment, progress_score) VALUES
(7,  2, '2026-09-20', N'Tiến bộ rõ rệt về độ dẻo dai sau 3 tuần, cần chú ý thêm nhịp thở khi giữ tư thế.', 7.5),
(8,  3, '2026-09-22', N'Tăng đều mức tạ qua từng buổi, thể lực cải thiện tốt.',                              8.0),
(9,  2, '2026-09-20', N'Giữ được nhịp tập đều đặn, tư thế cân bằng tốt hơn tuần đầu.',                       7.0),
(10, 3, '2026-09-23', N'Cần cải thiện việc đi trễ, kỹ thuật nâng tạ đã ổn định hơn.',                        6.5),
(10, 4, '2026-09-23', N'Phản xạ đấm bao cát nhanh, cần tập thêm về di chuyển chân.',                          7.0),
(12, 4, '2026-09-18', N'Mới tham gia nhưng tinh thần rất tốt, cần theo sát thêm để tránh chấn thương.',       6.0);

INSERT INTO attendances (session_id, member_id, recorded_by, state, check_in_time, check_out_time) VALUES
(1,    7,  2, N'Present',   '2026-09-10T05:55:00', '2026-09-10T07:05:00'),  -- diem danh trong buoi tap, do Coach ghi
(1,    9,  2, N'Present',   '2026-09-10T06:02:00', '2026-09-10T07:00:00'),
(3,    8,  3, N'Present',   '2026-09-11T17:58:00', '2026-09-11T19:35:00'),
(5,    12, 4, N'Absent',    NULL,                   NULL),
(NULL, 10, 5, N'CheckedIn', '2026-09-21T17:30:00', '2026-09-21T19:00:00'), -- check-in tai quay, do Receptionist ghi
(NULL, 11, 6, N'CheckedIn', '2026-09-19T06:10:00', '2026-09-19T07:20:00'),
(NULL, 7,  6, N'CheckedIn', '2026-09-15T06:00:00', NULL);                  -- chua check-out
GO

-- =====================================================================
-- 23. NOTIFICATION BROADCAST + NOTIFICATION_RECIPIENT MAU
-- =====================================================================

INSERT INTO notifications (user_id, sender_id, title, content, type, created_at) VALUES
(NULL, 1, N'Lịch nghỉ Lễ Quốc Khánh 2/9',              N'Trung tâm tạm nghỉ ngày 02/09/2026, các lớp học sẽ dạy bù vào tuần sau.',                         N'System',   '2026-08-28T09:00:00'),
(NULL, 2, N'Lớp Yoga Buổi Sáng tạm nghỉ ngày 24/09',   N'Do bảo trì phòng tập, lớp Yoga Buổi Sáng ngày 24/09 sẽ dời sang 25/09 cùng khung giờ.',           N'Schedule', '2026-09-22T18:00:00');

-- Lay lai notification_id vua tao o tren de gan danh sach nguoi nhan (broadcast)
DECLARE @notiHoliday INT = (SELECT notification_id FROM notifications WHERE title = N'Lịch nghỉ Lễ Quốc Khánh 2/9');
DECLARE @notiYogaOff INT = (SELECT notification_id FROM notifications WHERE title = N'Lớp Yoga Buổi Sáng tạm nghỉ ngày 24/09');

INSERT INTO notification_recipients (notification_id, user_id, is_read, read_at) VALUES
(@notiHoliday, 2, 1, '2026-08-28T10:00:00'),  -- gui toan bo HLV
(@notiHoliday, 3, 0, NULL),
(@notiHoliday, 4, 1, '2026-08-29T08:00:00'),
(@notiHoliday, 5, 0, NULL),                    -- va toan bo Le tan
(@notiHoliday, 6, 0, NULL),
(@notiYogaOff, 7, 0, NULL),                    -- gui hoc vien da dang ky lop Yoga Buoi Sang
(@notiYogaOff, 9, 1, '2026-09-22T19:00:00');
GO

-- =====================================================================
-- 24. BUSINESS-RULE GUARDS
-- Dat sau seed de van cho phep bootstrap du lieu lich su mau.
-- =====================================================================

CREATE OR ALTER TRIGGER trg_classes_validate_business_rules
ON classes
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        LEFT JOIN deleted d ON d.class_id = i.class_id
        WHERE d.class_id IS NULL
          AND i.status = N'Open'
          AND i.start_date < CAST(GETDATE() AS date)
    )
        THROW 51001, 'A new Open class cannot start in the past.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN users u ON u.user_id = i.coach_id
        JOIN roles r ON r.role_id = u.role_id
        WHERE i.coach_id IS NOT NULL
          AND UPPER(r.role_name) <> N'COACH'
    )
        THROW 51002, 'The assigned user must have the Coach role.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN sessions s ON s.class_id = i.class_id
        JOIN rooms r ON r.room_id = s.room_id
        WHERE i.max_capacity > r.capacity
    )
        THROW 51003, 'Class capacity cannot exceed the capacity of a room used by its sessions.', 1;
END;
GO

CREATE OR ALTER TRIGGER trg_sessions_validate_business_rules
ON sessions
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        LEFT JOIN deleted d ON d.session_id = i.session_id
        WHERE d.session_id IS NULL
          AND i.session_date < CAST(GETDATE() AS date)
    )
        THROW 51004, 'A new session cannot be created in the past.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        LEFT JOIN deleted d ON d.session_id = i.session_id
        JOIN rooms r ON r.room_id = i.room_id
        WHERE r.status <> N'Available'
          AND (d.session_id IS NULL OR d.room_id <> i.room_id)
    )
        THROW 51005, 'New or reassigned sessions require an Available room.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN classes c ON c.class_id = i.class_id
        JOIN rooms r ON r.room_id = i.room_id
        WHERE c.max_capacity > r.capacity
    )
        THROW 51006, 'Class capacity cannot exceed room capacity.', 1;
END;
GO

CREATE OR ALTER TRIGGER trg_rooms_protect_future_sessions
ON rooms
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.room_id = i.room_id
        JOIN sessions s ON s.room_id = i.room_id
        WHERE i.status IN (N'Maintenance', N'Closed')
          AND i.status <> d.status
          AND s.status = N'Scheduled'
          AND s.session_date >= CAST(GETDATE() AS date)
    )
        THROW 51007, 'Reassign or cancel future scheduled sessions before making the room unavailable.', 1;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN sessions s ON s.room_id = i.room_id
        JOIN classes c ON c.class_id = s.class_id
        WHERE c.max_capacity > i.capacity
    )
        THROW 51008, 'Room capacity cannot be reduced below the capacity of a class using it.', 1;
END;
GO

CREATE OR ALTER TRIGGER trg_member_packages_require_active_package
ON member_packages
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        LEFT JOIN deleted d ON d.subscription_id = i.subscription_id
        JOIN membership_packages p ON p.package_id = i.package_id
        WHERE p.status = N'Inactive'
          AND i.status = N'Active'
          AND (
              d.subscription_id IS NULL
              OR d.status <> N'Active'
              OR d.package_id <> i.package_id
              OR d.start_date <> i.start_date
              OR d.end_date <> i.end_date
          )
    )
        THROW 51009, 'Inactive packages cannot be used for new registrations or renewals.', 1;
END;
GO
