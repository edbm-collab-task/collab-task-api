package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.*;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roles_id")
    protected Long rolesId;

@Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "code_role", nullable = false, unique = true)
    private String codeRole;

    @ManyToMany(mappedBy = "roles")
    private List<User> users = new ArrayList<>();

    /**
     * Associations (rôle, permission) portant les actions configurées.
     * Remplace l'ancien {@code @ManyToMany} : la table {@code roles_permissions}
     * est désormais mappée par l'entité {@link RolePermission}, ce qui permet
     * d'y stocker la colonne {@code actions} sans ajouter de table.
     */
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RolePermission> rolePermissions = new ArrayList<>();

    public Long getRolesId() {
        return rolesId;
    }

    public void setRolesId(Long rolesId) {
        this.rolesId = rolesId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodeRole() {
        return codeRole;
    }

    public void setCodeRole(String codeRole) {
        this.codeRole = codeRole;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<RolePermission> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(List<RolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    /**
     * Permissions du rôle, vues comme la liste des permissions de ses
     * associations (sans les actions). Conserve l'API historique utilisée par
     * le mapper, la construction des autorités et l'évaluateur de permissions.
     */
    public List<Permission> getPermissions() {
        return rolePermissions.stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());
    }

    /**
     * Remplace les permissions du rôle en <strong>préservant les actions</strong>
     * déjà configurées pour les permissions qui restent. Les permissions
     * ajoutées démarrent avec des actions vides (accès seul), les permissions
     * retirées sont supprimées par le {@code orphanRemoval}.
     */
    public void setPermissions(List<Permission> permissions) {
        Map<Long, RolePermission> existing = rolePermissions.stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getPermissionId(),
                        rp -> rp,
                        (a, b) -> a));

        List<RolePermission> rebuilt = new ArrayList<>();
        for (Permission permission : permissions) {
            RolePermission rp = existing.get(permission.getPermissionId());
            if (rp == null) {
                rp = new RolePermission();
                rp.setRole(this);
                rp.setPermission(permission);
                rp.setActions("");
            }
            rebuilt.add(rp);
        }

        rolePermissions.clear();
        rolePermissions.addAll(rebuilt);
    }
}