-- D02: guarded catalog-only cleanup. Does not change classes, invoices, or legacy subscriptions.
BEGIN;
LOCK TABLE subjects IN SHARE ROW EXCLUSIVE MODE;
CREATE TABLE IF NOT EXISTS scms_data_migration_audit (
    migration_key VARCHAR(100) PRIMARY KEY, captured_at TIMESTAMP NOT NULL, before_data TEXT NOT NULL, after_data TEXT NOT NULL
);
DO $$
DECLARE before_snapshot TEXT; fk RECORD; used BOOLEAN;
BEGIN
    IF EXISTS (SELECT 1 FROM scms_data_migration_audit WHERE migration_key='09_approved_catalog') THEN RETURN; END IF;
    SELECT json_agg(s ORDER BY subject_id)::TEXT INTO before_snapshot FROM subjects s;
    -- Only the exact unused zero-price snapshot is eligible for deletion.
    IF EXISTS(SELECT 1 FROM subjects WHERE slug IS NULL) THEN
        IF (SELECT count(*) FROM subjects WHERE slug IS NULL) <> 9 OR
           EXISTS(SELECT 1 FROM subjects WHERE slug IS NULL AND NOT (
             normal_price=0 AND advanced_price=0 AND outdoor IS NULL AND
             (subject_id,subject_name) IN ((1,'Cầu lông'),(2,'Pickleball'),(3,'Bóng bàn'),(4,'Bắn cung'),
                 (5,'Bóng rổ'),(6,'Bóng chuyền'),(7,'Bắn súng'),(8,'Bóng đá'),(9,'Quần vợt')))) OR
           (SELECT count(*) FROM subjects WHERE (subject_id,slug,subject_name,normal_price,advanced_price) IN
             ((19,'quan-vot-tennis','Quần vợt (Tennis)',1500000,10000000),(20,'boi-loi','Bơi lội',1200000,9000000),
              (21,'pickleball','Pickleball',1100000,8500000),(22,'ban-cung','Bắn cung',1400000,9500000),
              (23,'bong-ro','Bóng rổ',1000000,8000000),(24,'bong-da','Bóng đá',900000,8000000),
              (25,'bong-chuyen','Bóng chuyền',900000,7000000),(26,'cau-long','Cầu lông',900000,7500000),
              (27,'bong-ban','Bóng bàn',800000,6500000))) <> 9 THEN
            RAISE EXCEPTION 'D02 catalog snapshot mismatch: manual reconciliation required';
        END IF;
        FOR fk IN SELECT conrelid::regclass AS tbl, a.attname AS col FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
            WHERE c.contype='f' AND c.confrelid='subjects'::regclass LOOP
            EXECUTE format('SELECT EXISTS(SELECT 1 FROM %s WHERE %I IN (SELECT subject_id FROM subjects WHERE slug IS NULL))',fk.tbl,fk.col) INTO used;
            IF used THEN RAISE EXCEPTION 'D02 duplicate subject has new references in %: reconcile first',fk.tbl; END IF;
        END LOOP;
        DELETE FROM subjects WHERE slug IS NULL AND subject_id BETWEEN 1 AND 9;
    END IF;
    IF (SELECT count(*) FROM subjects) <> 9 OR EXISTS(SELECT 1 FROM subjects WHERE slug NOT IN
        ('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban')) THEN
        RAISE EXCEPTION 'D02 requires exactly nine canonical subjects';
    END IF;
    UPDATE subjects SET normal_price = CASE slug
        WHEN 'quan-vot-tennis' THEN 2500000 WHEN 'boi-loi' THEN 1500000 WHEN 'pickleball' THEN 1800000
        WHEN 'ban-cung' THEN 2000000 WHEN 'bong-ro' THEN 1200000 WHEN 'bong-da' THEN 1000000
        WHEN 'bong-chuyen' THEN 1000000 WHEN 'cau-long' THEN 900000 WHEN 'bong-ban' THEN 800000 END,
        advanced_price = CASE slug
        WHEN 'quan-vot-tennis' THEN 8000000 WHEN 'boi-loi' THEN 4500000 WHEN 'pickleball' THEN 5000000
        WHEN 'ban-cung' THEN 6000000 WHEN 'bong-ro' THEN 3500000 WHEN 'bong-da' THEN 3500000
        WHEN 'bong-chuyen' THEN 3500000 WHEN 'cau-long' THEN 3000000 WHEN 'bong-ban' THEN 2500000 END;
    ALTER TABLE subjects ADD CONSTRAINT ck_approved_subject_slug CHECK (slug IS NOT NULL AND slug IN
        ('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban'));
    ALTER TABLE subjects ADD CONSTRAINT ck_subject_positive_prices CHECK(normal_price>0 AND advanced_price>0);
    INSERT INTO scms_data_migration_audit VALUES('09_approved_catalog',CURRENT_TIMESTAMP,before_snapshot,
        (SELECT json_agg(s ORDER BY subject_id)::TEXT FROM subjects s));
END $$;
COMMIT;
