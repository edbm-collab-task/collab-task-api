package com.school.security.repositories;

import com.school.security.entities.UserProjectPermission;
import com.school.security.enums.PermissionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Accès aux permissions utilisateur-par-projet.
 *
 * <p>Les requêtes JPQL portent sur la clé
 * {@code (user.usersId, project.projectId, permissionName)}. Les méthodes
 * {@code @Modifying} effectuent des suppressions en masse ; elles ne sont pas
 * annotées {@code @Transactional} sur le repository et nécessitent donc une
 * transaction ouverte par l'appelant.
 */
@Repository
public interface UserProjectPermissionRepository extends JpaRepository<UserProjectPermission, Long> {

    /** Indique si l'utilisateur possède la permission donnée sur le projet. */
    @Query("SELECT CASE WHEN COUNT(upp) > 0 THEN true ELSE false END FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId AND upp.permissionName = :permissionName")
    boolean existsByUserIdAndProjectIdAndPermissionName(@Param("userId") Long userId, @Param("projectId") Long projectId, @Param("permissionName") PermissionType permissionName);

    /** Toutes les permissions de l'utilisateur sur le projet (tous types). */
    @Query("SELECT upp FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId")
    List<UserProjectPermission> findByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /** Supprime toutes les permissions de l'utilisateur sur le projet. */
    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId")
    void deleteByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /** Supprime toutes les permissions associées au projet (tous utilisateurs). */
    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.project.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    /** Supprime la permission précise d'un utilisateur sur un projet. */
    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId AND upp.permissionName = :permission")
    void deleteByUserIdAndProjectIdAndPermission(@Param("userId") Long userId, @Param("projectId") Long projectId, @Param("permission") PermissionType permission);
}
