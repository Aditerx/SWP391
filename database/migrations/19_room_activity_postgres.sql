-- Physical court type for Owner02/11's twenty resources. No rooms.subject_id, no runtime name/code inference.
BEGIN;
DO $$ BEGIN
 IF to_regclass('public.rooms') IS NULL OR to_regclass('public.scms_data_migration_audit') IS NULL THEN
  RAISE EXCEPTION 'Migration13 room catalog baseline is required';
 END IF;
END $$;
SELECT center_id FROM public.centers WHERE center_id=1 FOR UPDATE;
LOCK TABLE public.rooms, public.sessions, public.classes IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE public.rooms ADD COLUMN IF NOT EXISTS activity_type VARCHAR(24);
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='rooms'
     AND column_name='activity_type' AND data_type='character varying' AND character_maximum_length=24 AND is_nullable='YES') THEN
  RAISE EXCEPTION 'Incompatible rooms.activity_type; reconcile before deploy';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.rooms'::regclass AND conname='ck_rooms_activity_type') THEN
  ALTER TABLE public.rooms ADD CONSTRAINT ck_rooms_activity_type CHECK(activity_type IN
   ('TENNIS','FOOTBALL','ARCHERY','BASKETBALL','VOLLEYBALL','PICKLEBALL','BADMINTON','TABLE_TENNIS','SWIMMING'));
 END IF;
END $$;
DO $activity$
DECLARE snapshot JSONB; before_state TEXT;
BEGIN
 IF EXISTS(SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261109_room_activity_v1') THEN RETURN; END IF;
 SELECT after_data::jsonb INTO snapshot FROM public.scms_data_migration_audit WHERE migration_key='20261102_room_catalog_v1';
 IF snapshot IS NULL THEN RAISE EXCEPTION 'Approved immutable room catalog snapshot is required; do not infer from current room names/codes'; END IF;
 CREATE TEMP TABLE _approved_activity(room_id INT PRIMARY KEY, activity TEXT NOT NULL) ON COMMIT DROP;
 -- These codes identify rows in the immutable approved snapshot, not the current editable catalog.
 INSERT INTO _approved_activity
 SELECT (r->>'room_id')::int, c.activity FROM jsonb_array_elements(snapshot->'rooms') r
 JOIN (VALUES
  ('C01-TEN-01','TENNIS'),('C01-TEN-02','TENNIS'),('C01-FOOT-01','FOOTBALL'),('C01-FOOT-02','FOOTBALL'),
  ('C01-ARC-01','ARCHERY'),('C01-ARC-02','ARCHERY'),('C01-BR-01','BASKETBALL'),('C01-BR-02','BASKETBALL'),
  ('C01-BC-01','VOLLEYBALL'),('C01-BC-02','VOLLEYBALL'),('C01-PICK-01','PICKLEBALL'),('C01-PICK-02','PICKLEBALL'),
  ('C01-BAD-01','BADMINTON'),('C01-BAD-02','BADMINTON'),('C01-TT-01','TABLE_TENNIS'),('C01-TT-02','TABLE_TENNIS'),
  ('C01-TT-03','TABLE_TENNIS'),('C01-TT-04','TABLE_TENNIS'),('C01-SWIM-01','SWIMMING'),('C01-SWIM-02','SWIMMING')
 ) c(code,activity) ON r->>'room_code'=c.code;
 IF (SELECT count(*) FROM _approved_activity)<>20 OR
    (SELECT count(*) FROM _approved_activity a JOIN public.rooms r USING(room_id) WHERE r.center_id=1)<>20 THEN
  RAISE EXCEPTION 'Approved twenty room IDs no longer match center1; reconcile explicitly';
 END IF;
 IF EXISTS(SELECT 1 FROM _approved_activity a JOIN public.rooms r USING(room_id) WHERE r.activity_type IS NOT NULL AND r.activity_type<>a.activity) THEN
  RAISE EXCEPTION 'Existing activity conflicts with the approved snapshot; reconcile explicitly';
 END IF;
 SELECT jsonb_agg(to_jsonb(r) ORDER BY room_id)::text INTO before_state FROM public.rooms r JOIN _approved_activity USING(room_id);
 UPDATE public.rooms r SET activity_type=a.activity FROM _approved_activity a WHERE r.room_id=a.room_id AND r.activity_type IS NULL;
 INSERT INTO public.scms_data_migration_audit VALUES('20261109_room_activity_v1',clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh',before_state,
  (SELECT jsonb_agg(to_jsonb(r) ORDER BY room_id)::text FROM public.rooms r JOIN _approved_activity USING(room_id)));
END $activity$;
DO $$ DECLARE issues TEXT; BEGIN
 SELECT string_agg(s.session_id::text,',' ORDER BY s.session_id) INTO issues
 FROM public.sessions s JOIN public.classes c USING(class_id) JOIN public.subjects sub USING(subject_id) JOIN public.rooms r USING(room_id)
 WHERE c.center_id=1 AND s.status='Scheduled' AND s.session_date+s.end_time > clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh'
 AND (r.activity_type IS NULL OR r.center_id<>c.center_id OR
  r.activity_type IS DISTINCT FROM CASE sub.slug WHEN 'quan-vot-tennis' THEN 'TENNIS' WHEN 'bong-da' THEN 'FOOTBALL'
   WHEN 'ban-cung' THEN 'ARCHERY' WHEN 'bong-ro' THEN 'BASKETBALL' WHEN 'bong-chuyen' THEN 'VOLLEYBALL'
   WHEN 'pickleball' THEN 'PICKLEBALL' WHEN 'cau-long' THEN 'BADMINTON' WHEN 'bong-ban' THEN 'TABLE_TENNIS' WHEN 'boi-loi' THEN 'SWIMMING' END);
 IF issues IS NOT NULL THEN RAISE EXCEPTION 'ROOM_ACTIVITY_SCHEDULE_CONFLICT: Reconcile existing future sessions before deploy' USING DETAIL=issues; END IF;
END $$;
-- Historical sessions, links, names, codes, prices and invoices are untouched. Replay preserves later authorized edits.
COMMIT;
