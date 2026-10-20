ALTER TABLE subjects
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'Active',
    ADD COLUMN IF NOT EXISTS slug VARCHAR(120) NULL;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS subjects_status_check;
ALTER TABLE subjects ADD CONSTRAINT subjects_status_check CHECK (status IN ('Active', 'Inactive'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_subjects_slug ON subjects(slug) WHERE slug IS NOT NULL;

CREATE TABLE IF NOT EXISTS specializations (
    specialization_id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    subject_id INT NULL REFERENCES subjects(subject_id),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive'))
);
CREATE TABLE IF NOT EXISTS coach_specializations (
    coach_id INT NOT NULL REFERENCES coaches(user_id) ON DELETE CASCADE,
    specialization_id INT NOT NULL REFERENCES specializations(specialization_id) ON DELETE CASCADE,
    PRIMARY KEY (coach_id, specialization_id)
);

INSERT INTO specializations(name, status)
SELECT DISTINCT trim(value), 'Active'
FROM coaches c
CROSS JOIN LATERAL regexp_split_to_table(c.specialization, '[,;]') AS value
WHERE c.specialization IS NOT NULL AND trim(value) <> ''
ON CONFLICT (name) DO NOTHING;

INSERT INTO coach_specializations(coach_id, specialization_id)
SELECT c.user_id, s.specialization_id
FROM coaches c
CROSS JOIN LATERAL regexp_split_to_table(c.specialization, '[,;]') AS value
JOIN specializations s ON lower(s.name) = lower(trim(value))
WHERE c.specialization IS NOT NULL AND trim(value) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO permissions(permission_name, description) VALUES
    ('LOCK_SUBJECTS', 'Khoa hoac mo mon hoc'),
    ('MANAGE_SPECIALIZATIONS', 'Quan ly chuyen mon huan luyen vien')
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'Admin'
  AND p.permission_name IN ('LOCK_SUBJECTS', 'MANAGE_SPECIALIZATIONS')
ON CONFLICT DO NOTHING;
