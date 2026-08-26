package com.school.security.repositories;

import com.school.security.entities.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectProjectId(Long projectId);

    List<Task> findByProjectProjectIdAndIsActiveTrue(Long projectId);

    List<Task> findByIsActiveTrue();

    List<Task> findByParentIsNull();

    long countByIsActiveTrueAndProjectProjectIdIn(List<Long> projectIds);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end")
    long countCompletedBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :today " +
           "AND t.status.name <> :completedStatusName")
    long countOverdue(
            @Param("projectIds") List<Long> projectIds,
            @Param("today") LocalDate today,
            @Param("completedStatusName") String completedStatusName);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.createdAt >= :start AND t.createdAt < :end")
    long countCreatedBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.status.name, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "GROUP BY t.status.name")
    List<Object[]> countByStatusGrouped(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT t.createdAt FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.createdAt >= :start AND t.createdAt < :end " +
           "ORDER BY t.createdAt")
    List<LocalDateTime> findCreatedDatesBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.completedAt FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end " +
           "ORDER BY t.completedAt")
    List<LocalDateTime> findCompletedDatesBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.project.projectId, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "GROUP BY t.project.projectId")
    List<Object[]> countActiveByProjectGrouped(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT t.project.projectId, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.status.name = :statusName " +
           "GROUP BY t.project.projectId")
    List<Object[]> countByStatusAndProjectGrouped(
            @Param("projectIds") List<Long> projectIds,
            @Param("statusName") String statusName);
}
