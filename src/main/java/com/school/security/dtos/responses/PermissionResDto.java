package com.school.security.dtos.responses;

import com.school.security.enums.PermissionCategoryType;

/**
 * Représentation d'une permission exposée par l'API.
 *
 * @param id identifiant de la permission
 * @param name nom technique de la permission (valeur de {@code PermissionType})
 * @param description libellé descriptif
 * @param categoryPermission catégorie d'appartenance, utilisée par le client
 *     pour regrouper les permissions
 */
public record PermissionResDto(
        Long id,
        String name,
        String description,
        PermissionCategoryType categoryPermission) {}
