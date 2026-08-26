package com.school.security.repositories;

import com.school.security.entities.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsActiveTrue();

    List<Project> findByIsActiveFalse();

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectContributor pc ON pc.project = p " +
           "WHERE p.isActive = true AND (p.owner.usersId = :userId OR pc.user.usersId = :userId)")
    List<Project> findActiveByOwnerOrContributor(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectContributor pc ON pc.project = p " +
           "WHERE p.owner.usersId = :userId OR pc.user.usersId = :userId")
    List<Project> findAllByOwnerOrContributor(@Param("userId") Long userId);
}
