package com.school.security.dtos.responses;

import java.util.List;

/**
 * Représentation d'un rôle exposé par l'API.
 *
 * <p>Les permissions sont renvoyées sous leur forme complète ({@link
 * PermissionResDto}) afin que le client puisse les regrouper par catégorie sans
 * second appel à {@code GET /roles/permissions}. Le côté requête ({@code
 * RoleReqDto}) reste exprimé en noms de permissions.
 *
 * @param id identifiant du rôle
 * @param name nom du rôle
 * @param codeRole code court du rôle
 * @param permissions permissions affectées au rôle
 */
public record RoleResDto(
        Long id, String name, String codeRole, List<PermissionResDto> permissions) {}
