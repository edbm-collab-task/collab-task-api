package com.school.security.services.contracts;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.ActionResDto;
import com.school.security.dtos.responses.PermissionActionsResDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleActionsResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.PermissionType;
import java.util.List;
import java.util.Map;

public interface RoleService {
    List<RoleResDto> findAll();
    RoleResDto findById(Long id);
    RoleResDto create(RoleReqDto dto);
    RoleResDto update(Long id, RoleReqDto dto);
    void delete(Long id);
    List<PermissionResDto> findAllPermissions();
    void addPermissions(Long roleId, List<PermissionType> permissionNames);

    /**
     * Catalogue des permissions, chacune accompagnée de toutes les actions
     * possibles ({@code ActionType}) pour construire une page
     * « permissions + actions » côté frontend.
     */
    List<PermissionActionsResDto> findAllPermissionsWithActions();

    /**
     * Catalogue des actions gérables, chacune exposée avec son code binaire
     * et son libellé ({@code [{codeAction, name, label}]}).
     */
    List<ActionResDto> findAllActions();

    /** Actions de chaque rôle, indexées par identifiant de rôle. */
    Map<Long, RoleActionsResDto> getActionsMap();

    /** Actions d'un rôle, regroupées par module. */
    RoleActionsResDto getRoleActions(Long roleId);

    /** Remplace la configuration d'actions d'un rôle ({@code module -> [actions]}). */
    RoleActionsResDto setRoleActions(Long roleId, Map<String, List<String>> actions);

    /**
     * Redéfinit les actions disponibles d'un module (permission) : la liste
     * fournie devient la référence. Recalcule {@code totalActions} et retire des
     * masques accordés des rôles les bits devenus indisponibles.
     */
    PermissionActionsResDto updatePermissionActions(Long permissionId, List<String> actionNames);
}
