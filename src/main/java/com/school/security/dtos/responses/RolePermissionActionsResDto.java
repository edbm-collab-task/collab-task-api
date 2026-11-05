package com.school.security.dtos.responses;

/**
 * Actions d'un module pour un rôle donné, sous forme de masques binaires.
 *
 * <pre>
 * {
 *   "totalActions": 15,
 *   "totalActionsAccordees": 3
 * }
 * </pre>
 *
 * <p>{@code totalActions} est le masque des actions <em>disponibles</em> sur le
 * module ; {@code totalActionsAccordees} celui des actions <em>accordées</em> au
 * rôle. Le détail des actions ({@code codeAction}, nom, libellé) est fourni une
 * seule fois par {@code GET /roles/actions} ; le client détermine les cases
 * cochées par un ET bit à bit entre les deux masques.
 *
 * @param totalActions masque des actions disponibles sur le module
 * @param totalActionsAccordees masque des actions accordées au rôle sur ce module
 */
public record RolePermissionActionsResDto(
        Integer totalActions,
        Integer totalActionsAccordees) {}
