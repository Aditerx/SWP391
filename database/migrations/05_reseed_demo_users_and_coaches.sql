-- DEMO RESET ONLY. NOT A DATA-PRESERVING MIGRATION. Never run on a shared/live database.
-- Use only a disposable demo database. Explicit session opt-in is required before executing this file.
DO $$ BEGIN
    IF current_setting('scms.demo_reset_confirmed', true) IS DISTINCT FROM 'DISPOSABLE_DATABASE_ONLY' THEN
        RAISE EXCEPTION 'Blocked destructive demo reset. Use a disposable database and explicitly opt in for this session.';
    END IF;
END $$;

-- ==============================================================================
-- MIGRATION SCRIPT: 05_reseed_demo_users_and_coaches.sql
-- Hệ thống: Sports Center Management System (SCMS)
-- Mục tiêu: 
--   1. Làm sạch dữ liệu users mẫu cũ và các bảng con liên quan an toàn.
--   2. Tạo bộ tài khoản đại diện chuẩn hóa cho 5 Roles:
--      - Manager: manager@scms.com
--      - Receptionist: receptionist@scms.com (và thu.reception@scms.com)
--      - Coach đại diện: coach@scms.com
--      - Member đại diện: member@scms.com
--      - Admin: admin@scms.com
--   3. Tạo đội ngũ HLV chuẩn cho 9 bộ môn (mỗi môn 2 HLV: coach.<sport>@scms.com & coach.<sport>2@scms.com)
--   4. Tạo 4 Hội viên theo từng kịch bản nghiệp vụ (Advanced 12 Credit, Normal 12 buổi, New, Expired)
--   5. Đồng bộ toàn bộ mật khẩu là "Scms@2026" (BCrypt: $2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi)
--   6. Nạp bảng giá chuẩn 9 môn theo 01-API-CONTRACT-FE-BE.md và liên kết lớp học, gói tập, hóa đơn demo.
-- ==============================================================================

-- BƯỚC 1: XÓA DỮ LIỆU CŨ AN TOÀN TRÊN CÁC BẢNG LIÊN QUAN ĐẾN USER (CASCADE)
TRUNCATE TABLE 
    attendances,
    evaluations,
    ai_consultations,
    notification_recipients,
    notifications,
    support_requests,
    invoices,
    class_enrollments,
    member_packages,
    training_results,
    sessions,
    training_plans,
    classes,
    coach_specializations,
    coaches,
    members,
    receptionists,
    center_managers,
    email_otps,
    system_logs,
    users
RESTART IDENTITY CASCADE;

-- Đảm bảo Center 1 tồn tại
INSERT INTO centers (center_id, center_name)
VALUES (1, 'Sports Center 1')
ON CONFLICT (center_id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('centers', 'center_id'),
              GREATEST((SELECT COALESCE(MAX(center_id), 1) FROM centers), 1));

-- Đảm bảo 5 vai trò (Roles) chuẩn tồn tại
INSERT INTO roles (role_id, role_name, description) VALUES
(1, 'CenterManager', 'Quan ly trung tam - van hanh lop hoc, goi tap, bao cao'),
(2, 'Coach',         'Huan luyen vien'),
(3, 'Member',        'Thanh vien / hoc vien'),
(4, 'Receptionist',  'Nhan vien le tan'),
(5, 'Admin',         'Quan tri vien he thong - quan tri nguoi dung va phan quyen RBAC')
ON CONFLICT (role_name) DO UPDATE SET description = EXCLUDED.description;
SELECT setval(pg_get_serial_sequence('roles', 'role_id'),
              GREATEST((SELECT COALESCE(MAX(role_id), 1) FROM roles), 1));

-- ==============================================================================
-- BƯỚC 2: NẠP DANH SÁCH USERS MỚI (Mật khẩu: Scms@2026)
-- BCrypt: $2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi
-- ==============================================================================

INSERT INTO users (user_id, role_id, full_name, email, phone, password_hash, address, gender, date_of_birth, status, center_id) VALUES
-- 1. TÀI KHOẢN ĐẠI DIỆN CHO 5 VAI TRÒ
(1,  (SELECT role_id FROM roles WHERE role_name = 'CenterManager'), 'Nguyễn Văn An',          'manager@scms.com',          '0901234501', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '12 Nguyễn Huệ, Quận 1, TP.HCM',          'Male',   '1985-03-12', 'Active', 1),
(2,  (SELECT role_id FROM roles WHERE role_name = 'Receptionist'),  'Phạm Thị Thu',           'receptionist@scms.com',     '0901234502', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '45 Lê Lợi, Quận 1, TP.HCM',              'Female', '1998-04-22', 'Active', 1),
(3,  (SELECT role_id FROM roles WHERE role_name = 'Coach'),         'Nguyễn Văn Hùng',        'coach@scms.com',            '0901234503', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '78 CMT8, Quận 3, TP.HCM',                'Male',   '1990-05-15', 'Active', 1),
(4,  (SELECT role_id FROM roles WHERE role_name = 'Member'),        'Nguyễn Văn Thành',       'member@scms.com',           '0901234504', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '23 NTM Khai, Quận 1, TP.HCM',            'Male',   '1995-08-20', 'Active', 1),
(5,  (SELECT role_id FROM roles WHERE role_name = 'Admin'),         'Admin Hệ Thống',         'admin@scms.com',            '0901234500', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Văn phòng Trung tâm, Q1, TP.HCM',        'Other',  '1980-01-01', 'Active', 1),

-- 2. LỄ TÂN PHỤ TRỢ (Khớp mock Frontend)
(6,  (SELECT role_id FROM roles WHERE role_name = 'Receptionist'),  'Phạm Thị Thu (Ca Chiều)','thu.reception@scms.com',   '0901234506', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '56 Điện Biên Phủ, Bình Thạnh, TP.HCM',  'Female', '1998-04-22', 'Active', 1),

-- 3. ĐỘI NGŨ COACH CHO 9 BỘ MÔN CHUẨN (Mỗi môn 2 Coach)
-- Bóng đá (Outdoor)
(7,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Trần Phi',           'coach.football@scms.com',   '0901234507', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân bóng đá trung tâm',                 'Male',   '1988-11-20', 'Active', 1),
(8,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Tuấn Vũ',        'coach.football2@scms.com',  '0901234508', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân bóng đá trung tâm',                 'Male',   '1992-06-14', 'Active', 1),

-- Quần vợt (Outdoor)
(9,  (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Huỳnh Diễm My',         'coach.tennis@scms.com',     '0901234509', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu sân Tennis ngoài trời',              'Female', '1993-09-12', 'Active', 1),
(10, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Quốc Bảo',         'coach.tennis2@scms.com',    '0901234510', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu sân Tennis ngoài trời',              'Male',   '1989-03-25', 'Active', 1),

-- Bơi lội (Outdoor)
(11, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Phương Anh',         'coach.swimming@scms.com',   '0901234511', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu thể thao dưới nước',                'Female', '1995-07-08', 'Active', 1),
(12, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Đỗ Hải Đăng',           'coach.swimming2@scms.com',  '0901234512', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Khu thể thao dưới nước',                'Male',   '1991-12-30', 'Active', 1),

-- Bắn cung (Outdoor)
(13, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Nguyễn Thiên Phúc',  'coach.archery@scms.com',    '0901234513', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Trường bắn ngoài trời',                  'Male',   '1994-04-18', 'Active', 1),
(14, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Hoàng Minh Tâm',        'coach.archery2@scms.com',   '0901234514', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Trường bắn ngoài trời',                  'Male',   '1990-10-05', 'Active', 1),

-- Cầu lông (Indoor)
(15, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Thị Hương',        'coach.badminton@scms.com',  '0901234515', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu Cầu lông',                  'Female', '1992-07-20', 'Active', 1),
(16, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Minh Khang',         'coach.badminton2@scms.com', '0901234516', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu Cầu lông',                  'Male',   '1996-01-22', 'Active', 1),

-- Bóng bàn (Indoor)
(17, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Lê Minh Khoa',          'coach.tabletennis@scms.com','0901234517', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Phòng Bóng bàn tiêu chuẩn',              'Male',   '1990-01-15', 'Active', 1),
(18, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Đức Trí',        'coach.tabletennis2@scms.com','0901234518','$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Phòng Bóng bàn tiêu chuẩn',              'Male',   '1993-08-19', 'Active', 1),

-- Bóng rổ (Indoor)
(19, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Trần Anh Dũng',         'coach.basketball@scms.com', '0901234519', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1987-05-11', 'Active', 1),
(20, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Phan Hoàng Nam',        'coach.basketball2@scms.com','0901234520', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1994-09-02', 'Active', 1),

-- Bóng chuyền (Indoor)
(21, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Nguyễn Văn Hoàng',      'coach.volleyball@scms.com', '0901234521', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1989-12-10', 'Active', 1),
(22, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Đặng Tuấn Anh',         'coach.volleyball2@scms.com','0901234522', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Nhà thi đấu đa năng',                    'Male',   '1995-03-17', 'Active', 1),

-- Pickleball (Indoor)
(23, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Phan Thị Lan',          'coach.pickleball@scms.com', '0901234523', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân Pickleball có mái che',              'Female', '1994-11-05', 'Active', 1),
(24, (SELECT role_id FROM roles WHERE role_name = 'Coach'), 'Vũ Thị Thảo',           'coach.pickleball2@scms.com','0901234524', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', 'Sân Pickleball có mái che',              'Female', '1997-04-28', 'Active', 1),

-- 4. HỘI VIÊN PHỤC VỤ CÁC KỊCH BẢN DEMO NGHIỆP VỤ
(25, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Hoàng Thị Oanh',        'member.normal@scms.com',    '0901234525', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '12 Trần Hưng Đạo, Q5, TP.HCM',       'Female', '1996-06-18', 'Active', 1),
(26, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Bùi Văn Phúc',          'member.new@scms.com',       '0901234526', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '34 Nguyễn Trãi, Q5, TP.HCM',         'Male',   '1993-02-27', 'Active', 1),
(27, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Đặng Văn Sơn',          'member.expired@scms.com',   '0901234527', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '90 Ba Tháng Hai, Q10, TP.HCM',       'Male',   '1988-08-08', 'Active', 1),
(28, (SELECT role_id FROM roles WHERE role_name = 'Member'), 'Ngô Thị Quyên',         'member.active2@scms.com',   '0901234528', '$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi', '67 Lý Thường Kiệt, Q10, TP.HCM',     'Female', '1999-12-01', 'Active', 1)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- Đồng bộ sequence bảng users
SELECT setval(pg_get_serial_sequence('users', 'user_id'),
              GREATEST((SELECT COALESCE(MAX(user_id), 1) FROM users), 1));

-- ==============================================================================
-- BƯỚC 3: PHÂN LOẠI CHI TIẾT TỪNG VAI TRÒ (SUBTYPES)
-- ==============================================================================

-- 3.1. Center Manager
INSERT INTO center_managers (user_id) VALUES (1) ON CONFLICT (user_id) DO NOTHING;

-- 3.2. Receptionists
INSERT INTO receptionists (user_id) VALUES (2), (6) ON CONFLICT (user_id) DO NOTHING;

-- 3.3. Coaches & Chuyên môn
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

-- Bảng danh mục chuyên môn và liên kết
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

-- 3.4. Members
INSERT INTO members (user_id, goal, health_note, join_date, registered_by) VALUES
(4,  'Tập luyện nâng cao 1:1, cải thiện thể lực toàn diện', NULL, '2026-08-01', 2),
(25, 'Rèn luyện phản xạ, giảm căng thẳng qua môn Cầu lông', NULL, '2026-08-15', 2),
(26, 'Làm quen môi trường thể thao, đăng ký môn mới', NULL, CURRENT_DATE, 2),
(27, 'Hồi phục thể lực sau chấn thương nhẹ', 'Cần khởi động kỹ khớp gối', '2026-06-01', 2),
(28, 'Yêu thích Pickleball, muốn thi đấu phong trào', NULL, '2026-09-01', 6)
ON CONFLICT (user_id) DO UPDATE SET
    goal = EXCLUDED.goal,
    health_note = EXCLUDED.health_note;

-- ==============================================================================
-- BƯỚC 4: BẢNG GIÁ 9 MÔN CHUẨN (MEMBER.PDF) & PHÒNG TẬP
-- ==============================================================================

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

-- ==============================================================================
-- BƯỚC 5: GÓI HỘI VIÊN, ĐĂNG KÝ GÓI & LỚP HỌC MẪU
-- ==============================================================================

-- Gói tập
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

-- Gán gói tập cho Member:
-- Member 4 (member@scms.com): Có sẵn Gói 1:1 Active, 12 credit, 45 ngày để test book ca
-- Member 27: Có gói 1:1 đã hết hạn
-- Member 28: Có gói 3 tháng tiêu chuẩn
INSERT INTO member_packages (member_id, package_id, start_date, end_date, remaining_credit, validity_days, status) VALUES
(4,  5, CURRENT_DATE, CURRENT_DATE + INTERVAL '45 days', 12, 45, 'Active'),
(27, 5, CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE - INTERVAL '15 days', 0, 45, 'Expired'),
(28, 2, '2026-09-01', '2026-11-30', 0, 90, 'Active')
ON CONFLICT DO NOTHING;

-- Tắt trigger tạm thời để nạp lớp học và buổi học
ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;

-- Lớp học nhóm Normal mẫu (Khóa 12 buổi cố định)
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
FROM subjects s WHERE s.slug = 'bong-ban';

-- Template Lớp học 1:1 Advanced cho 9 môn thể thao
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

ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;

-- Ghi danh học viên vào lớp (Enrollments)
-- Member 25 (member.normal@scms.com) vào lớp Cầu Lông (để test xem lịch tĩnh & chặn hủy)
INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 25, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

-- Member 28 vào lớp Pickleball
INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 28, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Pickleball Nhập Môn'
ON CONFLICT DO NOTHING;

-- Buổi học mẫu (Sessions)
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

-- Hóa đơn mẫu (Invoices)
INSERT INTO invoices (member_id, package_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref) VALUES
(4,  5, 2, 3500000, 'VNPay',        'Paid',    CURRENT_TIMESTAMP, 'VNPAY-TXN-20261001'),
(28, 2, 2, 1350000, 'BankTransfer', 'Paid',    CURRENT_TIMESTAMP - INTERVAL '15 days', 'BANK-TXN-20261002'),
(26, 1, 2, 500000,  NULL,           'Pending', NULL,              NULL)
ON CONFLICT DO NOTHING;

INSERT INTO invoices (member_id, class_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref)
SELECT 25, c.class_id, 2, c.tuition_fee, 'Cash', 'Paid', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

-- Đồng bộ tất cả các sequence
SELECT setval(pg_get_serial_sequence('classes', 'class_id'), GREATEST((SELECT COALESCE(MAX(class_id), 1) FROM classes), 1));
SELECT setval(pg_get_serial_sequence('sessions', 'session_id'), GREATEST((SELECT COALESCE(MAX(session_id), 1) FROM sessions), 1));
SELECT setval(pg_get_serial_sequence('invoices', 'invoice_id'), GREATEST((SELECT COALESCE(MAX(invoice_id), 1) FROM invoices), 1));
SELECT setval(pg_get_serial_sequence('member_packages', 'subscription_id'), GREATEST((SELECT COALESCE(MAX(subscription_id), 1) FROM member_packages), 1));

