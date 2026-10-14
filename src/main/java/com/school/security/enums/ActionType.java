package com.school.security.enums;

/**
 * Actions qu'un rôle peut se voir accorder sur un module (permission).
 *
 * <p>Chaque action porte un <strong>code binaire</strong> (puissance de 2) qui
 * sert de drapeau : l'ensemble des actions d'un module et l'ensemble des
 * actions accordées à un rôle sont représentés par la <em>somme</em> de ces
 * codes (un masque binaire). Le frontend peut ainsi tester une action par un
 * simple {@code ET} bit à bit : {@code (masqueAccorde & codeAction) != 0}.
 *
 * <pre>
 *   INSERT          = 1
 *   SELECT          = 2
 *   UPDATE          = 4
 *   DELETE          = 8
 *   GENERATE_REPORT = 16
 *   EXPORT          = 32
 * </pre>
 *
 * <p>Le libellé est la forme affichable côté client (« Insérer », « Voir », …).
 */
public enum ActionType {

    /** Créer / ajouter un nouvel élément. */
    INSERT(1, "Insérer"),

    /**
     * Voir / accéder à la page du module. Action de lecture de base : c'est
     * elle qui pilote la visibilité de la page pour un rôle (cochée = page
     * visible, décochée = page masquée).
     */
    SELECT(2, "Voir"),

    /** Modifier un élément existant. */
    UPDATE(4, "Modifier"),

    /** Supprimer / archiver un élément. */
    DELETE(8, "Supprimer"),

    /** Générer un rapport (document mis en forme, ex. PDF). */
    GENERATE_REPORT(16, "Générer un rapport"),

    /** Exporter / télécharger des données. */
    EXPORT(32, "Exporter");

    private final int code;
    private final String label;

    ActionType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** Code binaire (puissance de 2) de l'action. */
    public int getCode() {
        return code;
    }

    /** Libellé affichable de l'action. */
    public String getLabel() {
        return label;
    }

    /** Retrouve une action par son code binaire. */
    public static ActionType fromCode(int code) {
        for (ActionType action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Code d'action inconnu : " + code);
    }
}
