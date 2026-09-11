package com.school.security.repositories;

import com.school.security.entities.UserProjectPermission;
import com.school.security.enums.PermissionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProjectPermissionRepository extends JpaRepository<UserProjectPermission, Long> {

    @Query("SELECT CASE WHEN COUNT(upp) > 0 THEN true ELSE false END FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId AND upp.permissionName = :permissionName")
    boolean existsByUserIdAndProjectIdAndPermissionName(@Param("userId") Long userId, @Param("projectId") Long projectId, @Param("permissionName") PermissionType permissionName);

    @Query("SELECT upp FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId")
    List<UserProjectPermission> findByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId")
    void deleteByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.project.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("DELETE FROM UserProjectPermission upp WHERE upp.user.usersId = :userId AND upp.project.projectId = :projectId AND upp.permissionName = :permission")
    void deleteByUserIdAndProjectIdAndPermission(@Param("userId") Long userId, @Param("projectId") Long projectId, @Param("permission") PermissionType permission);
}
