-- Owner02/11 explicitly approved these seven class IDs/subjects/old -> new limits.
-- Specific to the reviewed center1 baseline, NOT an automatic migration for every DB.
-- In 0211-1 this runs in the SAME transaction as room migration13.
BEGIN;
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
COMMIT;
