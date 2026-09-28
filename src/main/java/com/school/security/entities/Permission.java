package com.school.security.entities;

import com.school.security.enums.PermissionCategoryType;
import com.school.security.enums.PermissionType;
import jakarta.persistence.*;
import java.io.Serializable;
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
}
