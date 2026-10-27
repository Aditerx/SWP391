-- SQL Server D02 counterpart. Guarded snapshot cleanup, no invoice/class rewrites.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.scms_data_migration_audit','U') IS NULL
    CREATE TABLE dbo.scms_data_migration_audit(migration_key VARCHAR(100) PRIMARY KEY,captured_at DATETIME2 NOT NULL,before_data NVARCHAR(MAX) NOT NULL,after_data NVARCHAR(MAX) NOT NULL);
IF NOT EXISTS(SELECT 1 FROM dbo.scms_data_migration_audit WITH(UPDLOCK,HOLDLOCK) WHERE migration_key='09_approved_catalog')
BEGIN
    DECLARE @locked INT; SELECT @locked=COUNT(*) FROM dbo.subjects WITH(TABLOCKX,HOLDLOCK);
    DECLARE @before NVARCHAR(MAX)=(SELECT * FROM dbo.subjects ORDER BY subject_id FOR JSON PATH);
    DECLARE @catalog TABLE(id INT,slug VARCHAR(40),name NVARCHAR(100),old_normal DECIMAL(12,2),old_advanced DECIMAL(12,2),normal DECIMAL(12,2),advanced DECIMAL(12,2));
    INSERT INTO @catalog VALUES
    (19,'quan-vot-tennis',N'Quần vợt (Tennis)',1500000,10000000,2500000,8000000),
    (20,'boi-loi',N'Bơi lội',1200000,9000000,1500000,4500000),(21,'pickleball',N'Pickleball',1100000,8500000,1800000,5000000),
    (22,'ban-cung',N'Bắn cung',1400000,9500000,2000000,6000000),(23,'bong-ro',N'Bóng rổ',1000000,8000000,1200000,3500000),
    (24,'bong-da',N'Bóng đá',900000,8000000,1000000,3500000),(25,'bong-chuyen',N'Bóng chuyền',900000,7000000,1000000,3500000),
    (26,'cau-long',N'Cầu lông',900000,7500000,900000,3000000),(27,'bong-ban',N'Bóng bàn',800000,6500000,800000,2500000);
    IF EXISTS(SELECT 1 FROM dbo.subjects WHERE slug IS NULL)
    BEGIN
        DECLARE @duplicates TABLE(id INT,name NVARCHAR(100));
        INSERT INTO @duplicates VALUES(1,N'Cầu lông'),(2,N'Pickleball'),(3,N'Bóng bàn'),(4,N'Bắn cung'),(5,N'Bóng rổ'),(6,N'Bóng chuyền'),(7,N'Bắn súng'),(8,N'Bóng đá'),(9,N'Quần vợt');
        IF (SELECT COUNT(*) FROM dbo.subjects WHERE slug IS NULL)<>9 OR
           (SELECT COUNT(*) FROM dbo.subjects s JOIN @duplicates d ON d.id=s.subject_id AND d.name=s.subject_name WHERE s.slug IS NULL AND s.normal_price=0 AND s.advanced_price=0 AND s.outdoor IS NULL)<>9 OR
           (SELECT COUNT(*) FROM dbo.subjects s JOIN @catalog c ON s.subject_id=c.id AND s.slug=c.slug AND s.subject_name=c.name AND s.normal_price=c.old_normal AND s.advanced_price=c.old_advanced)<>9
           THROW 51000,'D02 catalog snapshot mismatch: reconcile first',1;
        DECLARE @tbl NVARCHAR(260),@col NVARCHAR(128),@query NVARCHAR(MAX),@used BIT;
        DECLARE refs CURSOR LOCAL FAST_FORWARD FOR SELECT QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id))+'.'+QUOTENAME(OBJECT_NAME(parent_object_id)),QUOTENAME(COL_NAME(parent_object_id,parent_column_id)) FROM sys.foreign_key_columns WHERE referenced_object_id=OBJECT_ID('dbo.subjects');
        OPEN refs; FETCH NEXT FROM refs INTO @tbl,@col;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SET @query=N'SELECT @found=CASE WHEN EXISTS(SELECT 1 FROM '+@tbl+N' WHERE '+@col+N' IN (SELECT subject_id FROM dbo.subjects WHERE slug IS NULL)) THEN 1 ELSE 0 END';
            EXEC sp_executesql @query,N'@found BIT OUTPUT',@found=@used OUTPUT;
            IF @used=1 THROW 51000,'D02 duplicate subject has new references: reconcile first',1;
            FETCH NEXT FROM refs INTO @tbl,@col;
        END;
        CLOSE refs; DEALLOCATE refs;
        DELETE FROM dbo.subjects WHERE slug IS NULL AND subject_id BETWEEN 1 AND 9;
    END;
    IF (SELECT COUNT(*) FROM dbo.subjects)<>9 OR EXISTS(SELECT 1 FROM dbo.subjects s WHERE NOT EXISTS(SELECT 1 FROM @catalog c WHERE c.slug=s.slug))
        THROW 51000,'D02 requires nine canonical subjects',1;
    UPDATE s SET normal_price=c.normal,advanced_price=c.advanced FROM dbo.subjects s JOIN @catalog c ON s.slug=c.slug;
    IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_approved_subject_slug' AND parent_object_id=OBJECT_ID('dbo.subjects'))
        ALTER TABLE dbo.subjects ADD CONSTRAINT ck_approved_subject_slug CHECK(slug IS NOT NULL AND slug IN('quan-vot-tennis','boi-loi','pickleball','ban-cung','bong-ro','bong-da','bong-chuyen','cau-long','bong-ban'));
    IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_subject_positive_prices' AND parent_object_id=OBJECT_ID('dbo.subjects'))
        ALTER TABLE dbo.subjects ADD CONSTRAINT ck_subject_positive_prices CHECK(normal_price>0 AND advanced_price>0);
    INSERT INTO dbo.scms_data_migration_audit VALUES('09_approved_catalog',CURRENT_TIMESTAMP,@before,(SELECT * FROM dbo.subjects ORDER BY subject_id FOR JSON PATH));
END;
COMMIT TRANSACTION;
