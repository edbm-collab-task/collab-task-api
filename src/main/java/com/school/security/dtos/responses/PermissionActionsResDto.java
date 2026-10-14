package com.school.security.dtos.responses;

import com.school.security.enums.PermissionCategoryType;
import java.util.List;

/**
 * Représentation d'une permission (module) accompagnée des actions qui peuvent
 * lui être accordées.
 *
 * <p>Catalogue destiné au frontend, renvoyé par
 * {@code GET /roles/permissions/actions}. Chaque action est décrite par son
 * {@link ActionResDto code binaire, nom et libellé} ; {@code totalActions} est
 * la somme des codes (masque) des actions disponibles sur le module.
 *
 * <pre>
 * {
 *   "id": 12,
 *   "name": "MANAGE_TASKS",
 *   "description": "Créer et gérer les tâches",
 *   "categoryPermission": "PROJETS",
 *   "actions": [
 *     { "codeAction": 1, "name": "INSERT", "label": "Insérer" },
 *     { "codeAction": 2, "name": "SELECT", "label": "Voir" }
 *   ],
 *   "totalActions": 15
 * }
 * </pre>
 *
 * @param id identifiant de la permission
 * @param name nom technique de la permission (valeur de {@code PermissionType})
 * @param description libellé descriptif
 * @param categoryPermission catégorie d'appartenance
 * @param actions actions disponibles sur ce module
 * @param totalActions masque (somme des codes) des actions disponibles
 */
public record PermissionActionsResDto(
        Long id,
        String name,
        String description,
        PermissionCategoryType categoryPermission,
        List<ActionResDto> actions,
        Integer totalActions) {}
