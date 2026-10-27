-- SQL Server counterpart. Additive; no historical credit/payment rewriting.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.sessions','booking_key') IS NULL ALTER TABLE dbo.sessions ADD booking_key VARCHAR(80);
IF COL_LENGTH('dbo.sessions','booking_hash') IS NULL ALTER TABLE dbo.sessions ADD booking_hash VARCHAR(64);
IF COL_LENGTH('dbo.sessions','created_by') IS NULL ALTER TABLE dbo.sessions ADD created_by INT NULL REFERENCES users(user_id);
IF COL_LENGTH('dbo.sessions','agreement_confirmed_at') IS NULL ALTER TABLE dbo.sessions ADD agreement_confirmed_at DATETIME2;
IF COL_LENGTH('dbo.sessions','support_reason') IS NULL ALTER TABLE dbo.sessions ADD support_reason NVARCHAR(2000);
IF COL_LENGTH('dbo.sessions','credit_returned') IS NULL ALTER TABLE dbo.sessions ADD credit_returned BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.sessions','center_invalidated') IS NULL ALTER TABLE dbo.sessions ADD center_invalidated BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.sessions','makeup_for_session_id') IS NULL ALTER TABLE dbo.sessions ADD makeup_for_session_id INT NULL REFERENCES sessions(session_id);
IF COL_LENGTH('dbo.member_packages','deferred_credit_return') IS NULL ALTER TABLE dbo.member_packages ADD deferred_credit_return INT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.member_packages','approved_extra_nanos') IS NULL ALTER TABLE dbo.member_packages ADD approved_extra_nanos INT NOT NULL DEFAULT 0;
IF OBJECT_ID('dbo.session_absence_notices','U') IS NULL CREATE TABLE dbo.session_absence_notices (
    id INT IDENTITY(1,1) PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), member_id INT NOT NULL REFERENCES users(user_id),
    reported_at DATETIME2 NOT NULL, original_start DATETIME2 NOT NULL, original_end DATETIME2 NOT NULL,
    reason NVARCHAR(2000) NOT NULL, active BIT NOT NULL DEFAULT 1, superseded_at DATETIME2
);
IF OBJECT_ID('dbo.session_changes','U') IS NULL CREATE TABLE dbo.session_changes (
    id INT IDENTITY(1,1) PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), request_key VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, actor_id INT NOT NULL REFERENCES users(user_id), changed_at DATETIME2 NOT NULL,
    cause VARCHAR(40) NOT NULL, reason NVARCHAR(2000) NOT NULL, member_agreed BIT NOT NULL,
    before_data NVARCHAR(4000) NOT NULL, after_data NVARCHAR(4000) NOT NULL, UNIQUE(session_id,request_key)
);
IF OBJECT_ID('dbo.center_incidents','U') IS NULL CREATE TABLE dbo.center_incidents (
    id INT IDENTITY(1,1) PRIMARY KEY, session_id INT NOT NULL REFERENCES sessions(session_id), request_key VARCHAR(80) NOT NULL,
    started_at DATETIME2 NOT NULL, ended_at DATETIME2, reason NVARCHAR(2000) NOT NULL, evidence NVARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WaitingForRecovery', created_by INT NOT NULL REFERENCES users(user_id), created_at DATETIME2 NOT NULL,
    resolved_by INT NULL REFERENCES users(user_id), resolved_at DATETIME2, original_deadline DATETIME2, deadline_before DATETIME2, deadline_after DATETIME2,
    recovered_credits INT NOT NULL DEFAULT 0, session_compensated BIT NOT NULL DEFAULT 0, UNIQUE(session_id,request_key),
    CHECK(ended_at IS NULL OR ended_at>=started_at)
);
IF OBJECT_ID('dbo.incident_affected_members','U') IS NULL CREATE TABLE dbo.incident_affected_members (
    incident_id INT NOT NULL REFERENCES center_incidents(id), member_id INT NOT NULL REFERENCES users(user_id), PRIMARY KEY(incident_id,member_id)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.sessions') AND name='uq_session_booking_key') CREATE UNIQUE INDEX uq_session_booking_key ON dbo.sessions(subscription_id,booking_key) WHERE booking_key IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.sessions') AND name='uq_session_makeup_original') CREATE UNIQUE INDEX uq_session_makeup_original ON dbo.sessions(makeup_for_session_id) WHERE makeup_for_session_id IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.session_absence_notices') AND name='uq_active_absence_notice') CREATE UNIQUE INDEX uq_active_absence_notice ON dbo.session_absence_notices(session_id) WHERE active=1;
COMMIT TRANSACTION;
