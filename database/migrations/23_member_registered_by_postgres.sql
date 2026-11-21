-- Preserve existing data; allow Manager and Receptionist creators already supported by BE.
-- Run this WHOLE file on an existing SCMS DB. Do not run the demo-reset bundle for this fix.
-- Only changes the FK of public.members.registered_by; no seed, sequence or row updates.
BEGIN;
SET LOCAL search_path = pg_catalog, public;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

-- Block concurrent member writes while checking/replacing the FK.
LOCK TABLE public.users IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.members IN ACCESS EXCLUSIVE MODE;

DO $member_creator$
DECLARE
    creator_column SMALLINT;
    user_column SMALLINT;
    receptionist_column SMALLINT;
    creator_fk RECORD;
    fk_count INTEGER;
BEGIN
    SELECT attnum INTO creator_column FROM pg_attribute
    WHERE attrelid = 'public.members'::regclass AND attname = 'registered_by'
      AND atttypid = 'integer'::regtype AND NOT attnotnull AND NOT attisdropped;
    SELECT attnum INTO user_column FROM pg_attribute
    WHERE attrelid = 'public.users'::regclass AND attname = 'user_id'
      AND atttypid = 'integer'::regtype AND NOT attisdropped;
    SELECT attnum INTO receptionist_column FROM pg_attribute
    WHERE attrelid = 'public.receptionists'::regclass AND attname = 'user_id'
      AND atttypid = 'integer'::regtype AND NOT attisdropped;
    IF creator_column IS NULL OR user_column IS NULL OR receptionist_column IS NULL THEN
        RAISE EXCEPTION 'SCMS_MEMBER_CREATOR_INCOMPATIBLE: expected nullable integer members.registered_by and integer user IDs';
    END IF;

    SELECT count(*) INTO fk_count FROM pg_constraint
    WHERE conrelid = 'public.members'::regclass AND contype = 'f'
      AND creator_column = ANY(conkey);
    IF fk_count <> 1 THEN
        RAISE EXCEPTION 'SCMS_MEMBER_CREATOR_INCOMPATIBLE: expected exactly one creator FK, found %', fk_count;
    END IF;
    SELECT * INTO creator_fk FROM pg_constraint
    WHERE conrelid = 'public.members'::regclass AND contype = 'f'
      AND creator_column = ANY(conkey);
    IF creator_fk.conkey <> ARRAY[creator_column]::SMALLINT[]
       OR creator_fk.confupdtype <> 'a' OR creator_fk.confdeltype <> 'a'
       OR creator_fk.confmatchtype <> 's' OR creator_fk.condeferrable
       OR NOT creator_fk.convalidated THEN
        RAISE EXCEPTION 'SCMS_MEMBER_CREATOR_INCOMPATIBLE: unexpected creator FK definition';
    END IF;
    IF NOT (
        (creator_fk.conname = 'fk_member_receptionist'
         AND creator_fk.confrelid = 'public.receptionists'::regclass
         AND creator_fk.confkey = ARRAY[receptionist_column]::SMALLINT[])
        OR
        (creator_fk.conname = 'fk_member_registered_by'
         AND creator_fk.confrelid = 'public.users'::regclass
         AND creator_fk.confkey = ARRAY[user_column]::SMALLINT[])
    ) THEN
        RAISE EXCEPTION 'SCMS_MEMBER_CREATOR_INCOMPATIBLE: creator FK target/name differs from the reviewed old/new model';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.members m LEFT JOIN public.users u ON u.user_id = m.registered_by
        WHERE m.registered_by IS NOT NULL AND u.user_id IS NULL
    ) THEN
        RAISE EXCEPTION 'SCMS_MEMBER_CREATOR_ORPHAN: creator user is missing; reconcile without deleting history';
    END IF;

    IF creator_fk.conname = 'fk_member_receptionist' THEN
        ALTER TABLE public.members DROP CONSTRAINT fk_member_receptionist;
        ALTER TABLE public.members ADD CONSTRAINT fk_member_registered_by
            FOREIGN KEY (registered_by) REFERENCES public.users(user_id);
    END IF;
END $member_creator$;

COMMIT;
SELECT 'PASS' AS result, conname AS constraint_name,
       confrelid::regclass::text AS referenced_table, convalidated AS validated
FROM pg_constraint
WHERE conrelid = 'public.members'::regclass AND conname = 'fk_member_registered_by';
