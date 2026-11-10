-- READ ONLY. Diagnose migration19 without adding columns, changing sessions or running a migration.
-- Default target is the session reported by Owner. Change only target_session_id if needed.
WITH
params AS (SELECT 164::integer AS target_session_id),
policy(activity, subject_slug, codes) AS (VALUES
 ('TENNIS','quan-vot-tennis',ARRAY['C01-TEN-01','C01-TEN-02']),
 ('FOOTBALL','bong-da',ARRAY['C01-FOOT-01','C01-FOOT-02']),
 ('ARCHERY','ban-cung',ARRAY['C01-ARC-01','C01-ARC-02']),
 ('BASKETBALL','bong-ro',ARRAY['C01-BR-01','C01-BR-02']),
 ('VOLLEYBALL','bong-chuyen',ARRAY['C01-BC-01','C01-BC-02']),
 ('PICKLEBALL','pickleball',ARRAY['C01-PICK-01','C01-PICK-02']),
 ('BADMINTON','cau-long',ARRAY['C01-BAD-01','C01-BAD-02']),
 ('TABLE_TENNIS','bong-ban',ARRAY['C01-TT-01','C01-TT-02','C01-TT-03','C01-TT-04']),
 ('SWIMMING','boi-loi',ARRAY['C01-SWIM-01','C01-SWIM-02'])
),
approved AS (
 SELECT (entry->>'room_id')::integer AS room_id, entry->>'room_code' AS approved_code, p.activity
 FROM public.scms_data_migration_audit a
 CROSS JOIN LATERAL jsonb_array_elements(a.after_data::jsonb->'rooms') entry
 JOIN policy p ON entry->>'room_code'=ANY(p.codes)
 WHERE a.migration_key='20261102_room_catalog_v1'
),
deployment AS (
 SELECT statement_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh' AS business_time,
  (SELECT count(*)=3 FROM information_schema.columns WHERE table_schema='public' AND table_name='system_logs'
   AND column_name IN ('audit_category','center_id','scope_source')) AS audit18_columns_present,
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='rooms' AND column_name='activity_type') AS room19_column_present,
  EXISTS(SELECT 1 FROM public.scms_data_migration_audit WHERE migration_key='20261109_room_activity_v1') AS room19_marker_present,
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes' AND column_name='pricing_draft') AS pricing20_column_present
),
room_state AS (
 SELECT r.*, a.approved_code, a.activity AS approved_activity,
  to_jsonb(r)->>'activity_type' AS stored_activity,
  CASE WHEN d.room19_marker_present THEN to_jsonb(r)->>'activity_type'
       ELSE coalesce(to_jsonb(r)->>'activity_type', a.activity) END AS prospective_activity
 FROM public.rooms r CROSS JOIN deployment d LEFT JOIN approved a USING(room_id)
),
target AS (
 SELECT s.session_id, s.session_date, s.start_time, s.end_time, s.status,
  to_jsonb(s)->>'attendance_finalized' AS attendance_finalized,
  to_jsonb(s)->>'center_invalidated' AS center_invalidated,
  to_jsonb(s)->>'booked_member_id' IS NOT NULL AS private_member_linked,
  s.class_id, to_jsonb(c)->>'class_type' AS class_type, c.status AS class_status, c.center_id AS class_center,
  c.subject_id, to_jsonb(sub)->>'subject_name' AS subject_name, sub.slug AS subject_slug, p.activity AS required_activity,
  (to_jsonb(c)->>'max_capacity')::integer AS class_capacity,
  s.room_id, r.room_code, r.room_name, r.center_id AS room_center,
  to_jsonb(r)->>'status' AS room_status, (to_jsonb(r)->>'capacity')::integer AS room_capacity,
  r.approved_code, r.stored_activity, r.prospective_activity,
  coalesce(c.center_id=1 AND s.status='Scheduled' AND s.session_date+s.end_time>d.business_time
    AND c.class_id IS NOT NULL AND sub.subject_id IS NOT NULL AND r.room_id IS NOT NULL, false) AS guard_applies,
  array_remove(ARRAY[
   CASE WHEN c.class_id IS NULL OR sub.subject_id IS NULL OR r.room_id IS NULL THEN 'MISSING_REFERENCED_ROW' END,
   CASE WHEN p.activity IS NULL THEN 'SUBJECT_NOT_IN_APPROVED_CATALOG' END,
   CASE WHEN r.prospective_activity IS NULL THEN 'ROOM_ACTIVITY_UNCONFIGURED' END,
   CASE WHEN r.center_id IS DISTINCT FROM c.center_id THEN 'ROOM_CENTER_MISMATCH' END,
   CASE WHEN r.prospective_activity IS NOT NULL AND p.activity IS NOT NULL
     AND r.prospective_activity<>p.activity THEN 'ROOM_SUBJECT_MISMATCH' END
  ],NULL) AS diagnostic_issues
 FROM public.sessions s JOIN params v ON s.session_id=v.target_session_id
 LEFT JOIN public.classes c ON c.class_id=s.class_id
 LEFT JOIN public.subjects sub ON sub.subject_id=c.subject_id
 LEFT JOIN policy p ON p.subject_slug=sub.slug
 LEFT JOIN room_state r ON r.room_id=s.room_id CROSS JOIN deployment d
)
SELECT jsonb_build_object(
 'deployment', to_jsonb(d),
 'approved_room_bindings', (SELECT count(*) FROM approved),
 'target_session_id', v.target_session_id,
 'session', (SELECT to_jsonb(t) || jsonb_build_object('guard_would_block',
    t.guard_applies AND (t.prospective_activity IS NULL OR t.room_center<>t.class_center
      OR t.prospective_activity IS DISTINCT FROM t.required_activity)) FROM target t),
 'same_sport_rooms', coalesce((
  SELECT jsonb_agg(jsonb_build_object(
    'room_id', r.room_id, 'room_code', r.room_code, 'room_name', r.room_name,
    'status', to_jsonb(r)->>'status', 'capacity', (to_jsonb(r)->>'capacity')::integer,
    'capacity_sufficient', (to_jsonb(r)->>'capacity')::integer >= t.class_capacity,
    'overlap_session_ids', ARRAY(SELECT other.session_id FROM public.sessions other
      WHERE other.room_id=r.room_id AND other.session_id<>t.session_id AND other.status<>'Cancelled'
       AND other.session_date=t.session_date AND other.start_time<t.end_time AND other.end_time>t.start_time
      ORDER BY other.session_id)
   ) ORDER BY r.room_id)
  FROM target t JOIN room_state r ON r.center_id=t.class_center AND r.prospective_activity=t.required_activity
 ), '[]'::jsonb),
 'limits', 'Read-only diagnosis, not permission to move a session. Room options do not verify every actor, consent, coach, member, holiday or entitlement rule.'
) AS room_activity_diagnosis
FROM deployment d CROSS JOIN params v;
