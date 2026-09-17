package com.school.security.enums;

/**
 * Types de permissions autorisées dans l'application.
 *
 * <p>Liste utilisée par les annotations {@code @PreAuthorize} dans {@link
 * security.config.SecurityConfig} et les services. Les permissions suivantes
 * sont définies ; leur effet réel est contrôlé par le evaluateur de permissions
 * côté contrôleur/service.
 */
public enum PermissionType {
    VIEW_USERS,
    MANAGE_USERS,
    MANAGE_ADMINS,
    MANAGE_ROLES,
    MANAGE_PROJECTS,
    MANAGE_PROJECT_CONTRIBUTORS,
    MANAGE_DIRECTIONS,
    MANAGE_STATUSES,
    VIEW_REPORTS
}
