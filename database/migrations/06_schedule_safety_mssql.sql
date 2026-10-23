-- SQL Server alternative to 06_schedule_safety_postgres.sql; run exactly one dialect.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('invoices', 'reconciliation_required') IS NULL
    EXEC('ALTER TABLE invoices ADD reconciliation_required BIT NOT NULL CONSTRAINT df_invoice_reconciliation DEFAULT 0');
DECLARE @statusConstraints NVARCHAR(MAX) = N'';
SELECT @statusConstraints = @statusConstraints + N'ALTER TABLE member_packages DROP CONSTRAINT ' + QUOTENAME(name) + N';'
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('member_packages') AND definition LIKE '%status%'
    AND definition LIKE '%Active%' AND definition LIKE '%Expired%' AND definition LIKE '%Cancelled%';
EXEC sp_executesql @statusConstraints;
ALTER TABLE member_packages ADD CONSTRAINT chk_member_package_status_safety CHECK (status IN ('Pending','Active','Expired','Cancelled','Completed'));
IF COL_LENGTH('subjects', 'outdoor') IS NULL
    EXEC('ALTER TABLE subjects ADD outdoor BIT NULL');
EXEC('UPDATE subjects SET outdoor = 1 WHERE outdoor IS NULL AND slug IN (''bong-da'',''quan-vot-tennis'',''boi-loi'',''ban-cung'')');
EXEC('UPDATE subjects SET outdoor = 0 WHERE outdoor IS NULL AND slug IN (''cau-long'',''bong-ban'',''bong-ro'',''bong-chuyen'',''pickleball'')');
IF OBJECT_ID('normal_class_requests', 'U') IS NULL
    CREATE TABLE normal_class_requests (
        request_id VARCHAR(36) NOT NULL PRIMARY KEY,
        request_hash VARCHAR(64) NOT NULL,
        center_id INT NOT NULL REFERENCES centers(center_id),
        class_id INT NOT NULL UNIQUE REFERENCES classes(class_id)
    );
-- Explicitly map existing specialization.subject_id through Manager tooling before scheduling.
COMMIT TRANSACTION;
