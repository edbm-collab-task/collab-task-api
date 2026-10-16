package com.school.security.enums;

/**
 * Actions qu'un rôle peut se voir accorder sur un module (permission).
 *
 * <p>Le <strong>code binaire</strong> d'une action n'est plus stocké : il est
 * <em>calculé</em> à partir de sa position dans l'énumération. Chaque action
 * porte un {@code bitIndex} (0, 1, 2, ...) et son code vaut {@code 1 << bitIndex}
 * (une puissance de 2). L'ensemble des actions d'un module et l'ensemble des
 * actions accordées à un rôle sont représentés par la somme (OU bit à bit) de
 * ces codes : un masque binaire. Le frontend teste une action par un simple ET
 * bit à bit : {@code (masqueAccorde & codeAction) != 0}.
 *
 * <pre>
 *   INSERT          bitIndex 0 -> 1
 *   SELECT          bitIndex 1 -> 2
 *   UPDATE          bitIndex 2 -> 4
 *   DELETE          bitIndex 3 -> 8
 *   GENERATE_REPORT bitIndex 4 -> 16
 *   EXPORT          bitIndex 5 -> 32
 * </pre>
 *
 * <p><strong>IMPORTANT :</strong> l'ordre de déclaration est la source de
 * vérité. On ne réordonne jamais les constantes et on ajoute toute nouvelle
 * action <em>à la fin</em> (append-only), sinon les masques déjà stockés en
 * base ({@code roles_permissions.actions}, {@code permissions.total_actions})
 * deviendraient incohérents. Un test ({@code ActionTypeTest}) verrouille ces
 * codes pour qu'un réordonnancement casse immédiatement la compilation des
 * tests au lieu de corrompre la base.
 *
 * <p>Le libellé est la forme affichable côté client (« Insérer », « Voir », …).
 */
public enum ActionType {

    /** Créer / ajouter un nouvel élément. */
    INSERT(0, "Insérer"),

    /**
     * Voir / accéder à la page du module. Action de lecture de base : c'est
     * elle qui pilote la visibilité de la page pour un rôle (cochée = page
     * visible, décochée = page masquée).
     */
    SELECT(1, "Voir"),

    /** Modifier un élément existant. */
    UPDATE(2, "Modifier"),

    /** Supprimer / archiver un élément. */
    DELETE(3, "Supprimer"),

    /** Générer un rapport (document mis en forme, ex. PDF). */
    GENERATE_REPORT(4, "Générer un rapport"),

    /** Exporter / télécharger des données. */
    EXPORT(5, "Exporter");

    private final int bitIndex;
    private final String label;

    ActionType(int bitIndex, String label) {
        this.bitIndex = bitIndex;
        this.label = label;
    }

    /** Index 0-based de l'action (position de son bit dans le masque). */
    public int getBitIndex() {
        return bitIndex;
    }

    /** Code binaire (puissance de 2) de l'action : {@code 1 << bitIndex}. */
    public int getCode() {
        return 1 << bitIndex;
    }

    /** Libellé affichable de l'action. */
    public String getLabel() {
        return label;
    }

    /**
     * Calcule le code binaire (masque) associé à une position : {@code 1 << bitIndex},
     * c'est-à-dire {@code 2^bitIndex} (un seul bit à 1).
     *
     * @param bitIndex index 0-based de l'action
     * @return le code de l'action à cette position
     */
    public static int codeFor(int bitIndex) {
        return 1 << bitIndex;
    }

    /**
     * Applique le prérequis « Voir » à un masque d'actions accordées.
     *
     * <p>Une action n'est exploitable que si l'action {@link #SELECT} est
     * elle-même accordée : sans accès en lecture (page masquée), l'utilisateur
     * ne peut ni insérer, ni modifier, ni supprimer, etc. Un masque dépourvu du
     * bit SELECT est donc neutralisé (ramené à {@code 0}).
     *
     * @param mask masque binaire des actions accordées
     * @return le masque inchangé s'il contient SELECT, {@code 0} sinon
     */
    public static int applySelectPrerequisite(int mask) {
        return (mask & SELECT.getCode()) == 0 ? 0 : mask;
    }

    /** Retrouve une action par son code binaire. */
    public static ActionType fromCode(int code) {
        for (ActionType action : values()) {
            if (action.getCode() == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Code d'action inconnu : " + code);
    }
}
