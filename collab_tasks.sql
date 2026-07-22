-- ============================================================
-- SCRIPT DE CREATION DE LA BASE DE DONNEES - Collab Tasks
-- SGBD : PostgreSQL
-- Description : Base de donnees pour la gestion de projets, taches,
--               utilisateurs, fichiers et suivi d'activites.
-- ============================================================

-- Assure la disponibilite de gen_random_uuid() meme sur PostgreSQL < 13
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- TABLE : Address (Adresses)
-- Description : Stocke les adresses physiques des utilisateurs.
-- Relation : N-N avec Users via la table has_address.
-- ============================================================
CREATE TABLE Address(
   id_address SERIAL,                   -- Identifiant auto-incremente de l'adresse (PK)
   lot VARCHAR(256),                    -- Numero de lot / rue / adresse detaillee
   city_town VARCHAR(256),              -- Ville ou commune
   country VARCHAR(50),                 -- Pays
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de creation de l'enregistrement
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de derniere modification
   PRIMARY KEY(id_address)
);

-- ============================================================
-- TABLE : Role (Roles utilisateurs)
-- Description : Definit les roles possibles des utilisateurs
--               (ex: Admin, Manager, Collaborateur).
-- ============================================================
CREATE TABLE Role(
   id_role SERIAL,                      -- Identifiant auto-incremente du role (PK)
   name VARCHAR(256) NOT NULL,          -- Nom du role (ex: 'Administrateur')
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le role est actif
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_role)
);

-- ============================================================
-- TABLE : Status (Statuts de projet)
-- Description : Liste des statuts possibles pour un projet
--               (ex: En cours, Termine, En attente).
-- ============================================================
CREATE TABLE Status(
   id_status SERIAL,                    -- Identifiant auto-incremente du statut (PK)
   name VARCHAR(256) NOT NULL,          -- Libelle du statut
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le statut est actif
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_status)
);

-- ============================================================
-- TABLE : File_type (Types de fichiers)
-- Description : Categorisation des fichiers uploades
--               (ex: Image, Document, PDF).
-- ============================================================
CREATE TABLE File_type(
   id_file_type SERIAL,                 -- Identifiant auto-incremente du type (PK)
   type VARCHAR(50) NOT NULL,           -- Nom du type de fichier
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le type est actif
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_file_type)
);

-- ============================================================
-- TABLE : Priority (Priorites de tache)
-- Description : Niveaux de priorite pour les taches
--               (ex: Basse, Moyenne, Haute, Urgente).
-- ============================================================
CREATE TABLE Priority(
   id_priority SERIAL,                  -- Identifiant auto-incremente de la priorite (PK)
   name VARCHAR(256) NOT NULL,          -- Nom de la priorite
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_priority)
);

-- ============================================================
-- TABLE : Users (Utilisateurs)
-- Description : Table centrale des utilisateurs de l'application.
-- Relation : 1-N avec Role (un role peut avoir plusieurs users).
-- ============================================================
CREATE TABLE Users(
   id_user SERIAL,                      -- Identifiant auto-incremente de l'utilisateur (PK)
   first_name VARCHAR(256),             -- Prenom
   last_name VARCHAR(256),              -- Nom de famille
   email VARCHAR(256) NOT NULL,         -- Adresse email (obligatoire, unique)
   password_hash VARCHAR(256),          -- Mot de passe hashe (jamais en clair!)
   gender VARCHAR(50) NOT NULL,         -- Genre (obligatoire)
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de creation du compte
   status BOOLEAN DEFAULT TRUE,         -- Statut general de l'utilisateur (actif/inactif)
   phone_number VARCHAR(20),          -- Numero de telephone (renomme pour plus de clarte)
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le compte est actif
   id_role INT NOT NULL,                -- Reference vers le role (FK obligatoire)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_user),
   UNIQUE(email),                       -- Contrainte d'unicite sur l'email
   FOREIGN KEY(id_role) REFERENCES Role(id_role)
);

-- Index sur l'email pour accelerer les recherches par email
CREATE INDEX idx_users_email ON Users(email);

-- ============================================================
-- TABLE : File (Fichiers)
-- Description : Fichiers uploades par les utilisateurs.
-- Relation : 1-N avec Users, 1-N avec File_type.
-- ============================================================
CREATE TABLE File(
   id_file UUID DEFAULT gen_random_uuid(),  -- Identifiant unique UUID du fichier (PK)
   url VARCHAR(256) NOT NULL,           -- URL d'acces au fichier stocke
   size_bytes BIGINT,                   -- Taille du fichier en octets (BIGINT pour les gros fichiers)
   original_name VARCHAR(256) NOT NULL, -- Nom original du fichier uploade
   stored_name VARCHAR(256) NOT NULL,   -- Nom interne apres stockage
   mime_type VARCHAR(100),              -- Type MIME du fichier (ex: image/png)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de creation/upload
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le fichier est actif
   id_user INT NOT NULL,                -- Utilisateur ayant uploade le fichier (FK)
   id_file_type INT NOT NULL,           -- Type de fichier (FK)
   PRIMARY KEY(id_file),
   FOREIGN KEY(id_user) REFERENCES Users(id_user) ON DELETE CASCADE,
   FOREIGN KEY(id_file_type) REFERENCES File_type(id_file_type)
);

-- Index sur l'utilisateur pour lister rapidement les fichiers d'un user
CREATE INDEX idx_file_user ON File(id_user);

-- ============================================================
-- TABLE : Users_history (Historique des actions utilisateurs)
-- Description : Journal des actions realisees par un utilisateur.
-- Relation : N-1 avec Users.
-- ============================================================
CREATE TABLE Users_history(
   id_history SERIAL,                   -- Identifiant auto-incremente de l'entree (PK)
   action VARCHAR(255) NOT NULL,        -- Type d'action realisee
   description TEXT,                    -- Description detaillee de l'action (TEXT pour longueur illimitee)
   ip_address INET,                     -- Adresse IP de l'utilisateur lors de l'action
   date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date et heure de l'action
   id_user INT NOT NULL,                -- Utilisateur concerne (FK)
   PRIMARY KEY(id_history),
   FOREIGN KEY(id_user) REFERENCES Users(id_user) ON DELETE CASCADE
);

-- Index sur l'utilisateur pour l'historique
CREATE INDEX idx_users_history_user ON Users_history(id_user);
CREATE INDEX idx_users_history_date ON Users_history(date_action);

-- ============================================================
-- TABLE : Project (Projets)
-- Description : Projets crees et geres par les utilisateurs.
-- Relation : N-1 avec Users (createur), N-1 avec File (optionnel).
-- ============================================================
CREATE TABLE Project(
   id_project VARCHAR(50),              -- Identifiant unique du projet (PK)
   title VARCHAR(256) NOT NULL,         -- Titre du projet
   description TEXT,                    -- Description detaillee (TEXT pour longueur illimitee)
   start_date DATE,                     -- Date de debut prevue
   end_date DATE,                       -- Date de fin prevue
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si le projet est actif
   id_file UUID,                        -- Fichier associe au projet (FK, optionnel)
   id_user INT NOT NULL,                -- Createur/proprietaire du projet (FK)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_project),
   FOREIGN KEY(id_file) REFERENCES File(id_file) ON DELETE SET NULL,
   FOREIGN KEY(id_user) REFERENCES Users(id_user),
   -- Contrainte : la date de fin doit etre posterieure a la date de debut
   CONSTRAINT chk_project_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Index sur le createur pour lister les projets d'un utilisateur
CREATE INDEX idx_project_user ON Project(id_user);

-- ============================================================
-- TABLE : Task (Taches)
-- Description : Taches rattachees a un projet avec priorite et echeance.
-- Relation : N-1 avec Project, N-1 avec Priority, N-1 avec File (optionnel).
-- ============================================================
CREATE TABLE Task(
   id_task SERIAL,                      -- Identifiant auto-incremente de la tache (PK)
   title VARCHAR(256) NOT NULL,         -- Titre de la tache
   description TEXT,                    -- Description detaillee
   date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de creation
   start_date DATE,                     -- Date de debut de la tache
   deadline DATE,                       -- Date d'echeance (deadline)
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si la tache est active
   id_file UUID,                        -- Fichier joint a la tache (FK, optionnel)
   id_project VARCHAR(50) NOT NULL,     -- Projet parent (FK, obligatoire)
   id_priority INT NOT NULL,            -- Niveau de priorite (FK, obligatoire)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_task),
   FOREIGN KEY(id_file) REFERENCES File(id_file) ON DELETE SET NULL,
   FOREIGN KEY(id_project) REFERENCES Project(id_project) ON DELETE CASCADE,
   FOREIGN KEY(id_priority) REFERENCES Priority(id_priority),
   -- Contrainte : la deadline doit etre posterieure a la date de debut
   CONSTRAINT chk_task_dates CHECK (deadline IS NULL OR deadline >= start_date)
);

-- Index sur le projet pour lister rapidement les taches
CREATE INDEX idx_task_project ON Task(id_project);
CREATE INDEX idx_task_priority ON Task(id_priority);

-- ============================================================
-- TABLE : Comment (Commentaires)
-- Description : Commentaires laisses par les utilisateurs sur les projets/taches.
-- Relation : N-1 avec Project, N-1 avec Users, N-1 avec Task.
-- ============================================================
CREATE TABLE Comment(
   id_comment SERIAL,                   -- Identifiant auto-incremente du commentaire (PK)
   content TEXT NOT NULL,               -- Contenu du commentaire (renomme de 'description' a 'content')
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de creation
   id_project VARCHAR(50) NOT NULL,     -- Projet concerne (FK)
   id_user INT NOT NULL,                -- Auteur du commentaire (FK)
   id_task INT NOT NULL,                -- Tache concernee (FK)
   PRIMARY KEY(id_comment),
   FOREIGN KEY(id_project) REFERENCES Project(id_project) ON DELETE CASCADE,
   FOREIGN KEY(id_user) REFERENCES Users(id_user),
   FOREIGN KEY(id_task) REFERENCES Task(id_task) ON DELETE CASCADE
);

-- Index pour accelerer les recherches de commentaires
CREATE INDEX idx_comment_project ON Comment(id_project);
CREATE INDEX idx_comment_task ON Comment(id_task);
CREATE INDEX idx_comment_user ON Comment(id_user);

-- ============================================================
-- TABLE : Activity_history (Historique des activites de projet)
-- Description : Journal des changements de statut et actions sur un projet.
-- Relation : N-1 avec Project.
-- ============================================================
CREATE TABLE Activity_history(
   id_history_activity SERIAL,          -- Identifiant auto-incremente de l'entree (PK)
   action VARCHAR(256) NOT NULL,        -- Action realisee (obligatoire)
   description TEXT,                    -- Description detaillee
   last_status INT,                     -- Statut precedent (FK vers Status)
   recent_status INT,                   -- Nouveau statut (FK vers Status)
   user_members INT,                    -- Membres impliques dans l'action
   user_change_status INT,              -- Utilisateur ayant change le statut
   user_owner INT,                      -- Proprietaire du projet concerne
   date_action TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date et heure de l'action
   id_project VARCHAR(50) NOT NULL,     -- Projet concerne (FK, obligatoire)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_history_activity),
   FOREIGN KEY(id_project) REFERENCES Project(id_project) ON DELETE CASCADE,
   -- Ajout des FK manquantes vers Status
   FOREIGN KEY(last_status) REFERENCES Status(id_status) ON DELETE SET NULL,
   FOREIGN KEY(recent_status) REFERENCES Status(id_status) ON DELETE SET NULL,
   FOREIGN KEY(user_change_status) REFERENCES Users(id_user) ON DELETE SET NULL,
   FOREIGN KEY(user_owner) REFERENCES Users(id_user) ON DELETE SET NULL
);

-- Index pour accelerer les recherches d'historique
CREATE INDEX idx_activity_project ON Activity_history(id_project);
CREATE INDEX idx_activity_date ON Activity_history(date_action);

-- ============================================================
-- TABLE : has_address (Association Utilisateurs-Adresses)
-- Description : Table de liaison N-N entre Users et Address.
-- Un utilisateur peut avoir plusieurs adresses, une adresse peut
-- appartenir a plusieurs utilisateurs.
-- ============================================================
CREATE TABLE has_address(
   id_user INT,                         -- Reference vers l'utilisateur (FK, partie de PK)
   id_address INT,                      -- Reference vers l'adresse (FK, partie de PK)
   id_users_address SERIAL,             -- Identifiant unique auto-incremente de la liaison
   address_type VARCHAR(50) DEFAULT 'principal',  -- Type d'adresse (principal, secondaire, travail...)
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY(id_user, id_address),
   UNIQUE(id_users_address),            -- Contrainte d'unicite sur l'ID de liaison
   FOREIGN KEY(id_user) REFERENCES Users(id_user) ON DELETE CASCADE,
   FOREIGN KEY(id_address) REFERENCES Address(id_address) ON DELETE CASCADE
);

-- ============================================================
-- TABLE : contributors (Contributeurs aux projets)
-- Description : Table de liaison N-N entre Users et Project.
-- Associe les utilisateurs aux projets auxquels ils contribuent.
-- ============================================================
CREATE TABLE contributors(
   id_user INT,                         -- Reference vers l'utilisateur (FK, partie de PK)
   id_project VARCHAR(50),              -- Reference vers le projet (FK, partie de PK)
   id_contributor SERIAL,               -- Identifiant auto-incremente du contributeur
   role_in_project VARCHAR(100),        -- Role specifique dans le projet
   joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date d'adhesion au projet
   is_active BOOLEAN DEFAULT TRUE,      -- Indique si la contribution est active
   PRIMARY KEY(id_user, id_project),
   UNIQUE(id_contributor),              -- Chaque contributeur a un ID unique
   FOREIGN KEY(id_user) REFERENCES Users(id_user) ON DELETE CASCADE,
   FOREIGN KEY(id_project) REFERENCES Project(id_project) ON DELETE CASCADE
);

-- Index pour lister les contributeurs d'un projet
CREATE INDEX idx_contributors_project ON contributors(id_project);

-- ============================================================
-- TABLE : receives (Assignation des taches)
-- Description : Table de liaison N-N entre Users et Task.
-- Definit quels utilisateurs sont assignes a quelles taches.
-- ============================================================
CREATE TABLE receives(
   id_user INT,                         -- Reference vers l'utilisateur assigne (FK, partie de PK)
   id_task INT,                         -- Reference vers la tache (FK, partie de PK)
   id_users_task SERIAL,                -- Identifiant auto-incremente de l'assignation
   assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date d'assignation
   assigned_by INT,                     -- Utilisateur ayant fait l'assignation
   PRIMARY KEY(id_user, id_task),
   UNIQUE(id_users_task),               -- Contrainte d'unicite sur l'ID d'assignation
   FOREIGN KEY(id_user) REFERENCES Users(id_user) ON DELETE CASCADE,
   FOREIGN KEY(id_task) REFERENCES Task(id_task) ON DELETE CASCADE,
   FOREIGN KEY(assigned_by) REFERENCES Users(id_user) ON DELETE SET NULL
);

-- Index pour lister les taches assignees a un utilisateur
CREATE INDEX idx_receives_user ON receives(id_user);
CREATE INDEX idx_receives_task ON receives(id_task);

-- ============================================================
-- TABLE : has_status (Suivi des statuts de projet)
-- Description : Table de liaison N-N entre Project et Status.
-- Historise les changements de statut d'un projet dans le temps.
-- ============================================================
CREATE TABLE has_status(
   id_project VARCHAR(50),              -- Reference vers le projet (FK, partie de PK)
   id_status INT,                       -- Reference vers le statut (FK, partie de PK)
   id_project_status SERIAL,            -- Identifiant auto-incremente de l'entree statut
   date_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Date de mise a jour du statut
   updated_by INT,                      -- Utilisateur ayant fait la mise a jour
   notes TEXT,                          -- Notes explicatives du changement
   PRIMARY KEY(id_project, id_status),
   UNIQUE(id_project_status),           -- Contrainte d'unicite sur l'ID d'entree
   FOREIGN KEY(id_project) REFERENCES Project(id_project) ON DELETE CASCADE,
   FOREIGN KEY(id_status) REFERENCES Status(id_status),
   FOREIGN KEY(updated_by) REFERENCES Users(id_user) ON DELETE SET NULL
);

-- Index pour l'historique des statuts d'un projet
CREATE INDEX idx_has_status_project ON has_status(id_project);
CREATE INDEX idx_has_status_date ON has_status(date_update);

-- ============================================================
-- FONCTION : Mise a jour automatique de updated_at
-- Description : Trigger pour mettre a jour automatiquement
-- la colonne updated_at a chaque modification.
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$ language 'plpgsql';

-- Application du trigger sur toutes les tables avec updated_at
CREATE TRIGGER trg_address_updated_at BEFORE UPDATE ON Address
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_role_updated_at BEFORE UPDATE ON Role
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_status_updated_at BEFORE UPDATE ON Status
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_file_type_updated_at BEFORE UPDATE ON File_type
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_priority_updated_at BEFORE UPDATE ON Priority
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON Users
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_project_updated_at BEFORE UPDATE ON Project
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_task_updated_at BEFORE UPDATE ON Task
   FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- DONNEES INITIALES (SEED)
-- Description : Insertion des donnees de base necessaires.
-- ============================================================

-- Roles par defaut
INSERT INTO Role (name, is_active) VALUES
   ('Super Administrateur', TRUE),
   ('Administrateur', TRUE),
   ('Utilisateur', TRUE);

-- Statuts par defaut
INSERT INTO Status (name, is_active) VALUES
   ('En attente', TRUE),
   ('En cours', TRUE),
   ('En revision', TRUE),
   ('Termine', TRUE),
   ('Annule', TRUE);

-- Types de fichiers par defaut
INSERT INTO File_type (type, is_active) VALUES
   ('Image', TRUE),
   ('Document', TRUE),
   ('PDF', TRUE),
   ('Video', TRUE),
   ('Audio', TRUE),
   ('Archive', TRUE);

-- Priorites par defaut
INSERT INTO Priority (name) VALUES
   ('Basse'),
   ('Moyenne'),
   ('Haute'),
   ('Urgente');