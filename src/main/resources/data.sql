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
-- 2. Créer le rôle ADMIN s'il n'existe pas
-- =========================================================

INSERT INTO roles (name)
SELECT role_name
FROM (
    VALUES
        ('ADMIN')
) AS roles_to_insert(role_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE roles.name = roles_to_insert.role_name
);

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
-- 3. Créer l'utilisateur ADMIN
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
    '$2a$10$whzYKQHQ83Ec0KocU07wSukh/RAGfJFdrXDm.R4.AZ4kO59bQIfty',
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
-- 4. Associer l'utilisateur au rôle ADMIN
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
-- 5. Vérifier le compte créé
-- =========================================================

SELECT
    u.users_id,
    u.email,
    u.firstname,
    u.lastname,
    u.is_active,
    u.status,
    d.name AS direction,
    r.name AS role
FROM users u
LEFT JOIN direction d
    ON d.direction_id = u.direction_id
LEFT JOIN users_roles ur
    ON ur.users_users_id = u.users_id
LEFT JOIN roles r
    ON r.roles_id = ur.roles_roles_id
WHERE u.email = 'admin@edbm.com';
