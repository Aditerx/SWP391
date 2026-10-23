-- PostgreSQL only. Apply after migration 04; do NOT run destructive demo reset 05.
-- Additive, repeatable; no prices, invoices, credits or existing schedules are rewritten.
BEGIN;
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS outdoor BOOLEAN;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE subjects SET outdoor = TRUE WHERE outdoor IS NULL AND slug IN ('bong-da','quan-vot-tennis','boi-loi','ban-cung');
UPDATE subjects SET outdoor = FALSE WHERE outdoor IS NULL AND slug IN ('cau-long','bong-ban','bong-ro','bong-chuyen','pickleball');
-- Unclassified custom subjects remain NULL and must be configured by the Manager.
CREATE TABLE IF NOT EXISTS normal_class_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    center_id INTEGER NOT NULL REFERENCES centers(center_id),
    class_id INTEGER NOT NULL UNIQUE REFERENCES classes(class_id)
);
ALTER TABLE member_packages DROP CONSTRAINT IF EXISTS member_packages_status_check;
ALTER TABLE member_packages ADD CONSTRAINT member_packages_status_check CHECK (status IN ('Pending','Active','Expired','Cancelled','Completed'));
-- Only backfill known, unambiguous built-in specialization names. Custom mappings remain explicit.
UPDATE specializations sp SET subject_id = su.subject_id
FROM subjects su, (VALUES
    ('Tennis','quan-vot-tennis'),('Quần vợt','quan-vot-tennis'),('Quần vợt (Tennis)','quan-vot-tennis'),
    ('Bóng đá','bong-da'),('Bơi lội','boi-loi'),('Bắn cung','ban-cung'),('Cầu lông','cau-long'),
    ('Bóng bàn','bong-ban'),('Bóng rổ','bong-ro'),('Bóng chuyền','bong-chuyen'),('Pickleball','pickleball')
) AS mapping(name, slug)
WHERE sp.subject_id IS NULL AND LOWER(sp.name) = LOWER(mapping.name) AND su.slug = mapping.slug;
COMMIT;
