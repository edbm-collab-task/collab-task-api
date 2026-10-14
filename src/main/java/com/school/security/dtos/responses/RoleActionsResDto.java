package com.school.security.dtos.responses;

import java.util.Map;

/**
 * Actions autorisées pour un rôle, regroupées par module (permission).
 *
 * <p>Chaque module expose ses actions possibles et les deux totaux (masques) :
 *
 * <pre>
 * {
 *   "label": "ADMIN",
 *   "permissions": {
 *     "MANAGE_USERS": {
 *       "totalActions": 7,
 *       "totalActionsAccordees": 3,
 *       "actions": [
 *         { "codeAction": 1, "name": "INSERT", "label": "Insérer" },
 *         { "codeAction": 2, "name": "SELECT", "label": "Voir" },
 *         { "codeAction": 4, "name": "UPDATE", "label": "Modifier" }
 *       ]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Sémantique des {@code permissions} : une clé absente = aucun accès au
 * module ; le couple de totaux permet au client de calculer les actions
 * accordées via un ET bit à bit.
 *
 * @param label nom du rôle
 * @param permissions actions par nom de module ({@code PermissionType})
 */
public record RoleActionsResDto(String label, Map<String, RolePermissionActionsResDto> permissions) {}
