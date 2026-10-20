-- ==============================================================================

-- MIGRATION SCRIPT: 04_migration_member_flow_v2.sql

-- Hệ thống: Sports Center Management System (SCMS)

-- Mục tiêu: Chuẩn hóa CSDL theo đặc tả Member.pdf:

--   1. Thêm 2 mức giá cố định vào subjects (normal_price, advanced_price).

--   2. Phân loại sân bãi rooms (facility_type: Indoor/Outdoor).

--   3. Phân định classes (class_type: Normal/Advanced, total_sessions: 12, max_capacity).

--   4. Quản lý gói tín chỉ membership_packages & member_packages (total_credit, remaining_credit, validity_days).

--   5. Hỗ trợ đặt lịch 1:1 trên sessions theo Phương án B (booked_member_id, coach_id).

--   6. Tạo bảng holidays quản lý ngày lễ/tết với bộ tag NATIONAL_FIXED, LUNAR_FESTIVAL, CENTER_EVENT.

--   7. Seed dữ liệu 9 bộ môn chuẩn, phòng tập, ngày lễ và lớp 1:1 mẫu.

-- ==============================================================================



-- 1. BẢNG SUBJECTS (BỘ MÔN)

ALTER TABLE subjects ADD COLUMN IF NOT EXISTS normal_price NUMERIC(12,2) DEFAULT 0 CHECK (normal_price >= 0);

ALTER TABLE subjects ADD COLUMN IF NOT EXISTS advanced_price NUMERIC(12,2) DEFAULT 0 CHECK (advanced_price >= 0);



-- Cập nhật / Seed 9 bộ môn chuẩn theo bảng học phí tại Trang 7 Member.pdf

INSERT INTO subjects (subject_name, description, normal_price, advanced_price, status, slug) VALUES

('Quần vợt (Tennis)', 'Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực', 2500000, 8000000, 'Active', 'quan-vot-tennis'),

('Bơi lội',            'Môn thể thao dưới nước, phát triển toàn diện cơ thể',     1500000, 4500000, 'Active', 'boi-loi'),

('Pickleball',         'Môn thể thao thời thượng, kết hợp tennis, bóng bàn',     1800000, 5000000, 'Active', 'pickleball'),

('Bắn cung',           'Rèn luyện sự tập trung cao độ và tính kiên nhẫn',        2000000, 6000000, 'Active', 'ban-cung'),

('Bóng rổ',            'Môn thể thao đồng đội tốc độ và sức bật',                1200000, 3500000, 'Active', 'bong-ro'),

('Bóng đá',            'Môn thể thao vua, rèn luyện thể lực và phối hợp',         1000000, 3500000, 'Active', 'bong-da'),

('Bóng chuyền',        'Phối hợp đồng đội và kỹ thuật phản xạ trên lưới',        1000000, 3500000, 'Active', 'bong-chuyen'),

('Cầu lông',           'Môn thể thao nhanh nhẹn, linh hoạt trong nhà',            900000, 3000000, 'Active', 'cau-long'),

('Bóng bàn',           'Phản xạ nhanh, khéo léo với nhịp độ cao',                 800000, 2500000, 'Active', 'bong-ban')

ON CONFLICT (slug) DO UPDATE SET

    normal_price = EXCLUDED.normal_price,

    advanced_price = EXCLUDED.advanced_price,

    description = EXCLUDED.description;



-- 2. BẢNG ROOMS (CƠ SỞ VẬT CHẤT / SÂN BÃI)

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS facility_type VARCHAR(20) DEFAULT 'Indoor';

ALTER TABLE rooms DROP CONSTRAINT IF EXISTS chk_facility_type;

ALTER TABLE rooms ADD CONSTRAINT chk_facility_type CHECK (facility_type IN ('Indoor', 'Outdoor'));



-- Seed các sân tập chuẩn phân loại Indoor / Outdoor

INSERT INTO rooms (room_name, location, capacity, status, facility_type, center_id) VALUES

('Sân Tennis ngoài trời 1', 'Khu thể thao ngoài trời', 4,  'Available', 'Outdoor', 1),

('Hồ bơi ngoài trời tiêu chuẩn', 'Khu thể thao dưới nước', 20, 'Available', 'Outdoor', 1),

('Trường bắn ngoài trời',   'Khu vực an toàn ngoài trời', 10, 'Available', 'Outdoor', 1),

('Sân cỏ ngoài trời (Bóng đá)', 'Khu sân bóng đá cỏ nhân tạo', 30, 'Available', 'Outdoor', 1),

('Nhà thi đấu Cầu lông (Sàn thảm)', 'Tầng 1 Nhà thi đấu', 20, 'Available', 'Indoor', 1),

('Phòng Bóng bàn tiêu chuẩn', 'Tầng 2 Khu trong nhà', 15, 'Available', 'Indoor', 1),

('Nhà thi đấu đa năng (Bóng rổ & Chuyền)', 'Tầng 1 Nhà thi đấu lớn', 30, 'Available', 'Indoor', 1),

('Sân Pickleball có mái che', 'Khu liên hợp thể thao', 12, 'Available', 'Indoor', 1)

ON CONFLICT DO NOTHING;



-- 3. BẢNG CLASSES (LỚP HỌC)

-- Sửa Trigger fn_classes_validate để tránh chặn UPDATE các trường khác của các lớp học cũ trong quá khứ

CREATE OR REPLACE FUNCTION fn_classes_validate()

RETURNS TRIGGER LANGUAGE plpgsql AS $$

BEGIN

    -- Chỉ chặn start_date trong quá khứ khi INSERT mới hoặc khi đổi start_date sang ngày khác

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



ALTER TABLE classes ADD COLUMN IF NOT EXISTS class_type VARCHAR(20) DEFAULT 'Normal';

ALTER TABLE classes ADD COLUMN IF NOT EXISTS total_sessions INT DEFAULT 12;



ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_class_type;

ALTER TABLE classes ADD CONSTRAINT chk_class_type CHECK (class_type IN ('Normal', 'Advanced'));



-- Tạm vô hiệu hóa trigger để cập nhật chuẩn hóa dữ liệu cũ an toàn

ALTER TABLE classes DISABLE TRIGGER trg_classes_validate;



UPDATE classes SET max_capacity = 15 WHERE (class_type = 'Normal' OR class_type IS NULL) AND max_capacity > 15;



ALTER TABLE classes ENABLE TRIGGER trg_classes_validate;



ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_class_capacity;

ALTER TABLE classes ADD CONSTRAINT chk_class_capacity 

    CHECK ((class_type = 'Normal' AND (max_capacity IS NULL OR max_capacity <= 15))

        OR (class_type = 'Advanced' AND (max_capacity IS NULL OR max_capacity = 1)));



-- 4. BẢNG MEMBERSHIP_PACKAGES (GÓI KHUÔN MẪU)

ALTER TABLE membership_packages ADD COLUMN IF NOT EXISTS package_type VARCHAR(20) DEFAULT 'Normal';

ALTER TABLE membership_packages ADD COLUMN IF NOT EXISTS total_credit INT DEFAULT 12;



ALTER TABLE membership_packages DROP CONSTRAINT IF EXISTS chk_package_type;

ALTER TABLE membership_packages ADD CONSTRAINT chk_package_type CHECK (package_type IN ('Normal', 'Advanced'));



-- 5. BẢNG MEMBER_PACKAGES (GÓI HỘI VIÊN ĐĂNG KÝ)

ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS remaining_credit INT DEFAULT 0;

ALTER TABLE member_packages ADD COLUMN IF NOT EXISTS validity_days INT DEFAULT 45;



-- 6. BẢNG SESSIONS (BUỔI HỌC - HỖ TRỢ ĐẶT LỊCH 1:1 PHƯƠNG ÁN B)

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS booked_member_id INT NULL REFERENCES members(user_id);

ALTER TABLE sessions ADD COLUMN IF NOT EXISTS coach_id INT NULL REFERENCES coaches(user_id);



CREATE INDEX IF NOT EXISTS idx_sessions_booked_member ON sessions(booked_member_id);

CREATE INDEX IF NOT EXISTS idx_sessions_coach ON sessions(coach_id);



-- 7. BẢNG HOLIDAYS (QUẢN LÝ LỊCH NGHỈ LỄ/TẾT)

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



-- Seed danh mục ngày lễ chuẩn Việt Nam

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



-- 8. TẠO CÁC LỚP 1:1 TEMPLATE CHO 9 BỘ MÔN (PHƯƠNG ÁN B)

-- Tạm thời tắt trigger kiểm tra nếu có

DO $$

BEGIN

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

END $$;



