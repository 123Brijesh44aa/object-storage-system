INSERT INTO permissions (name,description) VALUES
                                               ('storage:read', 'Read files and buckets'),
                                               ('storage:write', 'Upload files'),
                                               ('storage:delete', 'Delete files and buckets'),
                                               ('metadata:read','Read file metadata'),
                                               ('metadata:write','Write file metadata');

-- Give ROLE_USER basic storage permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('storage:read','storage:write','metadata:read')
WHERE r.name = 'ROLE_USER';

-- ROLE_ADMIN already has all permissions via V4 seed
-- Add new ones to admin too
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,p.id
FROM roles r
JOIN permissions p ON p.name IN ('storage:read','storage:write','storage:delete','metadata:read','metadata:write')
WHERE r.name = 'ROLE_ADMIN';