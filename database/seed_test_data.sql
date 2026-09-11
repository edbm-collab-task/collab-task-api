-- =========================================================
-- SEED TEST DATA : import des taches reelles (95) depuis l'inventaire
-- Projets = 7 feuilles ; categories = prefixe [..] du titre ;
-- commentaires -> task_comments (auteur admin)
-- =========================================================

-- 2. Utilisateurs fictifs (un par responsable/equipe), mdp 12 chiffres en BCrypt
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'equipe-dev@edbm.com', 'Equipe', 'M', TRUE, 'Equipe developpement', 'Developpement', '0340000010', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'equipe-dev@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'equipe-dev@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'equipe-technique@edbm.com', 'Equipe', 'M', TRUE, 'Equipe technique', 'Technique', '0340000011', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'equipe-technique@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'equipe-technique@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'support-technique@edbm.com', 'Support', 'M', TRUE, 'Support technique', 'Technique', '0340000012', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'support-technique@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'support-technique@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'etech@edbm.com', 'E', 'M', TRUE, 'Equipe E-tech', 'Tech', '0340000013', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'etech@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'etech@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'instat@edbm.com', 'INSTAT', 'M', TRUE, 'Relais INSTAT', 'Service', '0340000014', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'instat@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'instat@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'marko@edbm.com', 'Marko', 'M', TRUE, 'Responsable metier', 'Responsable', '0340000015', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'marko@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'marko@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'tsiaro@edbm.com', 'Tsiaro', 'M', TRUE, 'Responsable metier', 'Responsable', '0340000016', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'tsiaro@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'tsiaro@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'ugd@edbm.com', 'UGD', 'M', TRUE, 'Unite de Gouvernance des Donnees', 'Service', '0340000017', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'ugd@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'ugd@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);
INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)
SELECT CURRENT_DATE, 'veronique@edbm.com', 'Veronique', 'M', TRUE, 'Responsable metier', 'Responsable', '0340000018', '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.', FALSE, d.direction_id, NULL
FROM direction d WHERE d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'veronique@edbm.com');
INSERT INTO users_roles (users_users_id, roles_roles_id)
SELECT u.users_id, r.roles_id FROM users u, roles r
WHERE u.email = 'veronique@edbm.com' AND r.name = 'USER'
AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);

-- 3. Projets (7 feuilles = 7 projets)
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'Orinasa', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Orinasa');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'SRNE', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'SRNE');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'E-Work', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'E-Work');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'MADAZEF', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'MADAZEF');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'LS-VISA', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'LS-VISA');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'Orinasa Iteration 4', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Orinasa Iteration 4');
INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)
SELECT 'Autres', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()
FROM users u, direction d
WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'
AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = 'Autres');

-- 3bis. Statuts specifiques aux projets (project_id renseigne -> supprimables dans l'app)
-- GET /statuses?projectId=X -> statuts du projet + globaux (project_id NULL)
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'A discuter avec BO après la MAJ effectuée concernant ces informations', 9, p.project_id FROM projects p
WHERE p.title = 'E-Work'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'A discuter avec BO après la MAJ effectuée concernant ces informations' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK', 4, p.project_id FROM projects p
WHERE p.title = 'E-Work'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK', 4, p.project_id FROM projects p
WHERE p.title = 'MADAZEF'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'Analyse effectuée / Développement des API en cours', 7, p.project_id FROM projects p
WHERE p.title = 'Orinasa Iteration 4'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'Analyse effectuée / Développement des API en cours' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'La passation avec l''équipe IT de l''INSTAT a été effectuée.', 8, p.project_id FROM projects p
WHERE p.title = 'Orinasa Iteration 4'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'La passation avec l''équipe IT de l''INSTAT a été effectuée.' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK', 4, p.project_id FROM projects p
WHERE p.title = 'Orinasa Iteration 4'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK - à vérifier', 5, p.project_id FROM projects p
WHERE p.title = 'Orinasa Iteration 4'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK - à vérifier' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK - pour la création', 6, p.project_id FROM projects p
WHERE p.title = 'Orinasa Iteration 4'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK - pour la création' AND st.project_id = p.project_id);
INSERT INTO statuses (name, sort_order, project_id)
SELECT 'OK', 4, p.project_id FROM projects p
WHERE p.title = 'Orinasa'
AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = 'OK' AND st.project_id = p.project_id);

-- 4. Taches (95) avec prefixe de titre [Categorie]
ALTER TABLE tasks ALTER COLUMN description TYPE TEXT;
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Référence enregistrement : Ajouter un onglet libre pour les autres actes enregistrés  Ajouter un onglet pour PV d’ouverture d’une succursale *Si possible basculement à partir de la facture proforma', NULLIF('Au niveau base de données :  Mifandray @ liste document ka tsy afaka atao dynamique. Solution : raha misy ampidirina dia tsimaintsy miteny aty @ ekipa dev dia ampidirinay ao @ table documentEnregistrement ny document mifandray amin’izay. Ohatra : efa napetraka anay ao ny PV d’ouverture d’une succursale',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Référence enregistrement : Ajouter un onglet libre pour les autres actes enregistrés  Ajouter un onglet pour PV d’ouverture d’une succursale *Si possible basculement à partir de la facture proforma' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Mettre en place l''historique de modification des dossiers de création.', NULLIF('Les agents doivent pouvoir suivre l''évolution des dossiers de création, la màj des statuts, les modifications effectuées par les usagers, etc.  Eventuellement, pouvoir retracer de manière explicite tous les évènements lors du traitement du dossier (changement de statut, panne interop, attente d’une autorisation, etc.)',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Mettre en place l''historique de modification des dossiers de création.' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Nouvelle fonctionnalité] Mettre en place l''historique de modification des dossiers de création.' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Bloquer les modifications d''un dossier après la validation d''un dossier', NULLIF('Un dossier ne doit plus être modifiable lorsque les agents l’ont marqué en recevable (Traitement OK). Pour qu''un usager puisse effectuer une modification sur un dossier recevable, ajouter une fonctionnalité qui permette à un agent de valider la modification.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Bloquer les modifications d''un dossier après la validation d''un dossier' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Bloquer les modifications d''un dossier après la validation d''un dossier' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Modification du comportement de traitement des dossiers durant l''étude de recevabilité', NULLIF('> Lorsqu’un agent prend en charge un dossier avec le statut "Nouveau" ou "Mis à jour", celui-ci passe automatiquement au statut "En cours" dès qu’un commentaire est ajouté par l’un des départements concernés. > Le statut du dossier reste "En cours" tant que les trois départements n’ont pas tous ajouté leurs commentaires. > Ce n’est qu’une fois tous les commentaires saisis par les départements que le dossier évolue vers le statut "Traitement OK" ou "Traitement KO". > Les commentaires ne sont pas envoyés individuellement au client. Ils sont regroupés dans un seul e-mail, qui est envoyé uniquement lorsque chaque département a soumis son commentaire. > Si un ou plusieurs départements ne saisissent pas leurs commentaires dans un délai de 24 heures, les commentaires déjà enregistrés sont envoyés automatiquement au client, même s’ils sont partiels.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'Termine'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Modification du comportement de traitement des dossiers durant l''étude de recevabilité' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Modification du comportement de traitement des dossiers durant l''étude de recevabilité' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO task_comments (content, task_id, author_id, parent_comment_id, created_at, updated_at)
SELECT 'Séparation des dossiers "nouveau" et "soumis"  Les dossiers restent en statut en cours tant que toutes les entités soumettent leur validation  KO si l''un d''entre les entités soumis dossier KO OK si les trois soumis OK', t.task_id, u.users_id, NULL, NOW(), NULL
FROM tasks t, users u
WHERE t.title = '[Amélioration] Modification du comportement de traitement des dossiers durant l''étude de recevabilité' AND u.email = 'admin@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_comments tc WHERE tc.task_id = t.task_id AND tc.content = 'Séparation des dossiers "nouveau" et "soumis"  Les dossiers restent en statut en cours tant que toutes les entités soumettent leur validation  KO si l''un d''entre les entités soumis dossier KO OK si les trois soumis OK');
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Modification à effectuer sur le bailleur "Personne morale" à l’étape 3', NULLIF('> Le libellé actuel "Personne morale" doit être remplacé par "Personne morale / EI". > Ajouter les nouvelles options suivantes dans la liste des formes juridiques : Association / ONG / EI / Autres > Le champ "Capital" ne doit pas être obligatoire lorsque la forme juridique sélectionnée est "ONG" ou "Association".',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Modification à effectuer sur le bailleur "Personne morale" à l’étape 3' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Modification à effectuer sur le bailleur "Personne morale" à l’étape 3' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Contrôle de doublon à l’étape 1 – Saisie de la société', NULLIF('Lorsqu’un usager saisit une nouvelle société à l’étape 1, un contrôle de doublon est effectué afin de vérifier si une société portant le même nom, appartenant au même usager, existe déjà en base de données. Si une telle société existe déjà, la saisie d’une nouvelle société n’est pas autorisée. Un message doit inviter l’usager à modifier ou poursuivre le traitement de la société déjà enregistrée.  Objectif : Éviter la création de plusieurs dossiers pour une même société appartenant à un même usager.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Contrôle de doublon à l’étape 1 – Saisie de la société' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Contrôle de doublon à l’étape 1 – Saisie de la société' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Modification du message de convocation à un rendez-vous (RDV)', NULLIF('Contenu du message à modifier : Ajouter dans le message envoyé à l’usager la mention suivante : "En cas d’indisponibilité pour honorer le rendez-vous fixé, veuillez contacter directement le standard de l’EDBM."  Restriction à vérifier : S’assurer que l’usager ne puisse pas répondre ou envoyer un message via la plateforme en retour de la convocation.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Modification du message de convocation à un rendez-vous (RDV)' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Modification du message de convocation à un rendez-vous (RDV)' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Site web] Quelques modifications à effectuer sur le site vitrine', NULLIF('Dans “ les outils à votre disposition”: - erreur de frappe: formalisée - le simulateur de frais au lieu de la simulateur  Dans la statistique sur le traitement des dossiers : - que veut dire formaliser votre entreprise dans le temps ? ( à modifier le terme si inapproprié pour le sens de la phrase)  Dans créer mon entreprise, créer mon entreprise en ligne: mode d’emploi, étape 2: - Enlever la dernière phrase sur l’envoi de message à l’Edbm. - Étape 3: enlever “pour” ( doublon)  Dans liste des documents par forme juridique : - Pour toutes formes juridiques : conformer les pièces justificatives au bail selon la liste des dossiers à fournir à jour , Pour pièces des dirigeants : enlever certificat de résidence - Pour forme juridique SARL : à vérifier et à modifier en conséquence : “ pour toutes personnes morales, les statuts est...” - Pour forme juridique SA : à ajouter administrateur sur la liste des dirigeants entre parenthèses - Pour pièces du CAC: on n’exige pas ces pièces pour les CAC personnes physiques  Dans Ressources - démarches-étape 2: - Pièces justificatives du bail à modifier conformément à la liste des dossiers à fournir à jour',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Site web] Quelques modifications à effectuer sur le site vitrine' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Site web] Quelques modifications à effectuer sur le site vitrine' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Exclusion du Commissaire aux comptes dans l’envoi de données vers NIFOnline', NULLIF('Lors des échanges de données entre les plateformes Orinasa et NIFOnline, les informations relatives au Commissaire aux comptes doivent être systématiquement exclues.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Exclusion du Commissaire aux comptes dans l’envoi de données vers NIFOnline' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Exclusion du Commissaire aux comptes dans l’envoi de données vers NIFOnline' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] À vérifier : Absence du gérant dans les statuts générés', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] À vérifier : Absence du gérant dans les statuts générés' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Bug] À vérifier : Absence du gérant dans les statuts générés' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] À vérifier : Absence de badge rouge sur les messages non lus', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] À vérifier : Absence de badge rouge sur les messages non lus' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] À vérifier : Absence de badge rouge sur les messages non lus' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Décalage des dates dans les statuts générés', NULLIF('Tout les documents généré (satut, contrat de bail), les dates affichées dans la section relative aux informations d’une personne sont erronée (date de naissance, date délivrance CIN) : elle apparaît systématiquement avec un jour de moins (J-1) par rapport à la date réelle saisie.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Décalage des dates dans les statuts générés' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Bug] Décalage des dates dans les statuts générés' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Amélioration de l’état de versement FO - FISC : Séparation des montants de paiement par mode', NULLIF('L’état de versement FO - FISC doit distinguer les montants selon le mode de paiement : Espèces et Chèques',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Amélioration de l’état de versement FO - FISC : Séparation des montants de paiement par mode' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Amélioration de l’état de versement FO - FISC : Séparation des montants de paiement par mode' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Correction du contrôle d''associé mineur', NULLIF('Par exemple, une personne née le 12/01/2003 est toujours identifiée comme mineure, alors qu’elle a 22 ans.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Correction du contrôle d''associé mineur' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Bug] Correction du contrôle d''associé mineur' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] À vérifier : Rôles d’une Société Civile (SC) et présence de la nomination statutaire', NULLIF('> Actuellement, tous les rôles disponibles s’affichent : PDG, PCA, DG, Gérant, Co-gérant, administrateur etc > L’information sur la nomination statutaire devrait être : Gérant statutaire',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'Termine'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] À vérifier : Rôles d’une Société Civile (SC) et présence de la nomination statutaire' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Bug] À vérifier : Rôles d’une Société Civile (SC) et présence de la nomination statutaire' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Support utilisateur] Support aux utilisateurs', NULLIF('Assistance technique continue aux usagers. Cette tâche consiste à répondre aux sollicitations des utilisateurs, que ce soit par téléphone, par courriel ou via les canaux internes. Les demandes concernent principalement la compréhension des démarches en ligne, la correction d’erreurs de saisie, les difficultés liées à la soumission des dossiers, ainsi que les cas particuliers bloquant le traitement. Une attention particulière est portée à l’accompagnement des usagers tout au long de la procédure, afin de garantir une utilisation correcte et fluide de la plateforme',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Haute' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Support utilisateur] Support aux utilisateurs' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Support utilisateur] Support aux utilisateurs' AND u.email = 'support-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Gestion manuelle] Abandon de dossier en doublon : Le système ne prévoit pas la suppression définitive d’un dossier, mais certains cas nécessitent l’abandon manuel de dossiers créés en double. Cela survient notamment lorsque l’utilisateur soumet une no...', NULLIF('Abandon de dossier en doublon : Le système ne prévoit pas la suppression définitive d’un dossier, mais certains cas nécessitent l’abandon manuel de dossiers créés en double. Cela survient notamment lorsque l’utilisateur soumet une nouvelle demande à la suite des observations formulées lors de l’étude de recevabilité du dossier initial. Dans ce contexte, les agents demandent l’abandon de l’un des deux dossiers identiques.  Un autre cas fréquent concerne les demandes déposées par un mandataire devenu injoignable, poussant la société à soumettre une nouvelle demande. Les agents identifient alors le dossier à conserver, et celui à abandonner.  L’opération consiste, pour les développeurs, à transférer la propriété du dossier concerné et à modifier sa dénomination sociale en y ajoutant la mention "ABANDONNÉ", après validation de Mme Mialy',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Gestion manuelle] Abandon de dossier en doublon : Le système ne prévoit pas la suppression définitive d’un dossier, mais certains cas nécessitent l’abandon manuel de dossiers créés en double. Cela survient notamment lorsque l’utilisateur soumet une no...' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Gestion manuelle] Modification de certaines informations d’un dossier en état "En attente de RDV" ou "En cours d''immatriculation", non modifiables depuis l’espace client.', NULLIF('Modification de certaines informations d’un dossier en état "En attente de RDV" ou "En cours d''immatriculation", non modifiables depuis l’espace client. Cette intervention est nécessaire lorsque des écarts sont constatés entre les informations saisies en ligne et celles figurant dans le dossier physique déposé, notamment en ce qui concerne les noms des parents des dirigeants, le numéro de la pièce d’identité, le changement de siège social, la dénomination sociale ou encore la date de fin d’exercice social. Ces modifications sont effectuées manuellement par les développeurs, à la demande des agents, afin de garantir la conformité du dossier',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Gestion manuelle] Modification de certaines informations d’un dossier en état "En attente de RDV" ou "En cours d''immatriculation", non modifiables depuis l’espace client.' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Interopérabilité] Résolution des problèmes liés à l’interopérabilité avec les plateformes externes (NIFonline, RCS-CM).', NULLIF('Résolution des problèmes liés à l’interopérabilité avec les plateformes externes (NIFonline, RCS-CM). Les incidents rencontrés concernent principalement des cas de doublons, des incohérences de format de données (numéro CIN, carte de résident) ou encore des informations non référencées dans la base de la DGI (ex. : fokontany non reconnu).',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Interopérabilité] Résolution des problèmes liés à l’interopérabilité avec les plateformes externes (NIFonline, RCS-CM).' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Reporting] Élaboration des états de versement pour les comptes des agents du Front Office d’Antananarivo (incluant initialement les paiements des antennes).', NULLIF('Élaboration des états de versement pour les comptes des agents du Front Office d’Antananarivo (incluant initialement les paiements des antennes). Les comptes utilisateurs des agents FO d’Antananarivo ont été configurés avec une visibilité étendue sur l’ensemble des dossiers au niveau national. Ainsi, lors de l’exportation des états de versement, ceux-ci incluent également les paiements effectués par les antennes régionales (ex. : Fort-Dauphin).  Pour garantir la cohérence des états de versement spécifiques à Antananarivo, un traitement manuel ou une adaptation de l’export est nécessaire afin d’exclure les paiements émis par les autres antennes.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Reporting] Élaboration des états de versement pour les comptes des agents du Front Office d’Antananarivo (incluant initialement les paiements des antennes).' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Google analytics', NULLIF('Mise à jour de Google Analytics : la configuration actuellement en place ne fonctionne plus et nécessite une actualisation afin de rétablir le suivi des statistiques.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Google analytics' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Affichage des IR sur le coté back office', NULLIF('IR  IR du regime réel  IR sans TVA',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Affichage des IR sur le coté back office' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Certaine dossier changent automatiquement en dossier en cours sans validation préalable par l''une des entités', NULLIF('Anomalie constatée : certains dossiers changent automatiquement de statut vers « En cours » sans validation préalable, ce qui entraîne une incohérence dans le flux de traitement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Certaine dossier changent automatiquement en dossier en cours sans validation préalable par l''une des entités' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Gestion des personnes listes noirs', NULLIF('Mise en place d’une fonctionnalité de gestion des personnes blacklistées, incluant l’ajout dans une liste noire et le blocage automatique de tous les dossiers concernés afin d’empêcher leur traitement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa' AND pr.name = 'Urgente' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Gestion des personnes listes noirs' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Support utilisateur] Support aux utilisateurs', NULLIF('Assistance technique continue aux usagers par téléphone, notamment sur les procédures liées au renouvellement des demandes. Cette tâche consiste à accompagner les utilisateurs dans le remplissage des informations relatives aux travailleurs, à les orienter sur la démarche à suivre pour le renouvellement d’un travailleur déjà enregistré sur la plateforme, ainsi qu’à les guider dans la soumission complète de leur dossier en ligne',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Haute' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Support utilisateur] Support aux utilisateurs' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Support utilisateur] Support aux utilisateurs' AND u.email = 'support-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Gestion manuelle] Ajustement du taux de change et du montant à payer', NULLIF('Ajustement du taux de change et du montant à payer d’un dossier en fonction de la somme effectivement versée par l’usager. Cette opération, validée en concertation avec la DAF et les agents des front offices, permet de corriger les écarts entre le montant initialement enregistré et la somme réellement apportée lors du paiement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Gestion manuelle] Ajustement du taux de change et du montant à payer' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Gestion manuelle] Ajustement du taux de change et du montant à payer' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Gestion manuelle] Modification de l''adress email associée à un compte société', NULLIF('Modification de l’adresse email associée à un compte société. Conformément à la règle en vigueur, une seule adresse email peut être liée à un compte validé par un NIF. En cas d’indisponibilité de l’adresse initiale (adresse inactive ou départ du titulaire), le changement est effectué exclusivement par l’équipe technique (développeurs), sur validation préalable de Mme Mialy  Solution proposée : permettre la modification de l’adresse e-mail via une procédure d’authentification, en saisissant les identifiants (NIF de la société ou ancienne adresse e-mail) ainsi que le mot de passe.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Gestion manuelle] Modification de l''adress email associée à un compte société' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Gestion manuelle] Modification de l''adress email associée à un compte société' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Vérification de l’impression des bulletins BE', NULLIF('Une autorisation d''emploi figure dans la liste d''imprission des bulletins BE alors qu’aucun numéro d''AE ne lui est attribué. Il faut analyser la cause de cette incohérence et corriger le comportement si nécessaire.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Vérification de l’impression des bulletins BE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Mise en place d’un système de notification par email pour les AE arrivant à échéance', NULLIF('Mettre en place un système d’envoi automatique d’emails aux utilisateurs dont l’AE (Autorisation d’Exercer) approche de la date d’échéance : > Un premier email est envoyé 6 mois avant l’expiration > Un second rappel est envoyé 1 mois avant l’expiration  Ces notifications permettront aux utilisateurs d’être informés à temps et de prendre les mesures nécessaires pour renouveler leur AE.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Mise en place d’un système de notification par email pour les AE arrivant à échéance' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Envoi d’un email de notification à l’utilisateur lorsque son dossier est recevable ou non', NULLIF('Mettre en place un système de notification par email pour informer automatiquement l’utilisateur du statut de son dossier : > Email de confirmation lorsque le dossier est recevable > Email de refus lorsque le dossier est non recevable, avec indication du ou des motifs si possible',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Envoi d’un email de notification à l’utilisateur lorsque son dossier est recevable ou non' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Envoi d’un email de notification lors de la récupération d’une AE', NULLIF('Mettre en place un système d’envoi automatique d’un email pour notifier l’utilisateur dès qu’une AE est récupérée.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Envoi d’un email de notification lors de la récupération d’une AE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration (UX)] Remplacer le libellé "Login" par "NIF" sur la page de connexion', NULLIF('Remplacer "Login" par "NIF", afin de clarifier à l’utilisateur qu’il doit saisir son Numéro d’Identification Fiscale.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration (UX)] Remplacer le libellé "Login" par "NIF" sur la page de connexion' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Étudier la possibilité de gérer plusieurs sociétés avec un seul email (mandataire/cabinet)', NULLIF('Analyser et mettre en œuvre, si possible, une fonctionnalité permettant à un mandataire ou cabinet de gérer plusieurs sociétés à l’aide d’un seul compte (même adresse email).',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Étudier la possibilité de gérer plusieurs sociétés avec un seul email (mandataire/cabinet)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Suppression des informations sur le passeport dans les documents générés (FRT)', NULLIF('Modifier la génération des documents FRT afin de supprimer les informations liées au passeport (numéro, date de délivrance, etc.).',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A discuter avec BO après la MAJ effectuée concernant ces informations'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Suppression des informations sur le passeport dans les documents générés (FRT)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration (UX)] Mettre en place un accès direct aux dossiers depuis la messagerie interne', NULLIF('Ajouter dans la fonctionnalité messagerie un lien cliquable permettant à l’utilisateur d’ouvrir directement le dossier concerné à partir du message reçu.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration (UX)] Mettre en place un accès direct aux dossiers depuis la messagerie interne' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité (UX)] Mise en place de la sauvegarde automatique des données lors du remplissage des formulaires', NULLIF('Une fonctionnalité permettant de sauvegarder automatiquement les données saisies par l’utilisateur au fur et à mesure du remplissage des formulaires Objectifs :  Éviter la perte d’informations en cas d’interruption (fermeture du navigateur, coupure réseau, etc.) Améliorer l’expérience utilisateur en permettant de reprendre facilement la saisie là où elle a été interrompue',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité (UX)] Mise en place de la sauvegarde automatique des données lors du remplissage des formulaires' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Recherche d’un salarié par numéro AE', NULLIF('Implémenter une fonctionnalité permettant de retrouver un salarié existant en saisissant le numéro de son dernier permis de travail',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Recherche d’un salarié par numéro AE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Rendre la saisie d’un associé non obligatoire sur la FRE', NULLIF('Modifier le formulaire FRE afin que la saisie d’un associé ne soit plus obligatoire lors de la création ou modification d’un dossier.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Rendre la saisie d’un associé non obligatoire sur la FRE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité (UX)] Valider un RDV via le bouton "Confirmer RDV" (Front Office)', NULLIF('Mettre en place la fonctionnalité permettant à l’utilisateur de valider un rendez-vous en cliquant directement sur un seul bouton "Confirmer RDV" depuis le front office.  Actuellement, nous avons deux boutons "confirmer RDV et En attente RDV"',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité (UX)] Valider un RDV via le bouton "Confirmer RDV" (Front Office)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Ajouter la fonctionnalité de suppression d’un dossier', NULLIF('Mettre en place la possibilité pour les utilisateurs autorisés de supprimer un dossier depuis l’interface.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Ajouter la fonctionnalité de suppression d’un dossier' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Ajouter un champ d’explications pour les variations importantes sur la FRE', NULLIF('Intégrer dans le formulaire FRE un champ permettant à l’utilisateur de renseigner des explications détaillées lorsque des variations importantes sont détectées.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Ajouter un champ d’explications pour les variations importantes sur la FRE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Ajouter la possibilité d’imprimer ou d’exporter le calendrier des RDV', NULLIF('Mettre en place une fonctionnalité permettant aux utilisateurs d’imprimer ou d’exporter le calendrier des rendez-vous depuis l’interface, pour les agents front office.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Ajouter la possibilité d’imprimer ou d’exporter le calendrier des RDV' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Mise en place du système de rejet d’une demande d’AE', NULLIF('Développement d’une fonctionnalité permettant de rejeter une demande de permis de travail, en réponse aux cas de refus émis par le ministère.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Mise en place du système de rejet d’une demande d’AE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Possibilité de supprimer une demande d’un travailleur dans un dossier', NULLIF('Mettre en place une fonctionnalité permettant de supprimer une demande spécifique liée à un travailleur au sein d’un dossier.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Possibilité de supprimer une demande d’un travailleur dans un dossier' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Possibilité de la modification du permis de travail après génération et avant envoi pour signature', NULLIF('Mettre en place une fonctionnalité qui autorise la modification des informations de l''autorisation d''emploi une fois celle-ci générée, mais avant son envoi pour signature',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Possibilité de la modification du permis de travail après génération et avant envoi pour signature' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug / Amélioration] Gestion des dossiers payés non validés par l’agent Front Office', NULLIF('Mettre en place une solution pour les cas où un dossier a été payé mais que l’agent Front Office n’a pas validé le paiement, entraînant un blocage du dossier qui reste en statut « dossier physique conforme » sans possibilité de validation. Actuellement, nous avons deux boutons "Confirmer" et "Valider le paiement"',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug / Amélioration] Gestion des dossiers payés non validés par l’agent Front Office' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug / Amélioration] Permettre la modification de la pièce "contrat de travail" après enregistrement pour les demandes de renouvellement', NULLIF('Ajouter une fonctionnalité qui autorise la modification du document "contrat de travail" d’un travailleur une fois celui-ci enregistré, uniquement dans le cadre des demandes de renouvellement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug / Amélioration] Permettre la modification de la pièce "contrat de travail" après enregistrement pour les demandes de renouvellement' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration (UX)] Avertir l’utilisateur avant de quitter la page si modifications sauvegardées mais non validées par « Mettre à jour »', NULLIF('Mettre en place une alerte pour prévenir l’utilisateur lorsqu’il quitte la page à la troisième étape, après avoir effectué des modifications, mais sans avoir cliqué sur le bouton « Mettre à jour ».',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration (UX)] Avertir l’utilisateur avant de quitter la page si modifications sauvegardées mais non validées par « Mettre à jour »' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Vérifier et corriger le blocage de soumission lié à un document manquant chez un travailleur', NULLIF('Analyser et corriger le cas où une demande ne peut pas être soumise parce qu’un document est détecté comme manquant pour un travailleur, alors que tous les autres documents sont correctement complétés.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Vérifier et corriger le blocage de soumission lié à un document manquant chez un travailleur' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug / Amélioration] Éviter le passage en « Modifications en cours » lors du téléchargement des documents générés', NULLIF('Corriger le comportement du dossier qui passe en statut « Modifications en cours » dès qu’un usager télécharge les documents générés après obtention d’un rendez-vous',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug / Amélioration] Éviter le passage en « Modifications en cours » lors du téléchargement des documents générés' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Analyse / Suivi] Mise en place de Google Analytics', NULLIF('Integration de système de suivi',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Analyse / Suivi] Mise en place de Google Analytics' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Statistique : nombre par genre (HOMME et FEMME)', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Statistique : nombre par genre (HOMME et FEMME)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Correction de la date de fin de validité des anciennes AE', NULLIF('Correction de la date de fin de validité de l’ancienne autorisation de travail pour certains travailleurs afin d’éviter les conflits dans la création des nouvelles autorisations. En effet, lorsque la date de fin de l’autorisation initiale se situe entre deux dates spécifiques (par exemple, entre le 15/08/2024 et le 15/08/2025), la nouvelle autorisation doit débuter le lendemain de la date de fin précédente (ici le 16/08/2025). Or, une saisie anticipée au 15/08/2025 provoque un conflit bloquant la création du nouveau dossier. Cette tâche consiste donc à ajuster la date de fin de l’ancienne autorisation pour garantir la continuité sans chevauchement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Correction de la date de fin de validité des anciennes AE' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Bug] Correction de la date de fin de validité des anciennes AE' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Ajout champs date de demande motivé et nominative', NULLIF('Incohérence entre la date de demande inscrite dans la lettre de demande motivée et celle figurant sur le permis de travail généré.  Solution : ajout d’un champ « date de demande motivée » modifiable depuis le back-office avant la génération du permis de travail, afin d’assurer la cohérence des informations.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Ajout champs date de demande motivé et nominative' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Ajout champs date de demande motivé et nominative' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Affichage des commentaires pour les sociétés et les travailleurs', NULLIF('Problème d’affichage des commentaires longs : lorsque la longueur du commentaire dépasse une certaine limite, celui-ci est tronqué et remplacé par des points de suspension (« … »), sans possibilité de visualiser le contenu complet.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'E-Work' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Affichage des commentaires pour les sociétés et les travailleurs' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Affichage des commentaires pour les sociétés et les travailleurs' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Suivi des autres digitalisations au niveau de l''EDBM', NULLIF('Surveillance et accompagnement des autres projets de digitalisation  Site web de l''EDBM RH EVALUATION HEAZ ETOOLIA',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Autres' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Suivi des autres digitalisations au niveau de l''EDBM' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Suivi des stagiaires', NULLIF('Encadrement et suivi des travaux des stagiaires',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Autres' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Suivi des stagiaires' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Déploiement des plateformes et les mises à jour', NULLIF('Mise en production des nouvelles plateformes',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Autres' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Déploiement des plateformes et les mises à jour' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Backup de toutes les plateformes digitalisées', NULLIF('Sauvegarde des bases de données, fichiers, documents et architectures',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Autres' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Backup de toutes les plateformes digitalisées' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = 'Backup de toutes les plateformes digitalisées' AND u.email = 'equipe-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Rédaction des documentations techniques des projets (LS-VISA, MADAZEF)', NULLIF('Préparer les documents techniques : architecture, API, modèle de données, procédures d''installation, sécurité, etc.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Autres' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Rédaction des documentations techniques des projets (LS-VISA, MADAZEF)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Ajout d’une confirmation lors de la validation d’une immatriculation avec le code activité 99909', NULLIF('Le code d’activité 99909 est utilisé par défaut dans Orinasa. Pour éviter que ce code temporaire soit validé par erreur comme activité principale, il est nécessaire d’afficher une alerte de confirmation au moment de la validation de l’immatriculation, si ce code n’a pas été modifié.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'SRNE' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Ajout d’une confirmation lors de la validation d’une immatriculation avec le code activité 99909' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Ajout d’une confirmation lors de la validation d’une immatriculation avec le code activité 99909' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Prise en charge des modèles de carte statistique pour chaque antenne', NULLIF('Actuellement, le modèle de carte statistique généré par la plateforme est adapté uniquement pour la région Analamanga (Antananarivo).  Il est nécessaire de mettre en place une configuration dynamique du modèle de carte statistique, en fonction de la localisation du siège social de la société (province ou région concernée).',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'SRNE' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Prise en charge des modèles de carte statistique pour chaque antenne' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Nouvelle fonctionnalité] Prise en charge des modèles de carte statistique pour chaque antenne' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Certificat d''existence', NULLIF('Même cas que le carte statistique',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'SRNE' AND pr.name = 'Haute' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Certificat d''existence' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Nouvelle fonctionnalité] Certificat d''existence' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Test] Exécution des tests des fonctionnalités : création, modification, radiation', NULLIF('Réaliser des tests fonctionnels pour valider le comportement de la partie création, ainsi que des fonctionnalités récemment ajoutées de modification et de radiation.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'OK - pour la création'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Test] Exécution des tests des fonctionnalités : création, modification, radiation' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Test] Exécution des tests des fonctionnalités : création, modification, radiation' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – RCS-CM pour la modification et la radiation', NULLIF('Mettre en œuvre l’interopérabilité entre la plateforme Orinasa et le système RCS-CM afin de permettre la transmission et la synchronisation des données dans le cadre des démarches de modification et de radiation d’entreprise. Cette tâche inclut : > Analyse des données à échanger entre les deux systèmes > Développement des API dans la partie SERAPI permet d''echanger les données entre Orinasa et la base de données RCS-CM  > Mise en place des règles de validation, d’envoi et de réception > Tests d’intégration pour garantir la fiabilité des échanges > Documentation de la solution mise en œuvre',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'Analyse effectuée / Développement des API en cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – RCS-CM pour la modification et la radiation' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – RCS-CM pour la modification et la radiation' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – SRNE pour la modification et la radiation', NULLIF('Analyse de l''existant et des besoins liés aux procedures de modification côté INSTAT. Développement de l’interopérabilité entre les plateformes Orinasa et SRNE afin de permettre la transmission automatique des informations relatives aux démarches de modification et de radiation d’entreprise, la validation des dossiers par le SRNE, ainsi que l’attribution ou la mise à jour du numéro statistique dans le système SRNE après traitement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'La passation avec l''équipe IT de l''INSTAT a été effectuée.'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – SRNE pour la modification et la radiation' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Interopérabilité] Implémentation de l’interopérabilité Orinasa – SRNE pour la modification et la radiation' AND u.email = 'instat@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Orinasa Itération 4] Mise en place des fonctionnalités de modification et radiation des entreprises', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Orinasa Itération 4] Mise en place des fonctionnalités de modification et radiation des entreprises' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Orinasa Itération 4] Mise en place des fonctionnalités de modification et radiation des entreprises' AND u.email = 'etech@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Mise à jour du site web d''information', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'OK - à vérifier'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Mise à jour du site web d''information' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Création des API pour l''échange de données vers SERAPI et SRNE', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Création des API pour l''échange de données vers SERAPI et SRNE' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Interopérabilité] Sécurisation des échanges de données entre les plateformes Orinasa, SERAPI et SRNE', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'Orinasa Iteration 4' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Interopérabilité] Sécurisation des échanges de données entre les plateformes Orinasa, SERAPI et SRNE' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Interopérabilité] Sécurisation des échanges de données entre les plateformes Orinasa, SERAPI et SRNE' AND u.email = 'ugd@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Retour incorrect à la phase de recevabilité après modification en phase de décision', NULLIF('Cas nouvelle demande : lorsqu''une modification est effectuée sur une demande déjà en phase de décision, le dossier revient à tort à la phase d’étude de recevabilité  Comportement attendu : La demande doit rester en phase de décision, même après modification, sauf si une règle spécifique impose un retour en arrière.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Retour incorrect à la phase de recevabilité après modification en phase de décision' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Réorganisation de l’ordre d’affichage des documents', NULLIF('Réorganiser l’affichage des documents selon l’ordre dans le modèle fournis.  Actuellement, certaines sections comme « Demande adressée à la DG » apparaissent en bas de page, ce qui crée de la confusion pour les utilisateurs.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Réorganisation de l’ordre d’affichage des documents' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Mettre la date au format JJ/MM/AAAA dans le commentaire de confirmation RDV réunion CTI', NULLIF('Modifier le format de la date affichée dans le commentaire de confirmation du rendez-vous pour la réunion CTI afin qu’elle soit au format 22/07/2025 au lieu de 2025-07-22.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Mettre la date au format JJ/MM/AAAA dans le commentaire de confirmation RDV réunion CTI' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Affichage des agréments/attestations dans l’espace client', NULLIF('Dans l’espace client, afficher la liste des agréments ou attestations liés à son compte avec les fonctionnalités suivantes : > Visualisation des informations clés (Dénomination social, type, numéro, statut, etc) > Consultation du fichier associé (agrément ou attestation) via un bouton ou lien > Ouverture dans une nouvelle fenêtre ou prévisualisation intégrée > Accès restreint uniquement au client concerné',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Affichage des agréments/attestations dans l’espace client' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Affichage de la liste des agréments dans l’espace admin', NULLIF('Mettre en place une interface dans l’espace administrateur permettant : > L’affichage de la liste des agréments (avec informations pertinentes : dénomination social, NIF , type de la demande, etc.)  La possibilité de télécharger : > La version non signée de l’agrément > L’agrément signé',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Affichage de la liste des agréments dans l’espace admin' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Export de la liste des agréments par type de demande (Excel)', NULLIF('Permettre le téléchargement de la liste des agréments sous format Excel, avec un filtrage par type de demande : Nouvelle demande Demande de modification',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Export de la liste des agréments par type de demande (Excel)' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration (UX)] Réorganiser l’ordre d’affichage : formulaire de paiement avant les détails de la demande', NULLIF('Adapter la disposition des formulaires afin que le formulaire de paiement soit affiché en priorité, avant le formulaire contenant les détails de la demande.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration (UX)] Réorganiser l’ordre d’affichage : formulaire de paiement avant les détails de la demande' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Statistique', NULLIF('Mise en place des statistiques :  > Délai de traitement pour chaque changement de statut',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Statistique' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Restrictions d’accès : réunions CTI & demandes éligibles', NULLIF('Accès réservé uniquement aux agents back office',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Restrictions d’accès : réunions CTI & demandes éligibles' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Bug] Modification de mot de passe et la fonctionnalité mot de passe oublié', NULLIF('Mot de passe oublié : Aucun effet après avoir cliqué sur le bouton de réinitialisation du mot de passe.  Modification du mot de passe via le menu Profil : Après l''enregistrement du nouveau mot de passe, l''ancien mot de passe reste actif et n''est pas remplacé.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Bug] Modification de mot de passe et la fonctionnalité mot de passe oublié' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Optimisation] Optimiser le temps de chargement de la page', NULLIF('Analyser et améliorer les performances pour réduire le temps de chargement de la page.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Optimisation] Optimiser le temps de chargement de la page' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Changement des libellés', NULLIF('Mettre produits finis ou services car ce n''est pas forcément une entreprise industrielle de transformation',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Changement des libellés' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Changement des libellés' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Restriction d''accès pour le compte d''un agent pilote', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Restriction d''accès pour le compte d''un agent pilote' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Restriction d''accès pour le compte d''un agent pilote' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Mise à jour du modèle de reçu de paiement', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Mise à jour du modèle de reçu de paiement' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Mise à jour du modèle de reçu de paiement' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Ajout de champs dans le rapport d’activités', NULLIF('Modifier le rapport d’activités pour inclure les nouveaux champs suivants :   Nom de la société Nationalité Promoteur Effectuf du personnel Investissement Chiffre d''affaire',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Ajout de champs dans le rapport d’activités' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Ajout de champs dans le rapport d’activités' AND u.email = 'tsiaro@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Envoie d''un email d''accusé de reception après dépôt d''un rapport d''activité', NULLIF('Envoi automatique d''un e-mail d''accusé de réception au client après l''enregistrement de son rapport d''activité et de son état financier.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Envoie d''un email d''accusé de reception après dépôt d''un rapport d''activité' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Nouvelle fonctionnalité] Envoie d''un email d''accusé de reception après dépôt d''un rapport d''activité' AND u.email = 'tsiaro@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Envoie mail pour réunion CTI : augmentation du taille des pièces jointes', NULLIF('La taille des pièces jointes est limitée à 15 Mo. Solution proposée : enregistrer les pièces jointes sur le serveur et insérer dans l''e-mail un lien de téléchargement permettant aux destinataires d''accéder aux documents.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Moyenne' AND st.name = 'En cours'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Envoie mail pour réunion CTI : augmentation du taille des pièces jointes' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Envoie mail pour réunion CTI : augmentation du taille des pièces jointes' AND u.email = 'tsiaro@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Utilisation de l''adresse e-mail institutionnelle pour les convocations CTI', NULLIF('Configuration des adresses d''expédition des e-mails : Utilisation de l''adresse madazef@edbm.mg pour l''envoi des e-mails de convocation destinés aux membres de la CTI. Utilisation de l''adresse noreply@edbm.mg pour l''ensemble des autres notifications et e-mails automatiques générés par la plateforme.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'MADAZEF' AND pr.name = 'Haute' AND st.name = 'OK'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Utilisation de l''adresse e-mail institutionnelle pour les convocations CTI' AND t.project_id = p.project_id);
INSERT INTO task_assignees (task_id, user_id)
SELECT t.task_id, u.users_id FROM tasks t, users u
WHERE t.title = '[Amélioration] Utilisation de l''adresse e-mail institutionnelle pour les convocations CTI' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Prise en main] Installation locale du projet', NULLIF('Effectuer l’installation du projet en environnement local afin de pouvoir l’exécuter et le tester. Cette tâche comprend : > Clonage du dépôt (Git ou autre) > Configuration de l’environnement (base de données, variables d’environnement, dépendances) > Lancement de l’application et vérification du bon fonctionnement initial Objectif : disposer d’un environnement local opérationnel pour le développement et les tests.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Prise en main] Installation locale du projet' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Prise en main] Prise en main du projet', NULLIF('Analyser la structure et le fonctionnement général du projet pour en comprendre l’architecture et les principaux modules. Cette tâche inclut : > Lecture du code et documentation existante > Compréhension des principaux composants (back-end, front-end, base de données, API, etc.) Objectif : être autonome sur le projet pour contribuer efficacement.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Prise en main] Prise en main du projet' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Test] Test de toutes les fonctionnalités', NULLIF('Tester l’ensemble des fonctionnalités de l’application afin de vérifier leur bon fonctionnement et identifier d’éventuels bugs. Cette tâche inclut : > Suivi d’un scénario utilisateur complet > Test des cas d’usage principaux et secondaires > Notation des anomalies ou incohérences rencontrées Objectif : s''assurer que le système fonctionne conformément aux attentes et préparer le terrain pour les éventuelles corrections ou améliorations.',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Test] Test de toutes les fonctionnalités' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Mise en place des statistiques', NULLIF('Développer et intégrer un module de statistiques',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Mise en place des statistiques' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Amélioration] Sécurisation des tokens', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Amélioration] Sécurisation des tokens' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT '[Nouvelle fonctionnalité] Interopérabilité avec la plateforme Pandora : ajout du champ "numéro de reçu préimprimé" et phase de test.', NULLIF('Dans le cadre de l’amélioration de l’échange de données avec la plateforme Pandora, un nouveau champ intitulé ''numéro de reçu préimprimé'' a été intégré au flux d’interopérabilité. Cette évolution vise à renforcer la traçabilité des paiements et à harmoniser les informations transmises entre les deux systèmes',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '[Nouvelle fonctionnalité] Interopérabilité avec la plateforme Pandora : ajout du champ "numéro de reçu préimprimé" et phase de test.' AND t.project_id = p.project_id);
INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)
SELECT 'Mise à jour des configurations de l''email', NULLIF('',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL
FROM projects p, priorities pr, statuses st
WHERE p.title = 'LS-VISA' AND pr.name = 'Moyenne' AND st.name = 'A faire'
AND (st.project_id = p.project_id OR st.project_id IS NULL)
AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = 'Mise à jour des configurations de l''email' AND t.project_id = p.project_id);

-- 5. Contributeurs projet (responsables presents dans le projet)
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'SRNE' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'E-Work' AND u.email = 'support-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa Iteration 4' AND u.email = 'ugd@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa Iteration 4' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'MADAZEF' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'E-Work' AND u.email = 'marko@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'E-Work' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Autres' AND u.email = 'equipe-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa Iteration 4' AND u.email = 'etech@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa' AND u.email = 'support-technique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'MADAZEF' AND u.email = 'tsiaro@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa Iteration 4' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'MADAZEF' AND u.email = 'veronique@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'Orinasa Iteration 4' AND u.email = 'instat@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);
INSERT INTO project_contributors (project_id, user_id, added_at)
SELECT p.project_id, u.users_id, NOW() FROM projects p, users u
WHERE p.title = 'E-Work' AND u.email = 'equipe-dev@edbm.com'
AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);

-- 6. Verification
SELECT (SELECT COUNT(*) FROM tasks) AS nb_taches, (SELECT COUNT(*) FROM users WHERE email NOT IN ('admin@edbm.com','superadmin@edbm.com')) AS nb_utilisateurs, (SELECT COUNT(*) FROM projects) AS nb_projets;
