package com.school.security.dtos.responses;

import com.school.security.enums.PermissionCategoryType;
import java.util.List;

/**
 * Représentation d'une permission accompagnée des actions qui peuvent lui être
 * accordées.
 *
 * <p>Catalogue destiné au frontend : chaque module ({@code name}) est exposé
 * avec la liste complète des actions possibles ({@code ActionType}). Le jeu
 * d'actions est générique et identique pour tous les modules ; il sert à
 * construire les cases à cocher d'actions côté client.
 *
 * <p>Forme renvoyée par {@code GET /roles/permissions/actions} :
 *
 * <pre>
 * {
 *   "id": 12,
 *   "name": "MANAGE_TASKS",
 *   "description": "Gestion des t\u00e2ches",
 *   "categoryPermission": "PROJETS",
 *   "actions": ["INSERT", "UPDATE", "DELETE", "GENERATE_REPORT", "EXPORT"]
 * }
 * </pre>
 *
 * @param id identifiant de la permission
 * @param name nom technique de la permission (valeur de {@code PermissionType})
 * @param description libellé descriptif
 * @param categoryPermission catégorie d'appartenance
 * @param actions actions qu'un rôle peut se voir accorder sur ce module
 */
public record PermissionActionsResDto(
        Long id,
        String name,
        String description,
        PermissionCategoryType categoryPermission,
        List<String> actions) {}
