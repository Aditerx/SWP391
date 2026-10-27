-- SQL Server equivalent of 08. Additive only; run after schedule safety.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF NOT EXISTS(SELECT 1 FROM sys.sequences WHERE name='invoice_code_seq' AND schema_id=SCHEMA_ID('dbo'))
BEGIN
    DECLARE @next BIGINT=(SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(invoice_code,CHARINDEX('-',invoice_code,5)+1,30))),0)+1 FROM dbo.invoices WITH(UPDLOCK,HOLDLOCK));
    DECLARE @sequenceSql NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.invoice_code_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(30),@next)+N' INCREMENT BY 1';
    EXEC sp_executesql @sequenceSql;
END;
IF COL_LENGTH('dbo.member_packages','purchase_amount') IS NULL ALTER TABLE dbo.member_packages ADD purchase_amount DECIMAL(12,2) NULL;
ALTER TABLE dbo.member_packages ALTER COLUMN package_id INT NULL;
ALTER TABLE dbo.member_packages ALTER COLUMN start_date DATE NULL;
ALTER TABLE dbo.member_packages ALTER COLUMN end_date DATE NULL;
IF EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='chk_invoice_target' AND parent_object_id=OBJECT_ID('dbo.invoices')) ALTER TABLE dbo.invoices DROP CONSTRAINT chk_invoice_target;
ALTER TABLE dbo.invoices ADD CONSTRAINT chk_invoice_target CHECK((class_id IS NOT NULL AND package_id IS NULL AND subscription_id IS NULL) OR (class_id IS NULL AND (package_id IS NOT NULL OR subscription_id IS NOT NULL)));
IF COL_LENGTH('dbo.member_packages','subject_id') IS NULL ALTER TABLE dbo.member_packages ADD subject_id INT NULL REFERENCES subjects(subject_id);
IF COL_LENGTH('dbo.member_packages','primary_coach_id') IS NULL ALTER TABLE dbo.member_packages ADD primary_coach_id INT NULL REFERENCES users(user_id);
IF COL_LENGTH('dbo.member_packages','activated_at') IS NULL ALTER TABLE dbo.member_packages ADD activated_at DATETIME2;
IF COL_LENGTH('dbo.member_packages','expires_at') IS NULL ALTER TABLE dbo.member_packages ADD expires_at DATETIME2;
IF COL_LENGTH('dbo.member_packages','expiry_revoked_credit') IS NULL ALTER TABLE dbo.member_packages ADD expiry_revoked_credit INT DEFAULT 0;
IF COL_LENGTH('dbo.member_packages','expiry_processed_at') IS NULL ALTER TABLE dbo.member_packages ADD expiry_processed_at DATETIME2;
IF COL_LENGTH('dbo.member_packages','approved_extra_seconds') IS NULL ALTER TABLE dbo.member_packages ADD approved_extra_seconds BIGINT NOT NULL DEFAULT 0;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.member_packages') AND name='idx_subscription_primary_coach') CREATE INDEX idx_subscription_primary_coach ON dbo.member_packages(primary_coach_id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.member_packages') AND name='idx_subscription_deadline') CREATE INDEX idx_subscription_deadline ON dbo.member_packages(status, expires_at);
IF OBJECT_ID('dbo.subscription_pauses','U') IS NULL CREATE TABLE dbo.subscription_pauses (
    id INT IDENTITY(1,1) PRIMARY KEY, subscription_id INT NOT NULL REFERENCES member_packages(subscription_id),
    source_key VARCHAR(120) NOT NULL, starts_at DATETIME2 NOT NULL, ends_at DATETIME2 NOT NULL,
    UNIQUE(subscription_id,source_key), CHECK(ends_at > starts_at)
);
IF OBJECT_ID('dbo.coach_work_schedules','U') IS NULL CREATE TABLE dbo.coach_work_schedules (
    coach_id INT PRIMARY KEY REFERENCES users(user_id), accepting_new BIT NOT NULL DEFAULT 0
);
IF OBJECT_ID('dbo.coach_work_slots','U') IS NULL CREATE TABLE dbo.coach_work_slots (
    coach_id INT NOT NULL REFERENCES coach_work_schedules(coach_id), slot_key VARCHAR(20) NOT NULL,
    PRIMARY KEY(coach_id,slot_key)
);
IF OBJECT_ID('dbo.coach_days_off','U') IS NULL CREATE TABLE dbo.coach_days_off (
    coach_id INT NOT NULL REFERENCES coach_work_schedules(coach_id), day_off DATE NOT NULL,
    PRIMARY KEY(coach_id,day_off)
);
IF OBJECT_ID('dbo.advanced_registrations','U') IS NULL CREATE TABLE dbo.advanced_registrations (
    id INT IDENTITY(1,1) PRIMARY KEY, member_id INT NOT NULL REFERENCES users(user_id),
    subject_id INT NOT NULL REFERENCES subjects(subject_id), coach_id INT NOT NULL REFERENCES users(user_id),
    request_key VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'Requested', created_at DATETIME2 NOT NULL,
    accepted_by INT NULL REFERENCES users(user_id), accepted_at DATETIME2, reason NVARCHAR(2000),
    subscription_id INT NULL REFERENCES member_packages(subscription_id), UNIQUE(member_id,request_key)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.advanced_registrations') AND name='uq_registration_subscription') CREATE UNIQUE INDEX uq_registration_subscription ON dbo.advanced_registrations(subscription_id) WHERE subscription_id IS NOT NULL;
COMMIT TRANSACTION;
