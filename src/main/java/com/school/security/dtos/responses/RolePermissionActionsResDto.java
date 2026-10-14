package com.school.security.dtos.responses;

import java.util.List;

/**
 * Actions d'un module pour un rôle donné, avec les deux totaux (masques).
 *
 * <pre>
 * {
 *   "actions": [
 *     { "codeAction": 1, "name": "INSERT", "label": "Insérer" },
 *     { "codeAction": 2, "name": "SELECT", "label": "Voir" }
 *   ],
 *   "totalActions": 15,
 *   "totalActionsAccordees": 3
 * }
 * </pre>
 *
 * <p>{@code actions} liste toutes les actions <em>possibles</em> sur le module
 * (et non seulement celles accordées). C'est en comparant
 * {@code totalActionsAccordees} à chaque {@code codeAction} par un ET bit à bit
 * que le client détermine quelles actions sont cochées.
 *
 * @param totalActions masque des actions disponibles sur le module
 * @param totalActionsAccordees masque des actions accordées au rôle sur ce module
 * @param actions toutes les actions disponibles sur le module (avec code et libellé)
 */
public record RolePermissionActionsResDto(
        Integer totalActions,
        Integer totalActionsAccordees,
        List<ActionResDto> actions) {}
