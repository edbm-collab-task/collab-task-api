-- Migration: réactions Facebook - une seule réaction par personne et par commentaire.
-- Ancienne contrainte : UNIQUE (comment_id, emoji, user_id)
-- Nouvelle contrainte   : UNIQUE (comment_id, user_id)

-- 1. Supprimer toute contrainte unique existante sur comment_id (ex. ancienne (comment_id, emoji, user_id))
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'comment_reactions'::regclass
          AND contype = 'u'
          AND conname <> 'comment_reactions_pkey'
    LOOP
        EXECUTE format('ALTER TABLE comment_reactions DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

-- 2. Dédupliquer : ne garder qu'une réaction par (comment_id, user_id)
--    (conserver la première insertion)
DELETE FROM comment_reactions cr
WHERE cr.reaction_id NOT IN (
    SELECT MIN(cr2.reaction_id)
    FROM comment_reactions cr2
    GROUP BY cr2.comment_id, cr2.user_id
);

-- 3. Ajouter la nouvelle contrainte unique
ALTER TABLE comment_reactions
    ADD CONSTRAINT uq_comment_reactions_comment_user
    UNIQUE (comment_id, user_id);