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

-- La contrainte de catégorie est retirée AVANT tous les UPDATE ci-dessous : le
-- script s'exécute à chaque démarrage et la contrainte laissée par le boot
-- précédent n'autorise pas encore la nouvelle catégorie COMMENTAIRE. Sans ce
-- DROP anticipé, les UPDATE qui écrivent COMMENTAIRE échoueraient. La contrainte
-- est recréée (COMMENTAIRE inclus) après les UPDATE.
-- Hibernate (ddl-auto=update) génère par ailleurs sa propre contrainte pour
-- l'enum @Enumerated(STRING) sous le nom permissions_category_permission_check
-- et ne la met PAS à jour quand l'énumération gagne une valeur : il faut donc
-- également la retirer sous peine de blocage à l'écriture de COMMENTAIRE.
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_category_check;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_category_permission_check;

-- Suppression de la permission redondante VIEW_USERS : le droit de consultation
-- est désormais porté par l'action SELECT de MANAGE_USERS. Les clés étrangères
-- imposent de purger d'abord les associations dépendantes (rôles et actions),
-- puis la permission elle-même. Idempotent. DOIT précéder la recréation de
-- permissions_name_check (qui n'autorise plus VIEW_USERS), sinon l'ajout de la
-- contrainte échouerait sur la ligne encore présente.
DELETE FROM roles_permissions
WHERE permission_id IN (SELECT permission_id FROM permissions WHERE name = 'VIEW_USERS');

DELETE FROM permissions_actions
WHERE permission_id IN (SELECT permission_id FROM permissions WHERE name = 'VIEW_USERS');

DELETE FROM permissions WHERE name = 'VIEW_USERS';

ALTER TABLE permissions DROP CONSTRAINT IF EXISTS permissions_name_check;
ALTER TABLE permissions ADD CONSTRAINT permissions_name_check CHECK (name IN ('MANAGE_USERS','MANAGE_ADMINS','MANAGE_ROLES','MANAGE_PROJECTS','MANAGE_PROJECT_CONTRIBUTORS','MANAGE_DIRECTIONS','MANAGE_STATUSES','VIEW_REPORTS','MANAGE_GLOBAL_REPORTS','MANAGE_TASKS','MANAGE_COMMENTS','MANAGE_ATTACHMENTS','MANAGE_MESSAGES'));

INSERT INTO permissions (name, description, category_permission)
SELECT perm_name, perm_desc, perm_cat
FROM (
         VALUES
             ('MANAGE_USERS', 'Créer, modifier, supprimer des utilisateurs', 'UTILISATEURS'),
             ('MANAGE_ADMINS', 'Gérer les comptes administrateurs', 'UTILISATEURS'),
             ('MANAGE_ROLES', 'Gérer les rôles et permissions', 'ORGANISATION'),
             ('MANAGE_DIRECTIONS', 'Gérer les directions', 'ORGANISATION'),
             ('MANAGE_PROJECTS', 'Créer et gérer les projets', 'PROJETS'),
             ('MANAGE_PROJECT_CONTRIBUTORS', 'Gérer les contributeurs d''un projet', 'PROJETS'),
             ('MANAGE_STATUSES', 'Gérer les statuts', 'AUTRES'),
             ('VIEW_REPORTS', 'Consulter mes rapports et statistiques personnelles', 'RAPPORTS'),
             ('MANAGE_GLOBAL_REPORTS', 'Consulter et exporter les rapports globaux de la plateforme (tableau de bord super administrateur)', 'RAPPORTS'),
             ('MANAGE_TASKS', 'Créer et gérer les tâches', 'PROJETS'),
             ('MANAGE_COMMENTS', 'Commenter et gérer les commentaires', 'COMMENTAIRE'),
             ('MANAGE_ATTACHMENTS', 'Ajouter et gérer les pièces jointes', 'COMMENTAIRE'),
             ('MANAGE_MESSAGES', 'Envoyer et gérer les messages', 'AUTRES')
     ) AS permissions_to_insert(perm_name, perm_desc, perm_cat)
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE permissions.name = permissions_to_insert.perm_name
);

-- Rattrapage idempotent des descriptions : l'INSERT ci-dessus ne s'applique
-- qu'aux permissions encore absentes en base, la description d'une permission
-- existante ne serait donc jamais mise a jour.
UPDATE permissions SET description = CASE name
    WHEN 'VIEW_REPORTS' THEN 'Consulter mes rapports et statistiques personnelles'
    WHEN 'MANAGE_GLOBAL_REPORTS' THEN 'Consulter et exporter les rapports globaux de la plateforme (tableau de bord super administrateur)'
    ELSE description
END
WHERE name IN ('VIEW_REPORTS', 'MANAGE_GLOBAL_REPORTS');

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
     WHEN 'MANAGE_USERS' THEN 'UTILISATEURS'
     WHEN 'MANAGE_ADMINS' THEN 'UTILISATEURS'
     WHEN 'MANAGE_ROLES' THEN 'ORGANISATION'
     WHEN 'MANAGE_DIRECTIONS' THEN 'ORGANISATION'
     WHEN 'MANAGE_PROJECTS' THEN 'PROJETS'
     WHEN 'MANAGE_PROJECT_CONTRIBUTORS' THEN 'PROJETS'
     WHEN 'MANAGE_STATUSES' THEN 'AUTRES'
     WHEN 'VIEW_REPORTS' THEN 'RAPPORTS'
     WHEN 'MANAGE_GLOBAL_REPORTS' THEN 'RAPPORTS'
     WHEN 'MANAGE_TASKS' THEN 'PROJETS'
     WHEN 'MANAGE_COMMENTS' THEN 'COMMENTAIRE'
     WHEN 'MANAGE_ATTACHMENTS' THEN 'COMMENTAIRE'
     WHEN 'MANAGE_MESSAGES' THEN 'AUTRES'
     ELSE 'AUTRES'
    END
WHERE category_permission IS NULL
   OR category_permission NOT IN ('UTILISATEURS', 'PROJETS', 'COMMENTAIRE', 'ORGANISATION', 'RAPPORTS', 'AUTRES');

-- Migration : MANAGE_COMMENTS et MANAGE_ATTACHMENTS rejoignent la catégorie
-- COMMENTAIRE. Les lignes déjà en base portaient 'PROJETS' et ne sont donc pas
-- rattrapées par l'UPDATE ci-dessus (leur valeur était valide). La contrainte
-- de catégorie ayant été retirée en début de section, l'écriture est autorisée.
-- Idempotent : ne s'applique que si la valeur diffère déjà de COMMENTAIRE.
UPDATE permissions
SET category_permission = 'COMMENTAIRE'
WHERE name IN ('MANAGE_COMMENTS', 'MANAGE_ATTACHMENTS')
  AND category_permission IS DISTINCT FROM 'COMMENTAIRE';

-- Recrée la contrainte en autorisant la catégorie COMMENTAIRE.
ALTER TABLE permissions ADD CONSTRAINT permissions_category_check
    CHECK (category_permission IS NULL
           OR category_permission IN ('UTILISATEURS', 'PROJETS', 'COMMENTAIRE', 'ORGANISATION', 'RAPPORTS', 'AUTRES'));

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
  AND p.name IN ('MANAGE_USERS', 'MANAGE_PROJECTS', 'VIEW_REPORTS',
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
-- Le code binaire (code_action) n'est plus stocké : il est CALCULÉ en Java
-- depuis la position de l'action (ActionType.getCode() = 1 << bitIndex).
-- Correspondance figée (append-only) :
--   INSERT=1, SELECT=2, UPDATE=4, DELETE=8, EXPORT=16, DOWNLOAD=32, ARCHIVE=64
--   (ancienne action GENERATE_REPORT supprimée — redondante avec EXPORT ; ses
--    codes ont été réalignés par la migration « actions-realign-v2 »)
-- L'ordre de déclaration de l'enum ActionType est la source de vérité.

CREATE TABLE IF NOT EXISTS actions (
    actions_id  BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    label       VARCHAR(100) NOT NULL
);

-- Migration : retire la colonne code_action des bases existantes (no-op si absente).
ALTER TABLE actions DROP COLUMN IF EXISTS code_action;

INSERT INTO actions (name, label)
VALUES
    ('INSERT',   'Insérer'),
    ('SELECT',   'Voir'),
    ('UPDATE',   'Modifier'),
    ('DELETE',   'Supprimer'),
    ('EXPORT',   'Exporter'),
    ('DOWNLOAD', 'Télécharger'),
    ('ARCHIVE',  'Archiver')
ON CONFLICT (name) DO NOTHING;

-- Migration idempotente du référentiel :
--   - suppression de GENERATE_REPORT (l'export suffit) — détachée d'abord de
--     permissions_actions à cause de la clé étrangère ;
--   - les nouvelles actions DOWNLOAD et ARCHIVE sont déjà insérées ci-dessus.
DELETE FROM permissions_actions pa
USING actions a
WHERE pa.actions_id = a.actions_id
  AND a.name = 'GENERATE_REPORT';

DELETE FROM actions WHERE name = 'GENERATE_REPORT';

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
             ('MANAGE_USERS', 'SELECT'), ('MANAGE_USERS', 'INSERT'), ('MANAGE_USERS', 'UPDATE'),
             ('MANAGE_ADMINS', 'SELECT'), ('MANAGE_ADMINS', 'INSERT'), ('MANAGE_ADMINS', 'UPDATE'),
             ('MANAGE_ROLES', 'SELECT'), ('MANAGE_ROLES', 'INSERT'), ('MANAGE_ROLES', 'UPDATE'),
              ('MANAGE_PROJECTS', 'SELECT'), ('MANAGE_PROJECTS', 'INSERT'), ('MANAGE_PROJECTS', 'UPDATE'), ('MANAGE_PROJECTS', 'ARCHIVE'),
             ('MANAGE_PROJECT_CONTRIBUTORS', 'SELECT'), ('MANAGE_PROJECT_CONTRIBUTORS', 'INSERT'), ('MANAGE_PROJECT_CONTRIBUTORS', 'DELETE'),
             ('MANAGE_DIRECTIONS', 'SELECT'), ('MANAGE_DIRECTIONS', 'INSERT'), ('MANAGE_DIRECTIONS', 'DELETE'),
             ('MANAGE_STATUSES', 'SELECT'), ('MANAGE_STATUSES', 'INSERT'), ('MANAGE_STATUSES', 'DELETE'),
              ('VIEW_REPORTS', 'SELECT'), ('VIEW_REPORTS', 'EXPORT'),
              ('MANAGE_GLOBAL_REPORTS', 'SELECT'), ('MANAGE_GLOBAL_REPORTS', 'EXPORT'),
             ('MANAGE_TASKS', 'SELECT'), ('MANAGE_TASKS', 'INSERT'), ('MANAGE_TASKS', 'UPDATE'), ('MANAGE_TASKS', 'DELETE'),
             ('MANAGE_COMMENTS', 'SELECT'), ('MANAGE_COMMENTS', 'INSERT'), ('MANAGE_COMMENTS', 'UPDATE'), ('MANAGE_COMMENTS', 'DELETE'),
              ('MANAGE_ATTACHMENTS', 'SELECT'), ('MANAGE_ATTACHMENTS', 'INSERT'), ('MANAGE_ATTACHMENTS', 'DELETE'), ('MANAGE_ATTACHMENTS', 'DOWNLOAD'),
             ('MANAGE_MESSAGES', 'SELECT'), ('MANAGE_MESSAGES', 'INSERT'), ('MANAGE_MESSAGES', 'DELETE'), ('MANAGE_MESSAGES', 'EXPORT')
     ) AS mapping(perm_name, action_name)
JOIN permissions p ON p.name = mapping.perm_name
JOIN actions a ON a.name = mapping.action_name
WHERE NOT EXISTS (SELECT 1 FROM permissions_actions);

-- Total des actions disponibles par module (masque dénormalisé).
-- La colonne est créée ici, mais sa VALEUR est calculée en Java au démarrage
-- par PermissionActionMaskInitializer : le code binaire n'existant plus en base,
-- on ne peut plus le sommer en SQL (SUM(a.code_action)).
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS total_actions INTEGER;

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
-- Masques exprimés dans l'AVANT-réalignement (EXPORT=32, DOWNLOAD=64,
-- ARCHIVE=128) : la section 17 les traduit vers les codes denses
-- (EXPORT=16, DOWNLOAD=32, ARCHIVE=64). Base neuve et base existante
-- convergent donc vers exactement les mêmes valeurs après la migration.
UPDATE roles_permissions rp
SET actions = CASE p.name
    WHEN 'MANAGE_USERS' THEN 7
    WHEN 'MANAGE_ADMINS' THEN 7
    WHEN 'MANAGE_ROLES' THEN 7
    WHEN 'MANAGE_PROJECTS' THEN 135
    WHEN 'MANAGE_PROJECT_CONTRIBUTORS' THEN 11
    WHEN 'MANAGE_DIRECTIONS' THEN 11
    WHEN 'MANAGE_STATUSES' THEN 11
    WHEN 'VIEW_REPORTS' THEN 34
    WHEN 'MANAGE_GLOBAL_REPORTS' THEN 18
    WHEN 'MANAGE_TASKS' THEN 15
    WHEN 'MANAGE_COMMENTS' THEN 15
    WHEN 'MANAGE_ATTACHMENTS' THEN 75
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
-- pas écrasée. Valeurs avant-réalignement, traduites par la section 17.
UPDATE roles_permissions rp
SET actions = CASE p.name
    WHEN 'MANAGE_TASKS' THEN 15
    WHEN 'MANAGE_COMMENTS' THEN 15
    WHEN 'MANAGE_ATTACHMENTS' THEN 75
    WHEN 'MANAGE_MESSAGES' THEN 43
END
FROM permissions p
WHERE rp.permission_id = p.permission_id
  AND p.name IN ('MANAGE_TASKS', 'MANAGE_COMMENTS', 'MANAGE_ATTACHMENTS', 'MANAGE_MESSAGES')
  AND rp.actions IS NULL;

-- =========================================================
-- 15. Prérequis « Voir » (SELECT) sur les masques accordés
-- =========================================================
-- Règle métier : une action n'est utilisable que si l'action SELECT (code 2)
-- est également accordée — sans accès en lecture (page masquée), l'utilisateur
-- ne peut ni insérer, ni modifier, ni supprimer, etc. Un masque dépourvu du
-- bit SELECT est donc inexploitable et neutralisé (masque = 0).
-- Le backend applique déjà cette règle à l'enforcement
-- (PermissionEvaluator.hasAction), au calcul des masques accordés
-- (AuthController.buildPermissionSummaries, RoleServiceImpl.toRoleActions) et
-- à l'écriture (RoleServiceImpl.setRoleActions). Cette migration met la base
-- en cohérence pour les lignes historiques qui porteraient d'autres bits sans
-- SELECT. Idempotent : ne touche que les lignes concernées (bit 2 absent et
-- masque déjà non nul).
UPDATE roles_permissions
SET actions = 0
WHERE actions IS NOT NULL
  AND actions <> 0
  AND (actions & 2) = 0;

-- =========================================================
-- 16. Réalignement du catalogue d'actions sur le métier réel
-- =========================================================
-- Trois corrections du référentiel :
--   a) PIECES JOINTES : l'action pertinente est TELECHARGER (DOWNLOAD, 64),
--      plus EXPORTER (EXPORT, 32) ;
--   b) PROJETS : on n'efface jamais un projet, on l'archive -> ARCHIVE (128)
--      remplace DELETE (8) ;
--   c) RAPPORTS : "Generer un rapport" (16) est redondant avec "Exporter" (32),
--      l'action est supprimee (detachee puis retiree en section 12).
-- Les attaches/detaches de permissions_actions sont idempotents et ne
-- concernent que leur permission. Les migrations de masques ci-dessous ne le
-- sont PAS : on les gate donc avec un marqueur (une seule application).

-- a) + b) : rattache les nouvelles actions.
INSERT INTO permissions_actions (permission_id, actions_id)
SELECT p.permission_id, a.actions_id
FROM permissions p, actions a
WHERE (p.name = 'MANAGE_ATTACHMENTS' AND a.name = 'DOWNLOAD')
   OR (p.name = 'MANAGE_PROJECTS'    AND a.name = 'ARCHIVE')
ON CONFLICT DO NOTHING;

-- a) + b) : detache les anciennes actions de ces permissions.
DELETE FROM permissions_actions pa
USING permissions p, actions a
WHERE pa.permission_id = p.permission_id
  AND pa.actions_id    = a.actions_id
  AND ((p.name = 'MANAGE_ATTACHMENTS' AND a.name = 'EXPORT')
    OR (p.name = 'MANAGE_PROJECTS'    AND a.name = 'DELETE'));

-- Migration des masques DEJA ACCORDES : sans elle, les bits des anciennes
-- actions resteraient morts dans roles_permissions.actions et les rôles
-- perdraient l'accès qu'ils avaient.
--
-- ATTENTION : ces trois UPDATE ne sont PAS idempotents (les bits EXPORT=32,
-- DOWNLOAD=64 et ARCHIVE=128 sont reutilises par la section 17, qui re-aligne
-- ensuite les codes). On les gate avec un marqueur de migration : ils ne
-- s'appliquent qu'une seule fois, sur les bases anterieures a la v1.

CREATE TABLE IF NOT EXISTS app_migrations (
    key        TEXT PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT now()
);

-- a) MANAGE_ATTACHMENTS : EXPORT (32) devient DOWNLOAD (64) -> 43 devient 75.
UPDATE roles_permissions rp
SET actions = rp.actions - 32 + 64
FROM permissions p
WHERE rp.permission_id = p.permission_id
  AND p.name = 'MANAGE_ATTACHMENTS'
  AND (rp.actions & 32) <> 0
  AND (rp.actions & 64) = 0
  AND NOT EXISTS (SELECT 1 FROM app_migrations
                  WHERE key = 'actions-download-archive-v1');

-- b) MANAGE_PROJECTS : DELETE (8) devient ARCHIVE (128) -> 15 devient 135.
UPDATE roles_permissions rp
SET actions = rp.actions - 8 + 128
FROM permissions p
WHERE rp.permission_id = p.permission_id
  AND p.name = 'MANAGE_PROJECTS'
  AND (rp.actions & 8) <> 0
  AND (rp.actions & 128) = 0
  AND NOT EXISTS (SELECT 1 FROM app_migrations
                  WHERE key = 'actions-download-archive-v1');

-- c) VIEW_REPORTS : bit GENERATE_REPORT (16) retire -> 50 devient 34.
UPDATE roles_permissions rp
SET actions = rp.actions - 16
FROM permissions p
WHERE rp.permission_id = p.permission_id
  AND p.name = 'VIEW_REPORTS'
  AND (rp.actions & 16) <> 0
  AND NOT EXISTS (SELECT 1 FROM app_migrations
                  WHERE key = 'actions-download-archive-v1');

INSERT INTO app_migrations (key) VALUES ('actions-download-archive-v1')
ON CONFLICT DO NOTHING;

-- =========================================================
-- 17. Realignement des codes d'actions (fin du trou du bit 4)
-- =========================================================
-- L'action GENERATE_REPORT (16), supprimee en section 12/16, laissait un "trou"
-- a l'index 4 de l'enum ActionType. On referme la suite pour retrouver une
-- numerotation dense :
--     EXPORT   32 -> 16
--     DOWNLOAD 64 -> 32
--     ARCHIVE 128 -> 64
-- (INSERT/SELECT/UPDATE/DELETE = 1/2/4/8 sont inchanges ; un eventuel bit 16
--  mort est abandonne au passage.)
--
-- Les masques accordes vivent dans roles_permissions.actions : ils doivent
-- etre traduits bit a bit. permissions.total_actions n'a rien a faire :
-- PermissionActionMaskInitializer le recalcule au demarrage.
--
-- ATTENTION : cette traduction n'est PAS idempotente (une seconde application
-- corromprait les masques). On la "gate" donc avec une table de marquage :
-- elle ne s'execute qu'une seule fois, a la premiere execution du script.

CREATE TABLE IF NOT EXISTS app_migrations (
    key        TEXT PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT now()
);

UPDATE roles_permissions
SET actions = (actions & 15) | ((actions & 224) >> 1)
WHERE actions IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM app_migrations WHERE key = 'actions-realign-v2');

INSERT INTO app_migrations (key) VALUES ('actions-realign-v2')
ON CONFLICT DO NOTHING;

-- =========================================================
-- 18. Permission « rapports globaux » (dashboard super administrateur)
-- =========================================================
-- Nouvelle permission MANAGE_GLOBAL_REPORTS (categorie RAPPORTS), reservee a
-- SUPER_ADMIN (la section 4 l'accorde automatiquement). Le seed de la section
-- 12 ne s'appliquant que sur une table permissions_actions vide, on rattache
-- ici les actions SELECT + EXPORT de facon idempotente pour les bases
-- existantes (masque 18).

INSERT INTO permissions_actions (permission_id, actions_id)
SELECT p.permission_id, a.actions_id
FROM permissions p, actions a
WHERE p.name = 'MANAGE_GLOBAL_REPORTS'
  AND a.name IN ('SELECT', 'EXPORT')
ON CONFLICT DO NOTHING;
