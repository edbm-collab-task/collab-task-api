package com.school.security.dtos.responses;

import com.school.security.enums.PermissionCategoryType;

/**
 * Représentation d'une permission (module) avec le masque de ses actions
 * disponibles.
 *
 * <p>Catalogue destiné au frontend, renvoyé par
 * {@code GET /roles/permissions/actions}. {@code totalActions} est la somme des
 * codes (masque) des actions disponibles sur le module ; le détail des actions
 * ({@code codeAction}, nom, libellé) n'est plus répété ici mais fourni une seule
 * fois par {@code GET /roles/actions}.
 *
 * <pre>
 * {
 *   "id": 12,
 *   "name": "MANAGE_TASKS",
 *   "description": "Créer et gérer les tâches",
 *   "categoryPermission": "PROJETS",
 *   "totalActions": 15
 * }
 * </pre>
 *
 * @param id identifiant de la permission
 * @param name nom technique de la permission (valeur de {@code PermissionType})
 * @param description libellé descriptif
 * @param categoryPermission catégorie d'appartenance
 * @param totalActions masque (somme des codes) des actions disponibles
 */
public record PermissionActionsResDto(
        Long id,
        String name,
        String description,
        PermissionCategoryType categoryPermission,
        Integer totalActions) {}
