-- Owner-run cleanup for the old nine subject-level Advanced templates.
-- Review the final deleted/retained IDs. This is NOT a reset or a history cleanup.
-- Run only after migration25, with BE writers/jobs stopped until the matching BE resumes:
-- the old BE still looks up a subject template and must not book during the transition.
-- Never replace these guards with CASCADE, or delete sessions/invoices/credits to force cleanup.
BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='60s';
DO $requires_private_classes$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='advanced_registrations'
      AND column_name='class_id' AND data_type='integer')
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.advanced_registrations'::regclass
      AND conname='fk_advanced_registration_class' AND contype='f' AND confrelid='public.classes'::regclass) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLEANUP_REQUIRES_MIGRATION25: upgrade private-class links with writers stopped before deleting templates';
 END IF;
END $requires_private_classes$;
LOCK TABLE public.classes IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE _scms_advanced_candidates ON COMMIT DROP AS
SELECT c.class_id,c.subject_id,c.class_name
FROM public.classes c JOIN public.subjects s ON s.subject_id=c.subject_id
WHERE c.class_type='Advanced' AND c.coach_id IS NULL
  AND c.status='Open' AND c.max_capacity=1 AND c.total_sessions=12
  AND c.start_date IS NULL AND c.end_date IS NULL AND NOT c.pricing_draft
  AND (c.class_name IN (s.subject_name||' - Advanced',s.subject_name||' Advanced')
       OR (s.slug='quan-vot-tennis' AND c.class_name IN ('Tennis - Advanced','Tennis Advanced')));

CREATE TEMP TABLE _scms_advanced_references(
 class_id INTEGER NOT NULL,source TEXT NOT NULL,referencing_rows BIGINT NOT NULL
) ON COMMIT DROP;
CREATE TEMP TABLE _scms_advanced_deleted(class_id INTEGER PRIMARY KEY) ON COMMIT DROP;

DO $guard$
DECLARE fk RECORD; column_name TEXT;
BEGIN
 FOR fk IN
  SELECT k.oid,k.conrelid,k.conname,k.conkey,k.confkey,n.nspname,t.relname
  FROM pg_constraint k JOIN pg_class t ON t.oid=k.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
  WHERE k.contype='f' AND k.confrelid='public.classes'::regclass
 LOOP
  IF cardinality(fk.conkey)<>1 OR cardinality(fk.confkey)<>1
     OR NOT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid='public.classes'::regclass
                   AND attnum=fk.confkey[1] AND attname='class_id') THEN
   RAISE EXCEPTION 'SCMS_ADVANCED_CLEANUP_UNKNOWN_FK: %',fk.conname;
  END IF;
  SELECT attname INTO STRICT column_name FROM pg_attribute
   WHERE attrelid=fk.conrelid AND attnum=fk.conkey[1];
  -- Freeze child writes before checking references, including CASCADE/SET NULL FKs.
  EXECUTE format('LOCK TABLE %I.%I IN SHARE ROW EXCLUSIVE MODE',fk.nspname,fk.relname);
  EXECUTE format('INSERT INTO _scms_advanced_references
     SELECT c.class_id,%L,count(*) FROM _scms_advanced_candidates c
     JOIN %I.%I x ON x.%I=c.class_id GROUP BY c.class_id',
     fk.nspname||'.'||fk.relname||'.'||column_name,fk.nspname,fk.relname,column_name);
 END LOOP;

 -- Do not orphan polymorphic links, even though they have no SQL FK.
 LOCK TABLE public.notifications,public.system_logs IN SHARE ROW EXCLUSIVE MODE;
 INSERT INTO _scms_advanced_references
 SELECT c.class_id,'notifications.related_id',count(*)
 FROM _scms_advanced_candidates c JOIN public.notifications n
   ON n.related_type='CLASS' AND n.related_id=c.class_id GROUP BY c.class_id;
 INSERT INTO _scms_advanced_references
 SELECT c.class_id,'system_logs.target_id',count(*)
 FROM _scms_advanced_candidates c JOIN public.system_logs l
   ON l.target_entity='CLASS' AND l.target_id=c.class_id GROUP BY c.class_id;

 -- A child FK with ON DELETE CASCADE/SET NULL must never be allowed to erase history.
 WITH deleted AS (
  DELETE FROM public.classes c USING _scms_advanced_candidates candidate
  WHERE c.class_id=candidate.class_id
    AND NOT EXISTS(SELECT 1 FROM _scms_advanced_references r WHERE r.class_id=c.class_id)
  RETURNING c.class_id
 ) INSERT INTO _scms_advanced_deleted SELECT class_id FROM deleted;
END $guard$;

SELECT 'DELETED_UNUSED_TEMPLATE' AS result,d.class_id FROM _scms_advanced_deleted d
UNION ALL
SELECT 'RETAINED_REFERENCED_TEMPLATE',c.class_id FROM _scms_advanced_candidates c
WHERE NOT EXISTS(SELECT 1 FROM _scms_advanced_deleted d WHERE d.class_id=c.class_id)
ORDER BY class_id;
SELECT class_id,source,referencing_rows FROM _scms_advanced_references ORDER BY class_id,source;
COMMIT;
