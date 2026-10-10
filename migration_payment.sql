-- PostgreSQL migration for membership payments and class tuition invoices.
-- Safe to run repeatedly; execute against the target database only after review.
BEGIN;

CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 1;

ALTER TABLE classes ADD COLUMN IF NOT EXISTS tuition_fee NUMERIC(12,2) NOT NULL DEFAULT 0;
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'classes'::regclass AND contype = 'c'
               AND pg_get_constraintdef(oid) ILIKE '%tuition_fee%'
    LOOP EXECUTE format('ALTER TABLE classes DROP CONSTRAINT %I', c.conname); END LOOP;
END $$;
ALTER TABLE classes ADD CONSTRAINT chk_class_tuition_fee CHECK (tuition_fee >= 0);

ALTER TABLE member_packages DROP CONSTRAINT IF EXISTS chk_member_packages_status;
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'member_packages'::regclass AND contype = 'c'
               AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP EXECUTE format('ALTER TABLE member_packages DROP CONSTRAINT %I', c.conname); END LOOP;
END $$;
ALTER TABLE member_packages ADD CONSTRAINT chk_member_packages_status CHECK (status IN ('Pending', 'Active', 'Expired', 'Cancelled'));

DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'class_enrollments'::regclass AND contype = 'c'
               AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP EXECUTE format('ALTER TABLE class_enrollments DROP CONSTRAINT %I', c.conname); END LOOP;
END $$;
ALTER TABLE class_enrollments ADD CONSTRAINT chk_class_enrollments_status CHECK (status IN ('Pending', 'Registered', 'Cancelled', 'Completed'));

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS class_id INT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS subscription_id INT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS invoice_code VARCHAR(30);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'invoices'::regclass AND conname = 'fk_invoice_class') THEN
        ALTER TABLE invoices ADD CONSTRAINT fk_invoice_class FOREIGN KEY (class_id) REFERENCES classes(class_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'invoices'::regclass AND conname = 'fk_invoice_subscription') THEN
        ALTER TABLE invoices ADD CONSTRAINT fk_invoice_subscription FOREIGN KEY (subscription_id) REFERENCES member_packages(subscription_id);
    END IF;
END $$;

UPDATE invoices SET invoice_code = 'INV-' || COALESCE(EXTRACT(YEAR FROM payment_date)::int, EXTRACT(YEAR FROM CURRENT_DATE)::int)::text
    || '-' || lpad(invoice_id::text, 6, '0') WHERE invoice_code IS NULL;
ALTER TABLE invoices ALTER COLUMN invoice_code SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN invoice_code SET DEFAULT ('INV-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(nextval('invoice_code_seq')::text, 6, '0'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_invoice_code ON invoices(invoice_code);
SELECT setval('invoice_code_seq', GREATEST(COALESCE((SELECT MAX((regexp_match(invoice_code, '-([0-9]+)$'))[1]::bigint) FROM invoices), 0), 1), true);

DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'invoices'::regclass AND contype = 'c'
               AND (pg_get_constraintdef(oid) ILIKE '%payment_method%'
                    OR pg_get_constraintdef(oid) ILIKE '%payment_status%'
                    OR pg_get_constraintdef(oid) ILIKE '%package_id%')
    LOOP EXECUTE format('ALTER TABLE invoices DROP CONSTRAINT %I', c.conname); END LOOP;
END $$;
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_payment_method CHECK (payment_method IN ('Cash', 'BankTransfer', 'CreditCard', 'EWallet', 'VNPay'));
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_payment_status CHECK (payment_status IN ('Pending', 'Paid', 'Failed', 'Refunded', 'Expired', 'Cancelled'));
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_target CHECK ((package_id IS NOT NULL AND class_id IS NULL) OR (package_id IS NULL AND class_id IS NOT NULL));

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_gateway_transaction_ref ON invoices(gateway_transaction_ref) WHERE gateway_transaction_ref IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_member_package_pending ON invoices(member_id, package_id) WHERE payment_status = 'Pending' AND package_id IS NOT NULL;

COMMIT;
