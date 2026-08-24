-- =========================================================
-- 1. Créer la direction DSI si elle n'existe pas
-- =========================================================

INSERT INTO direction (name)
SELECT direction_name
FROM (
    VALUES
        ('DSI')
) AS directions_to_insert(direction_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM direction
    WHERE direction.name = directions_to_insert.direction_name
);


-- =========================================================
-- 2. Créer les rôles s'ils n'existent pas
-- =========================================================

INSERT INTO roles (name)
SELECT role_name
FROM (
    VALUES
        ('ADMIN'),
        ('USER'),
        ('SUPER_ADMIN')
) AS roles_to_insert(role_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE roles.name = roles_to_insert.role_name
);

-- =========================================================
-- 3. Créer les permissions si elles n'existent pas
-- =========================================================

INSERT INTO permissions (name, description)
SELECT perm_name, perm_desc
FROM (
    VALUES
        ('VIEW_USERS', 'Voir la liste des utilisateurs'),
        ('MANAGE_USERS', 'Créer, modifier, supprimer des utilisateurs'),
        ('MANAGE_ADMINS', 'Gérer les comptes administrateurs'),
        ('MANAGE_ROLES', 'Gérer les rôles et permissions'),
        ('MANAGE_PROJECTS', 'Créer et gérer les projets'),
        ('MANAGE_DIRECTIONS', 'Gérer les directions'),
        ('MANAGE_STATUSES', 'Gérer les statuts'),
        ('VIEW_REPORTS', 'Voir les rapports et statistiques')
) AS permissions_to_insert(perm_name, perm_desc)
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE permissions.name = permissions_to_insert.perm_name
);

-- =========================================================
-- 4. Associer toutes les permissions au rôle SUPER_ADMIN
-- =========================================================

INSERT INTO roles_permissions (roles_id, permission_id)
SELECT r.roles_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM roles_permissions rp
      WHERE rp.roles_id = r.roles_id
        AND rp.permission_id = p.permission_id
  );

-- =========================================================
-- 5. Associer permissions de base au rôle ADMIN
-- =========================================================

INSERT INTO roles_permissions (roles_id, permission_id)
SELECT r.roles_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('VIEW_USERS', 'MANAGE_USERS', 'MANAGE_PROJECTS', 'VIEW_REPORTS')
  AND NOT EXISTS (
      SELECT 1
      FROM roles_permissions rp
      WHERE rp.roles_id = r.roles_id
        AND rp.permission_id = p.permission_id
  );

-- =========================================================
-- 6. Créer l'utilisateur ADMIN
-- =========================================================

INSERT INTO users (
    created_at,
    email,
    firstname,
    gender,
    is_active,
    job,
    lastname,
    number,
    pwd,
    status,
    direction_id,
    image_path
)
SELECT
    CURRENT_DATE,
    'admin@edbm.com',
    'Admin',
    'M',
    TRUE,
    'Administrateur',
    'EDBM',
    '0340000002',
    '$2b$10$nYHzR0Uv9G3Sxr9PYWOaIOyVznpsn3jrRAIh9VkvEVvk7XFwIedQa',
    FALSE,
    d.direction_id,
    NULL
FROM direction d
WHERE d.name = 'DSI'
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE users.email = 'admin@edbm.com'
);

-- =========================================================
-- 7. Associer l'utilisateur au rôle ADMIN
-- =========================================================

INSERT INTO users_roles (
    users_users_id,
    roles_roles_id
)
SELECT
    u.users_id,
    r.roles_id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@edbm.com'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM users_roles ur
      WHERE ur.users_users_id = u.users_id
        AND ur.roles_roles_id = r.roles_id
  );

-- =========================================================
-- 8. Créer l'utilisateur SUPER_ADMIN
-- =========================================================

INSERT INTO users (
    created_at,
    email,
    firstname,
    gender,
    is_active,
    job,
    lastname,
    number,
    pwd,
    status,
    direction_id,
    image_path
)
SELECT
    CURRENT_DATE,
    'superadmin@edbm.com',
    'Super',
    'M',
    TRUE,
    'Super Administrateur',
    'EDBM',
    '0340000003',
    '$2b$10$nYHzR0Uv9G3Sxr9PYWOaIOyVznpsn3jrRAIh9VkvEVvk7XFwIedQa',
    FALSE,
    d.direction_id,
    NULL
FROM direction d
WHERE d.name = 'DSI'
AND NOT EXISTS (
    SELECT 1
    FROM users
    WHERE users.email = 'superadmin@edbm.com'
);

-- =========================================================
-- 9. Associer l'utilisateur au rôle SUPER_ADMIN
-- =========================================================

INSERT INTO users_roles (
    users_users_id,
    roles_roles_id
)
SELECT
    u.users_id,
    r.roles_id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'superadmin@edbm.com'
  AND r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM users_roles ur
      WHERE ur.users_users_id = u.users_id
        AND ur.roles_roles_id = r.roles_id
  );

-- =========================================================
-- 10. Priorités et statuts
-- =========================================================

INSERT INTO priorities (name)
SELECT priority_name
FROM (
    VALUES
        ('Basse'),
        ('Moyenne'),
        ('Haute'),
        ('Urgente')
) AS priorities_to_insert(priority_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM priorities
    WHERE priorities.name = priorities_to_insert.priority_name
);

INSERT INTO statuses (name, sort_order)
SELECT status_name, status_order
FROM (
    VALUES
        ('A faire', 1),
        ('En cours', 2),
        ('Termine', 3)
) AS statuses_to_insert(status_name, status_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM statuses
    WHERE statuses.name = statuses_to_insert.status_name
);

-- =========================================================
-- 11. Vérification
-- =========================================================

SELECT
    u.users_id,
    u.email,
    u.firstname,
    u.lastname,
    u.is_active,
    r.name AS role
FROM users u
LEFT JOIN users_roles ur ON ur.users_users_id = u.users_id
LEFT JOIN roles r ON r.roles_id = ur.roles_roles_id
WHERE u.email IN ('admin@edbm.com', 'superadmin@edbm.com');
