-- Additive Normal consent/entitlement/manual-refund model. No cleanup/seed/provider operations.
BEGIN;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS change_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS entitlement_class_id INT REFERENCES classes(class_id);
DO $guard$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='classes' AND column_name='change_pending' AND data_type='boolean' AND is_nullable='NO' AND column_default='false')
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='invoices' AND column_name='entitlement_class_id' AND data_type='integer' AND is_nullable='YES') THEN
  RAISE EXCEPTION 'Incompatible Normal change columns';
 END IF;
END $guard$;
CREATE TABLE IF NOT EXISTS normal_proposals (
 id SERIAL PRIMARY KEY, class_id INT NOT NULL REFERENCES classes(class_id), target_class_id INT REFERENCES classes(class_id),
 kind VARCHAR(20) NOT NULL CHECK(kind IN ('Postpone','Transfer')), status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK(status IN ('Pending','Applied','Cancelled')),
 shift_days INT, version BIGINT NOT NULL, fingerprint VARCHAR(64) NOT NULL, schedule_json TEXT NOT NULL, reason VARCHAR(2000) NOT NULL,
 created_by INT NOT NULL REFERENCES users(user_id), created_at TIMESTAMP NOT NULL,
 CHECK((kind='Postpone' AND shift_days BETWEEN 1 AND 366 AND target_class_id IS NULL) OR (kind='Transfer' AND shift_days IS NULL AND target_class_id IS NOT NULL AND target_class_id<>class_id))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_normal_pending_proposal ON normal_proposals(class_id) WHERE status='Pending';
CREATE TABLE IF NOT EXISTS normal_responses (
 id SERIAL PRIMARY KEY, proposal_id INT NOT NULL REFERENCES normal_proposals(id), invoice_id INT NOT NULL REFERENCES invoices(invoice_id),
 member_id INT NOT NULL REFERENCES users(user_id), choice VARCHAR(20) NOT NULL CHECK(choice IN ('Waiting','Accept','Refund')),
 version BIGINT NOT NULL, responded_at TIMESTAMP NOT NULL, UNIQUE(proposal_id,invoice_id)
);
CREATE TABLE IF NOT EXISTS normal_transfers (
 id SERIAL PRIMARY KEY, proposal_id INT NOT NULL REFERENCES normal_proposals(id), invoice_id INT NOT NULL REFERENCES invoices(invoice_id),
 source_enrollment_id INT NOT NULL REFERENCES class_enrollments(enrollment_id), target_enrollment_id INT NOT NULL REFERENCES class_enrollments(enrollment_id),
 consented_by INT NOT NULL REFERENCES users(user_id), transferred_at TIMESTAMP NOT NULL,
 UNIQUE(proposal_id,invoice_id), CHECK(source_enrollment_id<>target_enrollment_id)
);
CREATE TABLE IF NOT EXISTS normal_refunds (
 id INT PRIMARY KEY REFERENCES payment_resolutions(id), class_id INT NOT NULL REFERENCES classes(class_id), origin VARCHAR(30) NOT NULL,
 receiver_id INT, approved_receiver_id INT, version BIGINT NOT NULL DEFAULT 0, due_at TIMESTAMP, first_due_at TIMESTAMP, issue_note VARCHAR(2000),
 CHECK(origin IN ('MemberChoice','CenterCancellation','LateReceipt'))
);
CREATE TABLE IF NOT EXISTS normal_refund_receivers (
 id SERIAL PRIMARY KEY, refund_id INT NOT NULL REFERENCES normal_refunds(id), bank VARCHAR(120) NOT NULL, account_number VARCHAR(40) NOT NULL,
 account_name VARCHAR(150) NOT NULL, confirmed_by INT NOT NULL REFERENCES users(user_id), confirmed_at TIMESTAMP NOT NULL,
 verified_by INT REFERENCES users(user_id), verified_at TIMESTAMP, verification_source VARCHAR(2000), UNIQUE(id,refund_id),
 CHECK((verified_by IS NULL AND verified_at IS NULL) OR (verified_by IS NOT NULL AND verified_at IS NOT NULL AND verification_source IS NOT NULL))
);
DO $fk$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='normal_refunds'::regclass AND conname='fk_normal_refund_receiver') THEN
  ALTER TABLE normal_refunds ADD CONSTRAINT fk_normal_refund_receiver FOREIGN KEY(receiver_id,id) REFERENCES normal_refund_receivers(id,refund_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='normal_refunds'::regclass AND conname='fk_normal_refund_approved_receiver') THEN
  ALTER TABLE normal_refunds ADD CONSTRAINT fk_normal_refund_approved_receiver FOREIGN KEY(approved_receiver_id,id) REFERENCES normal_refund_receivers(id,refund_id);
 END IF;
END $fk$;
CREATE TABLE IF NOT EXISTS normal_refund_attempts (
 id SERIAL PRIMARY KEY, refund_id INT NOT NULL REFERENCES normal_refunds(id), receiver_id INT NOT NULL,
 claim_key VARCHAR(36) NOT NULL UNIQUE, claimed_by INT NOT NULL REFERENCES users(user_id), claimed_at TIMESTAMP NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'Claimed' CHECK(status IN ('Claimed','Submitted','Verified','Failed','Reversed')),
 amount NUMERIC(12,2) NOT NULL CHECK(amount>0), currency VARCHAR(3) NOT NULL CHECK(currency='VND'), bank_reference VARCHAR(120) UNIQUE,
 performed_at TIMESTAMP, verified_by INT REFERENCES users(user_id), verified_at TIMESTAMP, bank_source VARCHAR(2000), outcome_note VARCHAR(2000),
 FOREIGN KEY(receiver_id,refund_id) REFERENCES normal_refund_receivers(id,refund_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_normal_refund_active_attempt ON normal_refund_attempts(refund_id) WHERE status IN ('Claimed','Submitted','Verified');
CREATE TABLE IF NOT EXISTS normal_refund_documents (
 id SERIAL PRIMARY KEY, attempt_id INT NOT NULL REFERENCES normal_refund_attempts(id), uploaded_by INT NOT NULL REFERENCES users(user_id),
 uploaded_at TIMESTAMP NOT NULL, content_type VARCHAR(30) NOT NULL CHECK(content_type IN ('application/pdf','image/png','image/jpeg')),
 sha256 VARCHAR(64) NOT NULL, content BYTEA NOT NULL CHECK(octet_length(content) BETWEEN 1 AND 5242880)
);
CREATE TABLE IF NOT EXISTS normal_refund_verifications (
 id SERIAL PRIMARY KEY, attempt_id INT NOT NULL REFERENCES normal_refund_attempts(id), verified_by INT NOT NULL REFERENCES users(user_id),
 verified_at TIMESTAMP NOT NULL, outcome VARCHAR(20) NOT NULL CHECK(outcome IN ('Success','Failed','Reversed','Pending')),
 bank_source VARCHAR(2000) NOT NULL, note VARCHAR(2000) NOT NULL
);
-- Column compatibility is checked on replay too; IF NOT EXISTS is not schema validation.
DO $model$
DECLARE item RECORD;
BEGIN
 FOR item IN SELECT * FROM (VALUES
('normal_proposals','id','integer',true),
('normal_proposals','class_id','integer',true),
('normal_proposals','target_class_id','integer',false),
('normal_proposals','kind','character varying(20)',true),
('normal_proposals','status','character varying(20)',true),
('normal_proposals','shift_days','integer',false),
('normal_proposals','version','bigint',true),
('normal_proposals','fingerprint','character varying(64)',true),
('normal_proposals','schedule_json','text',true),
('normal_proposals','reason','character varying(2000)',true),
('normal_proposals','created_by','integer',true),
('normal_proposals','created_at','timestamp without time zone',true),
('normal_responses','id','integer',true),
('normal_responses','proposal_id','integer',true),
('normal_responses','invoice_id','integer',true),
('normal_responses','member_id','integer',true),
('normal_responses','choice','character varying(20)',true),
('normal_responses','version','bigint',true),
('normal_responses','responded_at','timestamp without time zone',true),
('normal_transfers','id','integer',true),
('normal_transfers','proposal_id','integer',true),
('normal_transfers','invoice_id','integer',true),
('normal_transfers','source_enrollment_id','integer',true),
('normal_transfers','target_enrollment_id','integer',true),
('normal_transfers','consented_by','integer',true),
('normal_transfers','transferred_at','timestamp without time zone',true),
('normal_refunds','id','integer',true),
('normal_refunds','class_id','integer',true),
('normal_refunds','origin','character varying(30)',true),
('normal_refunds','receiver_id','integer',false),
('normal_refunds','approved_receiver_id','integer',false),
('normal_refunds','version','bigint',true),
('normal_refunds','due_at','timestamp without time zone',false),
('normal_refunds','first_due_at','timestamp without time zone',false),
('normal_refunds','issue_note','character varying(2000)',false),
('normal_refund_receivers','id','integer',true),
('normal_refund_receivers','refund_id','integer',true),
('normal_refund_receivers','bank','character varying(120)',true),
('normal_refund_receivers','account_number','character varying(40)',true),
('normal_refund_receivers','account_name','character varying(150)',true),
('normal_refund_receivers','confirmed_by','integer',true),
('normal_refund_receivers','confirmed_at','timestamp without time zone',true),
('normal_refund_receivers','verified_by','integer',false),
('normal_refund_receivers','verified_at','timestamp without time zone',false),
('normal_refund_receivers','verification_source','character varying(2000)',false),
('normal_refund_attempts','id','integer',true),
('normal_refund_attempts','refund_id','integer',true),
('normal_refund_attempts','receiver_id','integer',true),
('normal_refund_attempts','claim_key','character varying(36)',true),
('normal_refund_attempts','claimed_by','integer',true),
('normal_refund_attempts','claimed_at','timestamp without time zone',true),
('normal_refund_attempts','status','character varying(30)',true),
('normal_refund_attempts','amount','numeric(12,2)',true),
('normal_refund_attempts','currency','character varying(3)',true),
('normal_refund_attempts','bank_reference','character varying(120)',false),
('normal_refund_attempts','performed_at','timestamp without time zone',false),
('normal_refund_attempts','verified_by','integer',false),
('normal_refund_attempts','verified_at','timestamp without time zone',false),
('normal_refund_attempts','bank_source','character varying(2000)',false),
('normal_refund_attempts','outcome_note','character varying(2000)',false),
('normal_refund_documents','id','integer',true),
('normal_refund_documents','attempt_id','integer',true),
('normal_refund_documents','uploaded_by','integer',true),
('normal_refund_documents','uploaded_at','timestamp without time zone',true),
('normal_refund_documents','content_type','character varying(30)',true),
('normal_refund_documents','sha256','character varying(64)',true),
('normal_refund_documents','content','bytea',true),
('normal_refund_verifications','id','integer',true),
('normal_refund_verifications','attempt_id','integer',true),
('normal_refund_verifications','verified_by','integer',true),
('normal_refund_verifications','verified_at','timestamp without time zone',true),
('normal_refund_verifications','outcome','character varying(20)',true),
('normal_refund_verifications','bank_source','character varying(2000)',true),
('normal_refund_verifications','note','character varying(2000)',true)
 ) expected(table_name,column_name,type_name,required) LOOP
  IF NOT EXISTS(SELECT 1 FROM pg_attribute a WHERE a.attrelid=to_regclass('public.'||item.table_name) AND a.attname=item.column_name AND NOT a.attisdropped AND format_type(a.atttypid,a.atttypmod)=item.type_name AND a.attnotnull=item.required) THEN
   RAISE EXCEPTION 'Incompatible Normal table %.%',item.table_name,item.column_name;
  END IF;
 END LOOP;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='invoices'::regclass AND contype='f' AND conkey=ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid='invoices'::regclass AND attname='entitlement_class_id')]::smallint[]) THEN RAISE EXCEPTION 'Incompatible entitlement FK'; END IF;
 IF EXISTS(SELECT 1 FROM (VALUES('uq_normal_refund_active_attempt'),('uq_normal_pending_proposal')) expected(name) WHERE NOT EXISTS(SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid WHERE c.relname=expected.name AND i.indisunique AND i.indpred IS NOT NULL AND i.indisvalid)) THEN RAISE EXCEPTION 'Incompatible Normal uniqueness guards'; END IF;
END $model$;
COMMIT;
