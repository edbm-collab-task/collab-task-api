package com.school.security.repositories;

import com.school.security.entities.RolePermission;
import com.school.security.entities.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    /**
     * Associations (rôle, permission) rattachées à une permission donnée.
     * Utilisé pour recaler (clamp) les masques accordés lorsqu'une action est
     * retirée d'un module.
     */
    List<RolePermission> findByPermissionPermissionId(Long permissionId);
}
