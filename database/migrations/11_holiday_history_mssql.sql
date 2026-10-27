-- SQL Server counterpart. Additive; no historical credit/payment rewriting.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.holidays','actual_closed_until') IS NULL ALTER TABLE dbo.holidays ADD actual_closed_until DATETIME2;
COMMIT TRANSACTION;
