package com.school.security.repositories;

import com.school.security.entities.Activity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT a FROM Activity a " +
           "WHERE a.project.projectId IN :projectIds " +
           "AND a.createdAt >= :start AND a.createdAt < :end " +
           "ORDER BY a.createdAt DESC")
    List<Activity> findRecentByProjectIdsAndPeriod(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
