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

ALTER TABLE roles ADD COLUMN IF NOT EXISTS code_role VARCHAR(10);

-- Supprimer la contrainte qui limite les noms de rôles (pour permettre l'ajout de rôles personnalisés)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;

INSERT INTO roles (name, code_role)
SELECT role_name, code_role
FROM (
    VALUES
        ('ADMIN', 'ADM'),
        ('USER', 'USER'),
        ('SUPER_ADMIN', 'SADM')
) AS roles_to_insert(role_name, code_role)
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE roles.name = roles_to_insert.role_name
);

-- Rattrapage : ces rôles ont été créés avant l'ajout de code_role, donc leur
-- code_role est resté NULL. Sans ce backfill, findByCodeRole
-- (UserServiceImpl) ne trouve jamais rien et l'écran d'attribution de rôle
-- renvoie 404. Le AND code_role IS NULL évite d'écraser un code personnalisé.
-- Pas d'ON CONFLICT ici : roles n'a aucun index unique sur name ni sur
-- code_role, Postgres refuserait la clause.
UPDATE roles SET code_role = 'ADM'      WHERE name = 'ADMIN'       AND code_role IS NULL;
UPDATE roles SET code_role = 'USER'     WHERE name = 'USER'        AND code_role IS NULL;
UPDATE roles SET code_role = 'SADM'     WHERE name = 'SUPER_ADMIN' AND code_role IS NULL;

-- =========================================================
-- 3. Créer les permissions si elles n'existent pas
-- =========================================================

-- La colonne est de toute façon ajoutée par Hibernate (ddl-auto=update) avant
-- ce script ; on la déclare ici pour rester cohérent avec roles.code_role.
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS category_permission VARCHAR(50);

-- Sécurité pour les bases déjà peuplées : une ancienne version de l'entité
-- déclarait nullable = false, ce qui aurait pu poser un NOT NULL en base.
ALTER TABLE permissions ALTER COLUMN category_permission DROP NOT NULL;

ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_name_check;
ALTER TABLE permissions ADD CONSTRAINT permissions_name_check CHECK (name IN ('VIEW_USERS','MANAGE_USERS','MANAGE_ADMINS','MANAGE_ROLES','MANAGE_PROJECTS','MANAGE_PROJECT_CONTRIBUTORS','MANAGE_DIRECTIONS','MANAGE_STATUSES','VIEW_REPORTS','MANAGE_TASKS','MANAGE_COMMENTS','MANAGE_ATTACHMENTS','MANAGE_MESSAGES'));

INSERT INTO permissions (name, description, category_permission)
SELECT perm_name, perm_desc, perm_cat
FROM (
         VALUES
             ('VIEW_USERS', 'Voir la liste des utilisateurs', 'UTILISATEURS'),
             ('MANAGE_USERS', 'Créer, modifier, supprimer des utilisateurs', 'UTILISATEURS'),
             ('MANAGE_ADMINS', 'Gérer les comptes administrateurs', 'UTILISATEURS'),
             ('MANAGE_ROLES', 'Gérer les rôles et permissions', 'ORGANISATION'),
             ('MANAGE_DIRECTIONS', 'Gérer les directions', 'ORGANISATION'),
             ('MANAGE_PROJECTS', 'Créer et gérer les projets', 'PROJETS'),
             ('MANAGE_PROJECT_CONTRIBUTORS', 'Gérer les contributeurs d''un projet', 'PROJETS'),
             ('MANAGE_STATUSES', 'Gérer les statuts', 'AUTRES'),
             ('VIEW_REPORTS', 'Voir les rapports et statistiques', 'RAPPORTS'),
             ('MANAGE_TASKS', 'Créer et gérer les tâches', 'PROJETS'),
             ('MANAGE_COMMENTS', 'Commenter et gérer les commentaires', 'PROJETS'),
             ('MANAGE_ATTACHMENTS', 'Ajouter et gérer les pièces jointes', 'PROJETS'),
             ('MANAGE_MESSAGES', 'Envoyer et gérer les messages', 'AUTRES')
     ) AS permissions_to_insert(perm_name, perm_desc, perm_cat)
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE permissions.name = permissions_to_insert.perm_name
);

-- Rattrapage idempotent des permissions déjà présentes en base.
-- Couvre deux cas : les lignes dont la colonne est NULL (colonne ajoutée a
-- posteriori) ET les lignes contenant une valeur hors énumération (ex. une
-- saisie manuelle 'PROJETSTS'). Ce second cas est bloquant : avec
-- @Enumerated(EnumType.STRING), Hibernate lève
-- IllegalArgumentException: No enum constant ... dès qu'il matérialise une
-- ligne dont la valeur n'existe pas dans PermissionCategoryType, ce qui fait
-- échouer le chargement de TOUT utilisateur porteur de la permission.
-- DOIT précéder l'ajout de la contrainte CHECK ci-dessous, sinon la création
-- de la contrainte échoue sur la ligne fautive et data.sql avorte.
UPDATE permissions SET category_permission = CASE name
     WHEN 'VIEW_USERS' THEN 'UTILISATEURS'
     WHEN 'MANAGE_USERS' THEN 'UTILISATEURS'
     WHEN 'MANAGE_ADMINS' THEN 'UTILISATEURS'
     WHEN 'MANAGE_ROLES' THEN 'ORGANISATION'
     WHEN 'MANAGE_DIRECTIONS' THEN 'ORGANISATION'
     WHEN 'MANAGE_PROJECTS' THEN 'PROJETS'
     WHEN 'MANAGE_PROJECT_CONTRIBUTORS' THEN 'PROJETS'
     WHEN 'MANAGE_STATUSES' THEN 'AUTRES'
     WHEN 'VIEW_REPORTS' THEN 'RAPPORTS'
     WHEN 'MANAGE_TASKS' THEN 'PROJETS'
     WHEN 'MANAGE_COMMENTS' THEN 'PROJETS'
     WHEN 'MANAGE_ATTACHMENTS' THEN 'PROJETS'
     WHEN 'MANAGE_MESSAGES' THEN 'AUTRES'
     ELSE 'AUTRES'
    END
WHERE category_permission IS NULL
   OR category_permission NOT IN ('UTILISATEURS', 'PROJETS', 'ORGANISATION', 'RAPPORTS', 'AUTRES');

-- Empêche la réintroduction d'une valeur hors énumération.
-- Le DROP IF EXISTS est obligatoire : ce script s'exécute à chaque démarrage
-- (spring.sql.init.mode=always) et un simple ADD CONSTRAINT ferait échouer le
-- boot du deuxième démarrage.
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_category_check;
ALTER TABLE permissions ADD CONSTRAINT permissions_category_check
    CHECK (category_permission IS NULL
           OR category_permission IN ('UTILISATEURS', 'PROJETS', 'ORGANISATION', 'RAPPORTS', 'AUTRES'));

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
  AND p.name IN ('VIEW_USERS', 'MANAGE_USERS', 'MANAGE_PROJECTS', 'VIEW_REPORTS',
                 'MANAGE_TASKS', 'MANAGE_COMMENTS', 'MANAGE_ATTACHMENTS', 'MANAGE_MESSAGES')
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

-- Les priorités sont seedées avec leur ordre métier explicite (sort_order) :
-- Urgente = 1, Haute = 2, Moyenne = 3, Basse = 4. L'identifiant (priority_id,
-- auto-incrément) ne doit jamais être utilisé comme règle d'ordre.
INSERT INTO priorities (name, sort_order)
SELECT priority_name, priority_sort_order
FROM (
    VALUES
        ('Basse', 4),
        ('Moyenne', 3),
        ('Haute', 2),
        ('Urgente', 1)
) AS priorities_to_insert(priority_name, priority_sort_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM priorities
    WHERE priorities.name = priorities_to_insert.priority_name
);

-- Rattrapage idempotent pour les bases existantes : renseigne le sort_order des
-- priorités déjà présentes (colonne ajoutée a posteriori, donc nullable).
UPDATE priorities SET sort_order = 4 WHERE name = 'Basse' AND sort_order IS NULL;
UPDATE priorities SET sort_order = 3 WHERE name = 'Moyenne' AND sort_order IS NULL;
UPDATE priorities SET sort_order = 2 WHERE name = 'Haute' AND sort_order IS NULL;
UPDATE priorities SET sort_order = 1 WHERE name = 'Urgente' AND sort_order IS NULL;

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
    r.code_role AS role
FROM users u
LEFT JOIN users_roles ur ON ur.users_users_id = u.users_id
LEFT JOIN roles r ON r.roles_id = ur.roles_roles_id
WHERE u.email IN ('admin@edbm.com', 'superadmin@edbm.com');

-- =========================================================
-- 12. Référentiel des actions (table actions + jointure permissions_actions)
-- =========================================================
-- Chaque action porte un code binaire (puissance de 2) ; les ensembles
-- d'actions sont représentés par la somme de ces codes (masque).
--   INSERT=1, SELECT=2, UPDATE=4, DELETE=8, GENERATE_REPORT=16, EXPORT=32

CREATE TABLE IF NOT EXISTS actions (
    actions_id  BIGSERIAL PRIMARY KEY,
    code_action INTEGER NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL UNIQUE,
    label       VARCHAR(100) NOT NULL
);

INSERT INTO actions (code_action, name, label)
VALUES
    (1,  'INSERT',          'Insérer'),
    (2,  'SELECT',          'Voir'),
    (4,  'UPDATE',          'Modifier'),
    (8,  'DELETE',          'Supprimer'),
    (16, 'GENERATE_REPORT', 'Générer un rapport'),
    (32, 'EXPORT',          'Exporter')
ON CONFLICT (code_action) DO NOTHING;

-- Jointure plusieurs-à-plusieurs entre modules et actions : cet ensemble définit
-- quelles actions existent pour chaque permission. Ajustable par l'API
-- PUT /roles/permissions/{id}/actions.
CREATE TABLE IF NOT EXISTS permissions_actions (
    permission_id BIGINT NOT NULL REFERENCES permissions (permission_id),
    actions_id    BIGINT NOT NULL REFERENCES actions (actions_id),
    PRIMARY KEY (permission_id, actions_id)
);

-- Seed de la jointure, uniquement si elle est encore vide (création initiale).
-- Une configuration ajustée via l'API n'est donc jamais réécrasée au démarrage.
INSERT INTO permissions_actions (permission_id, actions_id)
SELECT p.permission_id, a.actions_id
FROM (
         VALUES
             ('VIEW_USERS', 'SELECT'),
             ('MANAGE_USERS', 'SELECT'), ('MANAGE_USERS', 'INSERT'), ('MANAGE_USERS', 'UPDATE'),
             ('MANAGE_ADMINS', 'SELECT'), ('MANAGE_ADMINS', 'INSERT'), ('MANAGE_ADMINS', 'UPDATE'),
             ('MANAGE_ROLES', 'SELECT'), ('MANAGE_ROLES', 'INSERT'), ('MANAGE_ROLES', 'UPDATE'),
             ('MANAGE_PROJECTS', 'SELECT'), ('MANAGE_PROJECTS', 'INSERT'), ('MANAGE_PROJECTS', 'UPDATE'), ('MANAGE_PROJECTS', 'DELETE'),
             ('MANAGE_PROJECT_CONTRIBUTORS', 'SELECT'), ('MANAGE_PROJECT_CONTRIBUTORS', 'INSERT'), ('MANAGE_PROJECT_CONTRIBUTORS', 'DELETE'),
             ('MANAGE_DIRECTIONS', 'SELECT'), ('MANAGE_DIRECTIONS', 'INSERT'), ('MANAGE_DIRECTIONS', 'DELETE'),
             ('MANAGE_STATUSES', 'SELECT'), ('MANAGE_STATUSES', 'INSERT'), ('MANAGE_STATUSES', 'DELETE'),
             ('VIEW_REPORTS', 'SELECT'), ('VIEW_REPORTS', 'GENERATE_REPORT'), ('VIEW_REPORTS', 'EXPORT'),
             ('MANAGE_TASKS', 'SELECT'), ('MANAGE_TASKS', 'INSERT'), ('MANAGE_TASKS', 'UPDATE'), ('MANAGE_TASKS', 'DELETE'),
             ('MANAGE_COMMENTS', 'SELECT'), ('MANAGE_COMMENTS', 'INSERT'), ('MANAGE_COMMENTS', 'UPDATE'), ('MANAGE_COMMENTS', 'DELETE'),
             ('MANAGE_ATTACHMENTS', 'SELECT'), ('MANAGE_ATTACHMENTS', 'INSERT'), ('MANAGE_ATTACHMENTS', 'DELETE'), ('MANAGE_ATTACHMENTS', 'EXPORT'),
             ('MANAGE_MESSAGES', 'SELECT'), ('MANAGE_MESSAGES', 'INSERT'), ('MANAGE_MESSAGES', 'DELETE'), ('MANAGE_MESSAGES', 'EXPORT')
     ) AS mapping(perm_name, action_name)
JOIN permissions p ON p.name = mapping.perm_name
JOIN actions a ON a.name = mapping.action_name
WHERE NOT EXISTS (SELECT 1 FROM permissions_actions);

-- Total des actions disponibles par module (masque dénormalisé).
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS total_actions INTEGER;

UPDATE permissions p
SET total_actions = COALESCE((
        SELECT SUM(a.code_action)
        FROM permissions_actions pa
        JOIN actions a ON a.actions_id = pa.actions_id
        WHERE pa.permission_id = p.permission_id
    ), 0)
WHERE p.total_actions IS NULL;

-- =========================================================
-- 13. Actions par défaut des rôles (colonne sur roles_permissions)
-- =========================================================

-- La colonne est normalement déjà ajoutée par Hibernate (ddl-auto=update) ;
-- le IF NOT EXISTS garantit l'exécution du script même dans le cas contraire.
-- Elle contient le masque binaire des actions accordées (somme des code_action).
ALTER TABLE roles_permissions ADD COLUMN IF NOT EXISTS actions INTEGER;

-- Seed idempotent, limité aux lignes dont la colonne est encore NULL (créées
-- avant l'introduction de la fonctionnalité). Toute configuration définie via
-- l'API (y compris un masque 0) n'est jamais écrasée.
-- Les actions par défaut dépendent du module (cf. table permissions_actions) :
-- chaque permission reçoit son propre masque, et non plus le jeu générique
-- complet. On garde ainsi la base cohérente avec la validation de
-- PUT /roles/{id}/actions dès le premier démarrage.
UPDATE roles_permissions rp
SET actions = CASE p.name
    WHEN 'VIEW_USERS' THEN 2
    WHEN 'MANAGE_USERS' THEN 7
    WHEN 'MANAGE_ADMINS' THEN 7
    WHEN 'MANAGE_ROLES' THEN 7
    WHEN 'MANAGE_PROJECTS' THEN 15
    WHEN 'MANAGE_PROJECT_CONTRIBUTORS' THEN 11
    WHEN 'MANAGE_DIRECTIONS' THEN 11
    WHEN 'MANAGE_STATUSES' THEN 11
    WHEN 'VIEW_REPORTS' THEN 50
    WHEN 'MANAGE_TASKS' THEN 15
    WHEN 'MANAGE_COMMENTS' THEN 15
    WHEN 'MANAGE_ATTACHMENTS' THEN 43
    WHEN 'MANAGE_MESSAGES' THEN 43
    ELSE 2
END
FROM roles r, permissions p
WHERE rp.roles_id = r.roles_id
  AND rp.permission_id = p.permission_id
  AND r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND rp.actions IS NULL;

-- =========================================================
-- 14. Grandfathering de l'enforcement (actions Tâches/Commentaires/
--     Pièces jointes/Messages)
-- =========================================================
-- Les contrôleurs Tâches/Commentaires/Pièces jointes/Messages appliquent
-- désormais des @PreAuthorize du type hasAction('MANAGE_TASKS','INSERT').
-- Or, avant cette fonctionnalité, ces endpoints n'étaient protégés que par
-- « authenticated() » : tout rôle pouvait écrire. Accorder ces modules à TOUS
-- les rôles, avec l'ensemble des actions réellement supportées par chacun
-- (cf. table permissions_actions), garantit que l'activation de l'enforcement
-- ne RETIRE l'accès à personne (aucune régression). Les administrateurs
-- pourront ensuite affiner via l'API /roles/{id}/actions.

INSERT INTO roles_permissions (roles_id, permission_id)
SELECT r.roles_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE p.name IN ('MANAGE_TASKS', 'MANAGE_COMMENTS', 'MANAGE_ATTACHMENTS', 'MANAGE_MESSAGES')
  AND NOT EXISTS (
      SELECT 1
      FROM roles_permissions rp
      WHERE rp.roles_id = r.roles_id
        AND rp.permission_id = p.permission_id
  );

-- Renseigne les actions de ces associations quand elles viennent d'être
-- créées (NULL). Une configuration déjà définie (y compris un masque 0) n'est
-- pas écrasée.
UPDATE roles_permissions rp
SET actions = CASE p.name
    WHEN 'MANAGE_TASKS' THEN 15
    WHEN 'MANAGE_COMMENTS' THEN 15
    WHEN 'MANAGE_ATTACHMENTS' THEN 43
    WHEN 'MANAGE_MESSAGES' THEN 43
END
FROM permissions p
WHERE rp.permission_id = p.permission_id
  AND p.name IN ('MANAGE_TASKS', 'MANAGE_COMMENTS', 'MANAGE_ATTACHMENTS', 'MANAGE_MESSAGES')
  AND rp.actions IS NULL;
