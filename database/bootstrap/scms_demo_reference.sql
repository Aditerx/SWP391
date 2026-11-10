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
-- Required by SessionService's subjectId lookup. These are unbooked templates, not purchased rights/classes.
INSERT INTO classes(class_name,subject_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft)
SELECT x.name||' - Advanced (1:1)',s.subject_id,1,s.advanced_price,'Open','Advanced',12,1,FALSE
 FROM _scms_subjects x JOIN subjects s USING(slug) ORDER BY x.ordinal;
-- Published demo availability only; actual heat/holiday/conflict/credit checks remain in BE.
INSERT INTO coach_work_schedules(coach_id,accepting_new) SELECT user_id,TRUE FROM coaches;
INSERT INTO coach_work_slots(coach_id,slot_key)
SELECT c.user_id,d.day||':'||slot FROM coaches c
 CROSS JOIN (VALUES('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY'),('SUNDAY')) d(day)
 CROSS JOIN generate_series(1,7) slot;
