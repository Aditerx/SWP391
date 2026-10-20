BEGIN;

INSERT INTO permissions (permission_name, description) VALUES
    ('MANAGE_PERMISSIONS', 'Chi quan tri vien duoc cau hinh ma tran phan quyen'),
    ('MANAGE_INVOICES', 'Quan ly va xem hoa don cua cac thanh vien')
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_name = 'MANAGE_PERMISSIONS'
WHERE UPPER(r.role_name) = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.permission_name = 'MANAGE_INVOICES'
WHERE UPPER(r.role_name) IN ('CENTERMANAGER', 'RECEPTIONIST')
ON CONFLICT DO NOTHING;

COMMIT;