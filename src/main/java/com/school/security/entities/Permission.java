package com.school.security.entities;

import com.school.security.enums.PermissionCategoryType;
import com.school.security.enums.PermissionType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "permissions")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Permission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PermissionType name;

    /**
     * Catégorie d'appartenance de la permission, utilisée pour regrouper les
     * permissions dans l'interface d'administration.
     *
     * <p>Volontairement nullable : la colonne est ajoutée par Hibernate
     * ({@code ddl-auto=update}) <em>avant</em> l'exécution de {@code data.sql},
     * qui la renseigne ensuite (même stratégie que {@code Priority.sortOrder}).
     * Un {@code nullable = false} ferait échouer le démarrage sur une base
     * contenant déjà des permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category_permission", length = 50)
    private PermissionCategoryType categoryPermission;

    private String description;

    /**
     * Somme des {@code code_action} des actions rattachées à ce module
     * (masque binaire). Exposé à l'API pour que le frontend sache quelles
     * actions sont <em>possibles</em> sur ce module. Recalculé à chaque
     * modification de {@link #actions}.
     */
    @Column(name = "total_actions")
    private Integer totalActions;

    /**
     * Actions disponibles pour ce module (relation plusieurs-à-plusieurs via la
     * table {@code permissions_actions}). Ajustable par l'administration :
     * retirer une action la retire aussi des rôles qui la détenaient (voir
     * {@code RoleServiceImpl.updatePermissionActions}).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "permissions_actions",
            joinColumns = @JoinColumn(name = "permission_id"),
            inverseJoinColumns = @JoinColumn(name = "actions_id"))
    private Set<Action> actions = new LinkedHashSet<>();

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public PermissionType getName() {
        return name;
    }

    public void setName(PermissionType name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PermissionCategoryType getCategoryPermission() {
        return categoryPermission;
    }

    public void setCategoryPermission(PermissionCategoryType categoryPermission) {
        this.categoryPermission = categoryPermission;
    }

    public Integer getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(Integer totalActions) {
        this.totalActions = totalActions;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public void setActions(Set<Action> actions) {
        this.actions = actions;
    }
}
