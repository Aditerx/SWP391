-- Owner-run upgrade: one private class per accepted Advanced registration.
-- Preserves invoice amounts, credit balances, session IDs/status/Coach/Member, audit and consent history.
-- Reassigns only sessions.class_id through the exact existing subscription->registration relation.
-- Legacy shared templates are retained for a separate guarded cleanup; the new API does not list them.
BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='90s';
SELECT pg_advisory_xact_lock(391,20261114);
LOCK TABLE public.classes,public.advanced_registrations,public.sessions,public.private_session_proposals IN SHARE ROW EXCLUSIVE MODE;
ALTER TABLE public.advanced_registrations ADD COLUMN IF NOT EXISTS class_id INTEGER;
DO $preflight$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='advanced_registrations'
   AND column_name='class_id' AND data_type='integer' AND is_nullable='YES') THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: advanced_registrations.class_id';
 END IF;
 IF EXISTS(SELECT 1 FROM advanced_registrations r JOIN users m ON m.user_id=r.member_id JOIN users c ON c.user_id=r.coach_id
           WHERE r.status IN ('Accepted','Invoiced') AND (r.accepted_at IS NULL OR r.accepted_by IS NULL OR m.center_id IS DISTINCT FROM c.center_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: missing acceptance or mismatched center';
 END IF;
 IF EXISTS(SELECT 1 FROM sessions s JOIN classes c ON c.class_id=s.class_id
           LEFT JOIN advanced_registrations r ON r.subscription_id=s.subscription_id
           WHERE (c.class_type='Advanced' OR s.subscription_id IS NOT NULL) AND
            (r.id IS NULL OR r.status NOT IN ('Accepted','Invoiced') OR c.class_type<>'Advanced'
             OR s.booked_member_id IS DISTINCT FROM r.member_id OR c.subject_id IS DISTINCT FROM r.subject_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: session lacks an exact registration/member/subject link';
 END IF;
 IF EXISTS(SELECT 1 FROM training_plans p JOIN classes c ON c.class_id=p.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM class_feedbacks f JOIN classes c ON c.class_id=f.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM class_enrollments e JOIN classes c ON c.class_id=e.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id))
 OR EXISTS(SELECT 1 FROM invoices i JOIN classes c ON c.class_id=i.class_id WHERE c.class_type='Advanced' AND NOT EXISTS(SELECT 1 FROM advanced_registrations owner WHERE owner.class_id=c.class_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_AMBIGUOUS: class-scoped legacy documents require explicit mapping; no history moved';
 END IF;
 IF EXISTS(SELECT 1 FROM private_session_proposals p JOIN advanced_registrations r ON r.subscription_id=p.subscription_id
           WHERE p.status='Pending' AND p.class_id IS NOT NULL AND p.class_id IS DISTINCT FROM r.class_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_PENDING_PROPOSAL: reject/reissue explicit legacy class offers through the authorized API first';
 END IF;
END $preflight$;

DO $backfill$
DECLARE owned_registration RECORD; new_class INTEGER;
BEGIN
 FOR owned_registration IN SELECT a.*,u.center_id,s.subject_name,coalesce(p.purchase_amount,s.advanced_price) AS price
  FROM advanced_registrations a JOIN users u ON u.user_id=a.member_id JOIN subjects s USING(subject_id)
  LEFT JOIN member_packages p ON p.subscription_id=a.subscription_id
  WHERE a.status IN ('Accepted','Invoiced') AND a.class_id IS NULL ORDER BY a.id
 LOOP
  INSERT INTO classes(class_name,subject_id,coach_id,max_capacity,tuition_fee,status,class_type,total_sessions,center_id,pricing_draft)
  VALUES(left(owned_registration.subject_name,65)||' - Advanced #'||owned_registration.id,owned_registration.subject_id,owned_registration.coach_id,1,owned_registration.price,'Closed','Advanced',12,owned_registration.center_id,false)
  RETURNING class_id INTO new_class;
  UPDATE advanced_registrations SET class_id=new_class WHERE id=owned_registration.id;
 END LOOP;
 IF EXISTS(SELECT 1 FROM advanced_registrations r JOIN classes c ON c.class_id=r.class_id JOIN users m ON m.user_id=r.member_id
           WHERE c.class_type<>'Advanced' OR c.max_capacity<>1 OR c.subject_id IS DISTINCT FROM r.subject_id
             OR c.coach_id IS DISTINCT FROM r.coach_id OR c.center_id IS DISTINCT FROM m.center_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: existing private class belongs to another course/Coach/center';
 END IF;
END $backfill$;

UPDATE sessions s SET class_id=r.class_id FROM advanced_registrations r
WHERE r.subscription_id=s.subscription_id AND r.class_id IS NOT NULL AND s.class_id IS DISTINCT FROM r.class_id;

DO $constraints$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='fk_advanced_registration_class') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT fk_advanced_registration_class FOREIGN KEY(class_id) REFERENCES classes(class_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='uq_advanced_registration_class') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT uq_advanced_registration_class UNIQUE(class_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='ck_advanced_registration_class_state') THEN
  ALTER TABLE advanced_registrations ADD CONSTRAINT ck_advanced_registration_class_state CHECK((status IN ('Accepted','Invoiced'))=(class_id IS NOT NULL));
 END IF;
 IF EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass
           AND conname IN ('fk_advanced_registration_class','uq_advanced_registration_class','ck_advanced_registration_class_state') AND NOT convalidated)
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='fk_advanced_registration_class'
      AND contype='f' AND confrelid='classes'::regclass
      AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='advanced_registrations'::regclass AND attname='class_id')]::smallint[]
      AND confkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='classes'::regclass AND attname='class_id')]::smallint[])
 OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass AND conname='uq_advanced_registration_class' AND contype='u'
      AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='advanced_registrations'::regclass AND attname='class_id')]::smallint[]) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: unexpected FK/unique/validation';
 END IF;
END $constraints$;
CREATE TEMP TABLE _scms_expected_advanced_class_state(
 status VARCHAR(20),class_id INTEGER,
 CHECK((status IN ('Accepted','Invoiced'))=(class_id IS NOT NULL))
) ON COMMIT DROP;
DO $check_definition$
BEGIN
 IF (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='advanced_registrations'::regclass
      AND conname='ck_advanced_registration_class_state') IS DISTINCT FROM
    (SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='_scms_expected_advanced_class_state'::regclass AND contype='c') THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_INCOMPATIBLE: unexpected state CHECK';
 END IF;
END $check_definition$;

CREATE OR REPLACE FUNCTION public.fn_scms_advanced_registration_class() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND OLD.class_id IS NOT NULL AND
  (NEW.class_id IS DISTINCT FROM OLD.class_id OR NEW.member_id IS DISTINCT FROM OLD.member_id
   OR NEW.subject_id IS DISTINCT FROM OLD.subject_id OR NEW.coach_id IS DISTINCT FROM OLD.coach_id
   OR (OLD.subscription_id IS NOT NULL AND NEW.subscription_id IS DISTINCT FROM OLD.subscription_id)) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_IMMUTABLE: approved registration identity cannot be replaced';
 END IF;
 IF NEW.class_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM classes c JOIN users m ON m.user_id=NEW.member_id
   WHERE c.class_id=NEW.class_id AND c.class_type='Advanced' AND c.max_capacity=1
     AND c.subject_id=NEW.subject_id AND c.coach_id=NEW.coach_id AND c.center_id=m.center_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_OWNER: exact private class/subject/Coach/center required';
 END IF;
 IF NEW.class_id IS NOT NULL AND (NEW.accepted_at IS NULL OR NEW.accepted_by IS NULL) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_OWNER: recorded decision actor and time required';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_advanced_registration_class ON public.advanced_registrations;
CREATE TRIGGER trg_advanced_registration_class BEFORE INSERT OR UPDATE ON public.advanced_registrations
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_registration_class();
CREATE OR REPLACE FUNCTION public.fn_scms_advanced_class_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=OLD.class_id) AND
   (NEW.subject_id IS DISTINCT FROM OLD.subject_id OR NEW.coach_id IS DISTINCT FROM OLD.coach_id
    OR NEW.center_id IS DISTINCT FROM OLD.center_id OR NEW.class_type IS DISTINCT FROM OLD.class_type
    OR NEW.max_capacity IS DISTINCT FROM 1) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_IMMUTABLE: use the registered course and per-session substitute workflow';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_advanced_class_identity ON public.classes;
CREATE TRIGGER trg_advanced_class_identity BEFORE UPDATE ON public.classes
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_identity();
CREATE OR REPLACE FUNCTION public.fn_scms_private_session_class() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF (NEW.subscription_id IS NOT NULL OR EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=NEW.class_id))
  AND NOT EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.subscription_id=NEW.subscription_id
                 AND r.class_id=NEW.class_id AND r.member_id=NEW.booked_member_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_SESSION_OWNER: exact subscription, Member and private class required';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_private_session_class ON public.sessions;
CREATE TRIGGER trg_private_session_class BEFORE INSERT OR UPDATE ON public.sessions
 FOR EACH ROW EXECUTE FUNCTION public.fn_scms_private_session_class();
CREATE OR REPLACE FUNCTION public.fn_scms_advanced_class_has_owner() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE private_id INTEGER;
BEGIN
 private_id:=CASE WHEN TG_OP='DELETE' THEN OLD.class_id ELSE NEW.class_id END;
 IF EXISTS(SELECT 1 FROM classes c WHERE c.class_id=private_id AND c.class_type='Advanced')
    AND NOT EXISTS(SELECT 1 FROM advanced_registrations r WHERE r.class_id=private_id) THEN
  RAISE EXCEPTION 'SCMS_ADVANCED_CLASS_UNOWNED: private class must belong to an accepted registration at commit';
 END IF;
 RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS trg_private_class_has_owner ON public.classes;
CREATE CONSTRAINT TRIGGER trg_private_class_has_owner AFTER INSERT ON public.classes
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_has_owner();
DROP TRIGGER IF EXISTS trg_deleted_registration_keeps_class_owner ON public.advanced_registrations;
CREATE CONSTRAINT TRIGGER trg_deleted_registration_keeps_class_owner AFTER DELETE ON public.advanced_registrations
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_scms_advanced_class_has_owner();
COMMIT;
SELECT 'PASS' AS result,'20261114_advanced_registration_classes_v1' AS migration_key,
       'Existing sessions re-linked by their subscription; money, credit, consent and session history retained' AS scope;
