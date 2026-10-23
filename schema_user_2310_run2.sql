-- ==============================================================================
-- (2) PHẦN 2: GÓI HỘI VIÊN, LỚP HỌC, GHI DANH, BUỔI HỌC, HÓA ĐƠN (RUN LẦN 2)
-- ==============================================================================

-- 2.1. Gói hội viên chuẩn & Kích hoạt gói mẫu
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

-- 2.2. Lớp học mẫu (Khóa Normal 12 buổi cố định & Template 1:1 Advanced)
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

-- Template Lớp 1:1 cho 9 môn
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

-- 2.3. Ghi danh học viên (Enrollments)
INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 25, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

INSERT INTO class_enrollments (member_id, class_id, status)
SELECT 28, c.class_id, 'Registered'
FROM classes c WHERE c.class_name = 'Lớp Pickleball Nhập Môn'
ON CONFLICT DO NOTHING;

-- 2.4. Buổi học mẫu
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

-- 2.5. Hóa đơn mẫu
INSERT INTO invoices (member_id, package_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref) VALUES
(4,  5, 2, 3500000, 'VNPay',        'Paid',    CURRENT_TIMESTAMP, 'VNPAY-TXN-20261001'),
(28, 2, 2, 1350000, 'BankTransfer', 'Paid',    CURRENT_TIMESTAMP - INTERVAL '15 days', 'BANK-TXN-20261002'),
(26, 1, 2, 500000,  NULL,           'Pending', NULL,              NULL)
ON CONFLICT DO NOTHING;

INSERT INTO invoices (member_id, class_id, receptionist_id, amount, payment_method, payment_status, payment_date, gateway_transaction_ref)
SELECT 25, c.class_id, 2, c.tuition_fee, 'Cash', 'Paid', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL
FROM classes c WHERE c.class_name = 'Lớp Cầu Lông Cơ Bản (K1)'
ON CONFLICT DO NOTHING;

-- 2.6. Đồng bộ Sequences
SELECT setval(pg_get_serial_sequence('classes', 'class_id'), GREATEST((SELECT COALESCE(MAX(class_id), 1) FROM classes), 1));
SELECT setval(pg_get_serial_sequence('sessions', 'session_id'), GREATEST((SELECT COALESCE(MAX(session_id), 1) FROM sessions), 1));
SELECT setval(pg_get_serial_sequence('invoices', 'invoice_id'), GREATEST((SELECT COALESCE(MAX(invoice_id), 1) FROM invoices), 1));
SELECT setval(pg_get_serial_sequence('member_packages', 'subscription_id'), GREATEST((SELECT COALESCE(MAX(subscription_id), 1) FROM member_packages), 1));

