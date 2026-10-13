package com.school.security.enums;

/**
 * Actions qu'un rôle peut se voir accorder sur une permission (module).
 *
 * <p>Une permission ({@link PermissionType}) matérialise l'accès à un module ;
 * une action matérialise ce qu'un rôle peut y faire. Le jeu est volontairement
 * générique et réutilisé pour tous les modules.
 */
public enum ActionType {
    /** Créer / ajouter un nouvel élément. */
    INSERT,

    /** Modifier un élément existant. */
    UPDATE,

    /** Supprimer / archiver un élément. */
    DELETE,

    /** Générer un rapport (document mis en forme, ex. PDF). */
    GENERATE_REPORT,

    /** Exporter / télécharger des données. */
    EXPORT
}
