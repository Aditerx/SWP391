-- RUN 1 OF 2: 0211-1.sql. Paste this ENTIRE file into Supabase SQL Editor.
-- Existing baseline29/10 DB only. Backup first; pause schedule writes.
-- Contains preflight + Owner-approved seven class-limit updates + migration13. No rebuild or seed.
-- If any guard fails: stop, resolve the reported issue, and rerun this file.
-- Run 0212-2.sql ONLY after this file succeeds.
-- Canonical sources: database/migrations/13_owner_class_capacity_20261102_postgres.sql
-- and database/migrations/13_room_catalog_postgres.sql.
-- Temporary root copy: may be deleted after successful execution/postflight.
BEGIN;
SET LOCAL search_path = public, pg_catalog;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
SET LOCAL lock_timeout = '10s';

-- Read-only preview; migration guards below recheck before commit.
SELECT c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name,
       count(*) AS scheduled_sessions,min(s.session_date+s.start_time) AS next_start,
       (SELECT count(*) FROM class_enrollments e WHERE e.class_id=c.class_id
        AND e.status IN ('Registered','Pending')) AS held_seats
FROM sessions s JOIN classes c USING(class_id) JOIN subjects su USING(subject_id)
JOIN rooms r USING(room_id)
WHERE r.center_id=1 AND s.status='Scheduled' AND NOT s.center_invalidated
  AND s.session_date>=(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
GROUP BY c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name
ORDER BY next_start,c.class_id;

-- Owner02/11 explicitly approved these seven class IDs/subjects/old -> new limits.
-- Specific to the reviewed center1 baseline, NOT an automatic migration for every DB.
-- In 0211-1 this runs in the SAME transaction as room migration13.
SELECT center_id FROM centers WHERE center_id=1 FOR UPDATE;
LOCK TABLE rooms, sessions, classes, class_enrollments IN SHARE ROW EXCLUSIVE MODE;
CREATE TABLE IF NOT EXISTS scms_data_migration_audit (
 migration_key VARCHAR(100) PRIMARY KEY,captured_at TIMESTAMP NOT NULL,before_data TEXT NOT NULL,after_data TEXT NOT NULL);
DO $owner_capacity$
DECLARE issues TEXT; before_state TEXT; after_state TEXT; captured TIMESTAMP:=clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh';
BEGIN
 IF EXISTS(SELECT 1 FROM scms_data_migration_audit WHERE migration_key='20261102_owner_normal_class_capacity_v1') THEN RETURN; END IF;
 CREATE TEMP TABLE _owner_class_caps(class_id INT PRIMARY KEY,slug TEXT,old_capacity INT,new_capacity INT) ON COMMIT DROP;
 INSERT INTO _owner_class_caps VALUES
  (11,'boi-loi',15,10),(12,'pickleball',12,4),(13,'ban-cung',10,5),
  (14,'bong-ro',15,8),(16,'bong-chuyen',15,8),(17,'cau-long',15,4),(18,'bong-ban',15,2);
 SELECT jsonb_agg(to_jsonb(problem) ORDER BY class_id)::text INTO issues FROM (
  SELECT a.class_id,c.class_name,c.center_id,c.class_type,s.slug AS actual_subject,a.slug AS expected_subject,
   c.max_capacity,a.old_capacity AS approved_old_capacity,a.new_capacity AS approved_new_capacity,seats.held_seats,
   array_remove(ARRAY[
    CASE WHEN c.class_id IS NULL THEN 'CLASS_NOT_FOUND' END,
    CASE WHEN c.class_id IS NOT NULL AND c.center_id IS DISTINCT FROM 1 THEN 'WRONG_CENTER' END,
    CASE WHEN c.class_id IS NOT NULL AND c.class_type IS DISTINCT FROM 'Normal' THEN 'WRONG_CLASS_TYPE' END,
    CASE WHEN c.class_id IS NOT NULL AND s.slug IS DISTINCT FROM a.slug THEN 'WRONG_SUBJECT' END,
    CASE WHEN c.class_id IS NOT NULL AND (c.max_capacity IS NULL OR c.max_capacity NOT IN(a.old_capacity,a.new_capacity)) THEN 'CLASS_CAPACITY_CHANGED_SINCE_APPROVAL' END,
    CASE WHEN seats.held_seats>a.new_capacity THEN 'HELD_SEATS_EXCEED_APPROVED_CAPACITY' END
   ],NULL) AS reasons
  FROM _owner_class_caps a LEFT JOIN classes c USING(class_id) LEFT JOIN subjects s ON s.subject_id=c.subject_id
  CROSS JOIN LATERAL(SELECT count(*) AS held_seats FROM class_enrollments e
   WHERE e.class_id=a.class_id AND e.status IN('Registered','Pending')) seats
  WHERE c.class_id IS NULL OR c.center_id IS DISTINCT FROM 1 OR c.class_type IS DISTINCT FROM 'Normal'
   OR s.slug IS DISTINCT FROM a.slug OR c.max_capacity IS NULL OR c.max_capacity NOT IN(a.old_capacity,a.new_capacity)
   OR seats.held_seats>a.new_capacity
 ) problem;
 IF issues IS NOT NULL THEN
  RAISE EXCEPTION 'OWNER_CLASS_CAPACITY_REVIEW: reviewed class identity/capacity or held seats no longer match approval'
   USING DETAIL=issues,HINT='Only the seven approved center1 Normal classes may change; do not remove students or relax this guard.';
 END IF;
 SELECT jsonb_build_object(
  'approval','Owner02/11 seven class capacity limits; unrelated fields unchanged',
  'classes',(SELECT jsonb_agg(to_jsonb(c) ORDER BY c.class_id) FROM classes c JOIN _owner_class_caps a USING(class_id)),
  'held_seats',(SELECT jsonb_agg(to_jsonb(x) ORDER BY class_id) FROM (
   SELECT a.class_id,count(e.enrollment_id) FILTER(WHERE e.status IN('Registered','Pending')) AS held_seats
   FROM _owner_class_caps a LEFT JOIN class_enrollments e USING(class_id) GROUP BY a.class_id) x)
 )::text INTO before_state;
 UPDATE classes c SET max_capacity=a.new_capacity FROM _owner_class_caps a
 WHERE c.class_id=a.class_id AND c.max_capacity IS DISTINCT FROM a.new_capacity;
 IF EXISTS(SELECT 1 FROM _owner_class_caps a JOIN classes c USING(class_id) WHERE c.max_capacity IS DISTINCT FROM a.new_capacity) THEN
  RAISE EXCEPTION 'OWNER_CLASS_CAPACITY_POSTFLIGHT: approved limits were not saved';
 END IF;
 IF (SELECT jsonb_agg(to_jsonb(c)-'max_capacity' ORDER BY c.class_id) FROM classes c JOIN _owner_class_caps a USING(class_id))
  IS DISTINCT FROM (SELECT jsonb_agg(entry-'max_capacity' ORDER BY (entry->>'class_id')::int)
   FROM jsonb_array_elements(before_state::jsonb->'classes') AS saved(entry)) THEN
  RAISE EXCEPTION 'OWNER_CLASS_CAPACITY_POSTFLIGHT: an unapproved class field changed';
 END IF;
 SELECT jsonb_build_object(
  'classes',(SELECT jsonb_agg(to_jsonb(c) ORDER BY c.class_id) FROM classes c JOIN _owner_class_caps a USING(class_id)),
  'held_seats',(SELECT jsonb_agg(to_jsonb(x) ORDER BY class_id) FROM (
   SELECT a.class_id,count(e.enrollment_id) FILTER(WHERE e.status IN('Registered','Pending')) AS held_seats
   FROM _owner_class_caps a LEFT JOIN class_enrollments e USING(class_id) GROUP BY a.class_id) x)
 )::text INTO after_state;
 INSERT INTO scms_data_migration_audit(migration_key,captured_at,before_data,after_data)
 VALUES('20261102_owner_normal_class_capacity_v1',captured,before_state,after_state);
END $owner_capacity$;

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
COMMIT;

SELECT '0211-1 SUCCESS: migration13 committed or previously applied' AS result;
SELECT room_code,room_name,location,facility_type,capacity,status
FROM public.rooms WHERE center_id=1 AND room_code NOT LIKE 'C01-LEGACY-%' ORDER BY room_code;

-- Owner-approved class-limit postflight.
SELECT class_id,class_name,max_capacity FROM public.classes
WHERE class_id IN(11,12,13,14,16,17,18) ORDER BY class_id;
