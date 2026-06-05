INSERT INTO permissions (name, description)
VALUES ('user:read', 'Read user information'),
       ('user:update', 'Update user information'),
       ('user:delete', 'Delete users'),
       ('role:read', 'Read roles'),
       ('role:manage', 'Create and manage roles'),
       ('permission:manage', 'Manage permissions');

INSERT INTO roles (name, description)
VALUES ('ROLE_USER', 'Standard user'),
       ('ROLE_ADMIN', 'Administrator with full access');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ROLE_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN ('user:read', 'user:update')
WHERE r.name = 'ROLE_USER';