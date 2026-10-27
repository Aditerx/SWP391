-- SQL Server counterpart. Additive; no historical credit/payment rewriting.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.invoices','cash_note') IS NULL ALTER TABLE dbo.invoices ADD cash_note NVARCHAR(2000);
IF COL_LENGTH('dbo.invoices','cash_confirmation_reversed') IS NULL ALTER TABLE dbo.invoices ADD cash_confirmation_reversed BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.invoices','resolution_status') IS NULL ALTER TABLE dbo.invoices ADD resolution_status VARCHAR(30);
IF COL_LENGTH('dbo.class_enrollments','eligibility_subscription_id') IS NULL ALTER TABLE dbo.class_enrollments ADD eligibility_subscription_id INT NULL REFERENCES member_packages(subscription_id);
IF OBJECT_ID('dbo.payment_resolutions','U') IS NULL CREATE TABLE dbo.payment_resolutions (
    id INT IDENTITY(1,1) PRIMARY KEY, invoice_id INT NOT NULL REFERENCES invoices(invoice_id), resolution_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Open', amount NUMERIC(12,2) NOT NULL, reason NVARCHAR(2000) NOT NULL,
    evidence NVARCHAR(2000), outcome_note NVARCHAR(2000), created_by INT NOT NULL REFERENCES users(user_id), created_at DATETIME2 NOT NULL,
    approved_by INT NULL REFERENCES users(user_id), approved_at DATETIME2, completed_by INT NULL REFERENCES users(user_id), completed_at DATETIME2,
    performed_at DATETIME2, UNIQUE(invoice_id,resolution_type), CHECK(amount>0)
);
COMMIT TRANSACTION;
