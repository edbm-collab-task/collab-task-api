-- ============================================================
-- Collab_tasks BDD
-- Version: 1.0
-- Date: 2026-07-20
-- ============================================================

-- ============================================================
-- PARTIE 1 : CRÉATION DE LA BASE ET DES EXTENSIONS
-- ============================================================

-- Créer la base (à exécuter en tant que super-utilisateur)
-- CREATE DATABASE collab_tasks;
-- \c collab_tasks;

-- Extension pour les UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- PARTIE 2 : TABLES DE RÉFÉRENCE
-- ============================================================

-- 2.1 TABLE ROLE (rôles globaux + rôles de projet dynamiques)
CREATE TABLE role (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_default  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2.2 TABLE STATUT
CREATE TABLE statut (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label  VARCHAR(100) NOT NULL,
    "order" INT NOT NULL DEFAULT 0
);

-- ============================================================
-- PARTIE 3 : TABLES PRINCIPALES
-- ============================================================

-- 3.1 TABLE UTILISATEUR
CREATE TABLE utilisateur (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    role_id       UUID NOT NULL REFERENCES role(id),
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3.2 TABLE PROJET
CREATE TABLE projet (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    UUID NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3.3 TABLE TÂCHE
CREATE TABLE tache (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    projet_id   UUID NOT NULL REFERENCES projet(id) ON DELETE CASCADE,
    statut_id   UUID REFERENCES statut(id) ON DELETE SET NULL,
    priority    VARCHAR(50) DEFAULT 'medium',
    due_date    TIMESTAMP WITH TIME ZONE,
    created_by  UUID NOT NULL REFERENCES utilisateur(id),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3.4 TABLE PIÈCE_JOINTE
CREATE TABLE piece_jointe (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename    VARCHAR(255) NOT NULL,
    url         TEXT NOT NULL,
    tache_id    UUID NOT NULL REFERENCES tache(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES utilisateur(id),
    mime_type   VARCHAR(100),
    size        INT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3.5 TABLE AFFECTATION (avec rôle spécifique au projet)
CREATE TABLE affectation (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tache_id     UUID NOT NULL REFERENCES tache(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    role_id      UUID REFERENCES role(id) ON DELETE SET NULL,
    assigned_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by  UUID NOT NULL REFERENCES utilisateur(id),

    UNIQUE(tache_id, user_id)
);

-- 3.6 TABLE PROJET_ROLE (rôles disponibles dans un projet)
CREATE TABLE projet_role (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id  UUID NOT NULL REFERENCES projet(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    UNIQUE(projet_id, role_id)
);

-- ============================================================
-- PARTIE 4 : INDEX POUR LES PERFORMANCES
-- ============================================================

CREATE INDEX idx_projet_owner ON projet(owner_id);
CREATE INDEX idx_tache_projet ON tache(projet_id);
CREATE INDEX idx_tache_statut ON tache(statut_id);
CREATE INDEX idx_tache_created_by ON tache(created_by);
CREATE INDEX idx_piece_jointe_tache ON piece_jointe(tache_id);
CREATE INDEX idx_affectation_tache ON affectation(tache_id);
CREATE INDEX idx_affectation_user ON affectation(user_id);
CREATE INDEX idx_affectation_role ON affectation(role_id);
CREATE INDEX idx_utilisateur_role ON utilisateur(role_id);
CREATE INDEX idx_projet_role_projet ON projet_role(projet_id);
CREATE INDEX idx_projet_role_role ON projet_role(role_id);

-- ============================================================
-- PARTIE 5 : FONCTIONS ET TRIGGERS
-- ============================================================

-- 5.1 Fonction pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5.2 Triggers updated_at
CREATE TRIGGER trigger_projet_updated_at
    BEFORE UPDATE ON projet
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trigger_tache_updated_at
    BEFORE UPDATE ON tache
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 5.3 Fonction pour empêcher la suppression des rôles par défaut
CREATE OR REPLACE FUNCTION prevent_default_role_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.is_default = true THEN
        RAISE EXCEPTION 'Les rôles par défaut (super_admin, admin, user) ne peuvent pas être supprimés.';
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_prevent_default_role_delete
    BEFORE DELETE ON role
    FOR EACH ROW EXECUTE FUNCTION prevent_default_role_delete();

-- 5.4 Fonction pour vérifier qu'un rôle de projet est bien associé au projet
CREATE OR REPLACE FUNCTION check_projet_role_validity()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM projet_role pr
            JOIN tache t ON t.projet_id = pr.projet_id
            WHERE t.id = NEW.tache_id AND pr.role_id = NEW.role_id
        ) AND EXISTS (
            SELECT 1 FROM role WHERE id = NEW.role_id AND is_default = false
        ) THEN
            RAISE EXCEPTION 'Ce rôle n''est pas disponible pour ce projet.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_projet_role
    BEFORE INSERT OR UPDATE ON affectation
    FOR EACH ROW EXECUTE FUNCTION check_projet_role_validity();

-- ============================================================
-- PARTIE 6 : DONNÉES DE BASE
-- ============================================================

-- 6.1 Rôles par défaut (protégés)
INSERT INTO role (name, description, is_default) VALUES
    ('super_admin', 'Super administrateur - accès total au système', true),
    ('admin', 'Administrateur - gestion des utilisateurs et projets', true),
    ('user', 'Utilisateur standard - accès limité', true);

-- 6.2 Statuts par défaut
INSERT INTO statut (label, "order") VALUES
    ('À faire', 1),
    ('En cours', 2),
    ('En révision', 3),
    ('Terminé', 4),
    ('Annulé', 5);

-- ============================================================
-- PARTIE 7 : VUES UTILES
-- ============================================================

-- 7.1 Vue : utilisateurs avec leur rôle global
CREATE OR REPLACE VIEW v_utilisateurs AS
SELECT 
    u.id, u.name, u.email, u.is_active, u.created_at,
    r.name AS role_global
FROM utilisateur u
JOIN role r ON u.role_id = r.id;

-- 7.2 Vue : projets avec propriétaire
CREATE OR REPLACE VIEW v_projets AS
SELECT 
    p.id, p.title, p.description, p.created_at, p.updated_at,
    u.name AS proprietaire,
    u.email AS proprietaire_email
FROM projet p
JOIN utilisateur u ON p.owner_id = u.id;

-- 7.3 Vue : tâches avec détails
CREATE OR REPLACE VIEW v_taches AS
SELECT 
    t.id, t.title, t.description, t.priority, t.due_date,
    t.created_at, t.updated_at,
    p.title AS projet,
    s.label AS statut,
    u.name AS cree_par
FROM tache t
JOIN projet p ON t.projet_id = p.id
LEFT JOIN statut s ON t.statut_id = s.id
JOIN utilisateur u ON t.created_by = u.id;

-- 7.4 Vue : affectations complètes
CREATE OR REPLACE VIEW v_affectations AS
SELECT 
    a.id,
    t.title AS tache,
    p.title AS projet,
    u.name AS assigne_a,
    assigneur.name AS assigne_par,
    COALESCE(r.name, 'Membre') AS role_projet,
    a.assigned_at
FROM affectation a
JOIN tache t ON a.tache_id = t.id
JOIN projet p ON t.projet_id = p.id
JOIN utilisateur u ON a.user_id = u.id
JOIN utilisateur assigneur ON a.assigned_by = assigneur.id
LEFT JOIN role r ON a.role_id = r.id;

-- 7.5 Vue : rôles disponibles par projet
CREATE OR REPLACE VIEW v_projet_roles AS
SELECT 
    p.title AS projet,
    r.name AS role,
    r.description,
    r.is_default
FROM projet_role pr
JOIN projet p ON pr.projet_id = p.id
JOIN role r ON pr.role_id = r.id;

-- ============================================================
-- PARTIE 8 : SÉCURITÉ (ROW LEVEL SECURITY)
-- ============================================================

-- 8.1 Activer RLS sur les tables sensibles
ALTER TABLE projet ENABLE ROW LEVEL SECURITY;
ALTER TABLE tache ENABLE ROW LEVEL SECURITY;
ALTER TABLE affectation ENABLE ROW LEVEL SECURITY;
ALTER TABLE piece_jointe ENABLE ROW LEVEL SECURITY;
ALTER TABLE projet_role ENABLE ROW LEVEL SECURITY;

-- 8.2 Fonction utilitaire pour récupérer le rôle global de l'utilisateur courant
CREATE OR REPLACE FUNCTION get_current_user_role()
RETURNS TEXT AS $$
DECLARE
    user_role TEXT;
BEGIN
    SELECT r.name INTO user_role
    FROM utilisateur u
    JOIN role r ON u.role_id = r.id
    WHERE u.id = current_setting('app.current_user_id')::UUID;
    RETURN user_role;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8.3 Fonction utilitaire pour vérifier si l'utilisateur est membre d'un projet
CREATE OR REPLACE FUNCTION is_project_member(p_project_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM projet p
        WHERE p.id = p_project_id AND p.owner_id = current_setting('app.current_user_id')::UUID
    ) OR EXISTS (
        SELECT 1 FROM affectation a
        JOIN tache t ON a.tache_id = t.id
        WHERE t.projet_id = p_project_id
        AND a.user_id = current_setting('app.current_user_id')::UUID
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8.4 Politique PROJET
-- Super admin : tout voir
-- Admin : tout voir
-- User : voir ses propres projets + projets où il est affecté
CREATE POLICY projet_select ON projet
    FOR SELECT
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR owner_id = current_setting('app.current_user_id')::UUID
        OR EXISTS (
            SELECT 1 FROM affectation a
            JOIN tache t ON a.tache_id = t.id
            WHERE t.projet_id = projet.id
            AND a.user_id = current_setting('app.current_user_id')::UUID
        )
    );

CREATE POLICY projet_insert ON projet
    FOR INSERT
    WITH CHECK (
        get_current_user_role() IN ('super_admin', 'admin', 'user')
    );

CREATE POLICY projet_update ON projet
    FOR UPDATE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR owner_id = current_setting('app.current_user_id')::UUID
    );

CREATE POLICY projet_delete ON projet
    FOR DELETE
    USING (
        get_current_user_role() = 'super_admin'
        OR owner_id = current_setting('app.current_user_id')::UUID
    );

-- 8.5 Politique TÂCHE
CREATE POLICY tache_select ON tache
    FOR SELECT
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR created_by = current_setting('app.current_user_id')::UUID
        OR is_project_member(projet_id)
    );

CREATE POLICY tache_insert ON tache
    FOR INSERT
    WITH CHECK (
        get_current_user_role() IN ('super_admin', 'admin')
        OR is_project_member(projet_id)
    );

CREATE POLICY tache_update ON tache
    FOR UPDATE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR created_by = current_setting('app.current_user_id')::UUID
        OR is_project_member(projet_id)
    );

CREATE POLICY tache_delete ON tache
    FOR DELETE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR created_by = current_setting('app.current_user_id')::UUID
    );

-- 8.6 Politique AFFECTATION
CREATE POLICY affectation_select ON affectation
    FOR SELECT
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR user_id = current_setting('app.current_user_id')::UUID
        OR assigned_by = current_setting('app.current_user_id')::UUID
        OR EXISTS (
            SELECT 1 FROM tache t
            WHERE t.id = affectation.tache_id
            AND is_project_member(t.projet_id)
        )
    );

CREATE POLICY affectation_insert ON affectation
    FOR INSERT
    WITH CHECK (
        get_current_user_role() IN ('super_admin', 'admin')
        OR EXISTS (
            SELECT 1 FROM tache t
            WHERE t.id = affectation.tache_id
            AND (t.created_by = current_setting('app.current_user_id')::UUID
                 OR is_project_member(t.projet_id))
        )
    );

CREATE POLICY affectation_update ON affectation
    FOR UPDATE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR assigned_by = current_setting('app.current_user_id')::UUID
    );

CREATE POLICY affectation_delete ON affectation
    FOR DELETE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR assigned_by = current_setting('app.current_user_id')::UUID
    );

-- 8.7 Politique PIÈCE_JOINTE
CREATE POLICY piece_jointe_select ON piece_jointe
    FOR SELECT
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR uploaded_by = current_setting('app.current_user_id')::UUID
        OR EXISTS (
            SELECT 1 FROM tache t
            WHERE t.id = piece_jointe.tache_id
            AND is_project_member(t.projet_id)
        )
    );

CREATE POLICY piece_jointe_insert ON piece_jointe
    FOR INSERT
    WITH CHECK (
        get_current_user_role() IN ('super_admin', 'admin')
        OR EXISTS (
            SELECT 1 FROM tache t
            WHERE t.id = piece_jointe.tache_id
            AND is_project_member(t.projet_id)
        )
    );

CREATE POLICY piece_jointe_delete ON piece_jointe
    FOR DELETE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR uploaded_by = current_setting('app.current_user_id')::UUID
    );

-- 8.8 Politique PROJET_ROLE
CREATE POLICY projet_role_select ON projet_role
    FOR SELECT
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR is_project_member(projet_id)
    );

CREATE POLICY projet_role_insert ON projet_role
    FOR INSERT
    WITH CHECK (
        get_current_user_role() IN ('super_admin', 'admin')
        OR EXISTS (
            SELECT 1 FROM projet p
            WHERE p.id = projet_role.projet_id
            AND p.owner_id = current_setting('app.current_user_id')::UUID
        )
    );

CREATE POLICY projet_role_delete ON projet_role
    FOR DELETE
    USING (
        get_current_user_role() IN ('super_admin', 'admin')
        OR EXISTS (
            SELECT 1 FROM projet p
            WHERE p.id = projet_role.projet_id
            AND p.owner_id = current_setting('app.current_user_id')::UUID
        )
    );

-- ============================================================
-- PARTIE 9 : FONCTIONS STOCKÉES POUR LA GESTION DES RÔLES
-- ============================================================

-- 9.1 Créer un rôle dynamique pour un projet (en une seule commande)
CREATE OR REPLACE FUNCTION create_projet_role(
    p_projet_id UUID,
    p_role_name VARCHAR(100),
    p_description TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    new_role_id UUID;
BEGIN
    -- Créer le rôle
    INSERT INTO role (name, description, is_default)
    VALUES (p_role_name, p_description, false)
    RETURNING id INTO new_role_id;

    -- L'associer au projet
    INSERT INTO projet_role (projet_id, role_id)
    VALUES (p_projet_id, new_role_id);

    RETURN new_role_id;
END;
$$ LANGUAGE plpgsql;

-- 9.2 Affecter un utilisateur à une tâche avec un rôle de projet
CREATE OR REPLACE FUNCTION assign_user_to_tache(
    p_tache_id UUID,
    p_user_id UUID,
    p_role_id UUID,
    p_assigned_by UUID
)
RETURNS UUID AS $$
DECLARE
    new_affectation_id UUID;
BEGIN
    INSERT INTO affectation (tache_id, user_id, role_id, assigned_by)
    VALUES (p_tache_id, p_user_id, p_role_id, p_assigned_by)
    RETURNING id INTO new_affectation_id;

    RETURN new_affectation_id;
END;
$$ LANGUAGE plpgsql;

-- 9.3 Récupérer les rôles disponibles pour un projet
CREATE OR REPLACE FUNCTION get_projet_roles(p_projet_id UUID)
RETURNS TABLE(role_id UUID, role_name VARCHAR, role_description TEXT, is_default_role BOOLEAN) AS $$
BEGIN
    RETURN QUERY
    SELECT r.id, r.name, r.description, r.is_default
    FROM projet_role pr
    JOIN role r ON pr.role_id = r.id
    WHERE pr.projet_id = p_projet_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- PARTIE 10 : UTILISATION DANS L'APPLICATION
-- ============================================================

/*

-- Avant chaque requête dans l'application, définir l'ID utilisateur :
SET app.current_user_id = 'uuid-de-l-utilisateur-connecte';

-- Exemple : Créer un super admin
INSERT INTO utilisateur (name, email, role_id, password_hash)
VALUES (
    'Admin Principal',
    'admin@gestion-projets.com',
    (SELECT id FROM role WHERE name = 'super_admin'),
    '$2b$10$...'  -- hash bcrypt
);

-- Exemple : Créer un projet
INSERT INTO projet (title, description, owner_id)
VALUES (
    'Mon Premier Projet',
    'Description du projet',
    'uuid-du-proprietaire'
);

-- Exemple : Créer un rôle dynamique pour un projet
SELECT create_projet_role(
    'uuid-du-projet',
    'chef_equipe',
    'Chef d'équipe technique'
);

-- Exemple : Affecter un utilisateur avec ce rôle
SELECT assign_user_to_tache(
    'uuid-de-la-tache',
    'uuid-de-l-utilisateur',
    (SELECT id FROM role WHERE name = 'chef_equipe'),
    'uuid-de-qui-assigne'
);

-- Exemple : Requête sécurisée (RLS filtre automatiquement)
SELECT * FROM v_projets;
SELECT * FROM v_taches;
SELECT * FROM v_affectations;

*/

-- ============================================================
-- FIN DU SCRIPT
-- ============================================================
