
BEGIN;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_sub ON public.users (google_sub) WHERE google_sub IS NOT NULL;
COMMIT;
