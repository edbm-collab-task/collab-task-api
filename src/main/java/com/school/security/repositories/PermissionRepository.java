package com.school.security.repositories;

import com.school.security.entities.Permission;
import com.school.security.enums.PermissionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(PermissionType name);

    /**
     * Permissions triées par catégorie puis par nom, afin que le regroupement
     * par catégorie soit stable d'un appel à l'autre côté client.
     */
    List<Permission> findAllByOrderByCategoryPermissionAscNameAsc();
}
