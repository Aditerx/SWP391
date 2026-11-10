-- Normal lifecycle metadata. Additive upgrade; no guessing legacy confirmation/minimum or changing receipts.
BEGIN;
DO $$ BEGIN
 IF to_regclass('public.classes') IS NULL OR to_regclass('public.sessions') IS NULL THEN
  RAISE EXCEPTION 'Normal lifecycle requires the classes/sessions baseline';
 END IF;
END $$;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS min_capacity INT;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_confirmed_by INT REFERENCES public.users(user_id);
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_confirmed_at TIMESTAMP;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS opening_reason VARCHAR(2000);
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS postponement_count INT NOT NULL DEFAULT 0;
ALTER TABLE public.classes ADD COLUMN IF NOT EXISTS lifecycle_version BIGINT NOT NULL DEFAULT 0;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes'
   AND ((column_name IN ('min_capacity','opening_confirmed_by','postponement_count') AND data_type<>'integer')
     OR (column_name='lifecycle_version' AND data_type<>'bigint')
     OR (column_name='opening_confirmed_at' AND data_type<>'timestamp without time zone')
     OR (column_name='opening_reason' AND (data_type<>'character varying' OR character_maximum_length<>2000)))) THEN
  RAISE EXCEPTION 'Incompatible Normal lifecycle columns; reconcile before upgrade';
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes'
   AND column_name IN ('postponement_count','lifecycle_version') AND (is_nullable<>'NO' OR coalesce(column_default,'')<>'0')) THEN
  RAISE EXCEPTION 'Incompatible Normal lifecycle defaults/nullability; reconcile before upgrade';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
   WHERE c.conrelid='public.classes'::regclass AND c.confrelid='public.users'::regclass AND c.contype='f' AND a.attname='opening_confirmed_by') THEN
  ALTER TABLE public.classes ADD CONSTRAINT fk_normal_opening_actor FOREIGN KEY(opening_confirmed_by) REFERENCES public.users(user_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_capacity') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_capacity CHECK(min_capacity IS NULL OR (min_capacity>0 AND min_capacity<=max_capacity));
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_postponement') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_postponement CHECK(postponement_count IN (0,1) AND lifecycle_version>=0);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.classes'::regclass AND conname='ck_normal_lifecycle_confirmation') THEN
  ALTER TABLE public.classes ADD CONSTRAINT ck_normal_lifecycle_confirmation CHECK((opening_confirmed_by IS NULL)=(opening_confirmed_at IS NULL));
 END IF;
END $$;
-- NULL minimum/confirmation on legacy rows is deliberate: the matching BE blocks confirmation until reviewed.
COMMIT;
