package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/**
 * Association entre un {@link Role} et une {@link Permission}, enrichie des
 * {@link com.school.security.enums.ActionType actions} accordées sur ce module.
 *
 * <p>Représente la table existante {@code roles_permissions} : c'est la même
 * table, pas une table supplémentaire. La colonne {@code actions} stocke le
 * <strong>masque binaire</strong> des actions accordées (somme des
 * {@code code_action} de {@link com.school.security.enums.ActionType}, ex.
 * {@code 3} = INSERT + SELECT) ; {@code 0} ou {@code null} signifie « accès en
 * lecture seule ».
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
     * Masque binaire des actions accordées (somme des {@code code_action}).
     * {@code 0} ou {@code null} = accès seul (lecture).
     */
    @Column(name = "actions")
    private Integer actions;

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

    public Integer getActions() {
        return actions;
    }

    public void setActions(Integer actions) {
        this.actions = actions;
    }
}
