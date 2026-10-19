package com.school.security.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite de {@link RolePermission}, calquée sur la clé primaire déjà
 * présente sur la table {@code roles_permissions} ({@code roles_id},
 * {@code permission_id}).
 *
 * <p>Les noms des attributs ({@code role}, {@code permission}) doivent
 * correspondre à ceux des champs {@code @Id} de l'entité ; leurs types sont
 * ceux des identifiants des entités référencées ({@code Role.rolesId},
 * {@code Permission.permissionId}).
 */
public class RolePermissionId implements Serializable {

    private Long role;
    private Long permission;

    public RolePermissionId() {}

    public RolePermissionId(Long role, Long permission) {
        this.role = role;
        this.permission = permission;
    }

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
    }

    public Long getPermission() {
        return permission;
    }

    public void setPermission(Long permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePermissionId that)) return false;
        return Objects.equals(role, that.role) && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, permission);
    }
}
