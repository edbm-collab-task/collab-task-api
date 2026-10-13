package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/**
 * Association entre un {@link Role} et une {@link Permission}, enrichie des
 * {@link com.school.security.enums.ActionType actions} accordées sur ce module.
 *
 * <p>Représente la table existante {@code roles_permissions} : c'est la même
 * table, pas une table supplémentaire. La colonne {@code actions} stocke une
 * liste d'actions séparées par des virgules (ex. {@code "INSERT,UPDATE"}) ;
 * {@code null} ou vide signifie « accès en lecture seule ».
 *
 * <p>La clé primaire est composite ({@code roles_id}, {@code permission_id}) :
 * une seule ligne par couple (rôle, permission).
 */
@Entity
@Table(name = "roles_permissions")
@IdClass(RolePermissionId.class)
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "roles_id", nullable = false)
    private Role role;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * Actions accordées, séparées par des virgules (ex. {@code "INSERT,UPDATE"}).
     * Vide ou {@code null} = accès seul (lecture).
     */
    @Column(name = "actions", length = 255)
    private String actions;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }
}
