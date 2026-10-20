CREATE TABLE IF NOT EXISTS centers (
    center_id SERIAL PRIMARY KEY,
    center_name VARCHAR(150) NOT NULL
);

INSERT INTO centers (center_id, center_name)
VALUES (1, 'Sports Center 1')
ON CONFLICT (center_id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('centers', 'center_id'),
              GREATEST((SELECT COALESCE(MAX(center_id), 1) FROM centers), 1));

ALTER TABLE users ADD COLUMN IF NOT EXISTS center_id INT NULL;
ALTER TABLE users ALTER COLUMN center_id SET DEFAULT 1;
ALTER TABLE users ADD CONSTRAINT fk_user_center FOREIGN KEY (center_id) REFERENCES centers(center_id);
UPDATE users u
SET center_id = 1
WHERE u.center_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_id = u.role_id AND r.role_name = 'Admin');

CREATE OR REPLACE FUNCTION enforce_user_center_assignment()
RETURNS TRIGGER AS $$
DECLARE role_name_value VARCHAR(50);
BEGIN
    SELECT role_name INTO role_name_value FROM roles WHERE role_id = NEW.role_id;
    IF role_name_value IS DISTINCT FROM 'Admin' AND NEW.center_id IS NULL THEN
        RAISE EXCEPTION 'center_id is required for non-admin users';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_require_center ON users;
CREATE TRIGGER trg_users_require_center
    BEFORE INSERT OR UPDATE OF role_id, center_id ON users
    FOR EACH ROW EXECUTE FUNCTION enforce_user_center_assignment();

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS center_id INT NOT NULL DEFAULT 1 REFERENCES centers(center_id);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS center_id INT NOT NULL DEFAULT 1 REFERENCES centers(center_id);
ALTER TABLE membership_packages ADD COLUMN IF NOT EXISTS center_id INT NOT NULL DEFAULT 1 REFERENCES centers(center_id);
