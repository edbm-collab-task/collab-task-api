package com.school.security.enums;

/**
 * Catégories d'appartenance des permissions, utilisées pour regrouper les
 * permissions dans l'interface d'administration.
 *
 * <p>La catégorie d'une permission est une donnée de référence : elle est
 * approvisionnée par {@code data.sql} (rubrique « 3. Créer les permissions »)
 * et n'est jamais modifiable via l'API. Seule la valeur affectée à chaque
 * permission ({@code permissions.category_permission}) change.
 */
public enum PermissionCategoryType {
    /** Gestion des comptes : consultation, création, comptes administrateurs. */
    UTILISATEURS,

    /** Cycle de vie des projets et de leurs contributeurs. */
    PROJETS,

    /** Structure de l'organisation : rôles et directions. */
    ORGANISATION,

    /** Consultation des rapports et des statistiques. */
    RAPPORTS,

    /** Permissions ne relevant d'aucune autre catégorie (statuts, etc.). */
    AUTRES
}
