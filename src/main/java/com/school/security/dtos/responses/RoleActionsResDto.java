package com.school.security.dtos.responses;

import java.util.List;
import java.util.Map;

/**
 * Actions autorisées pour un rôle, regroupées par module (permission).
 *
 * <p>Forme renvoyée au sein d'une map indexée par identifiant de rôle :
 * <pre>
 * {
 *   "1": {
 *     "label": "ADMIN",
 *     "permissions": {
 *       "MANAGE_USERS": ["INSERT", "UPDATE"],
 *       "VIEW_REPORTS": ["GENERATE_REPORT"]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Sémantique des {@code permissions} : une clé absente = aucun accès ;
 * une clé avec une liste vide = accès en lecture seule ; une clé avec des
 * actions = accès + ces actions.
 *
 * @param label nom du rôle
 * @param permissions actions par nom de module ({@code PermissionType})
 */
public record RoleActionsResDto(String label, Map<String, List<String>> permissions) {}
