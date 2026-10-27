BEGIN;
ALTER TABLE holidays ADD COLUMN IF NOT EXISTS actual_closed_until TIMESTAMP;
-- No guessed backfill. An active holiday's actual past interval is captured on deactivation.
COMMIT;
