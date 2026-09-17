package com.school.security.repositories;

import com.school.security.entities.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Accès aux tâches : requêtes dérivées simples et requêtes JPQL de comptage /
 * d'agrégation utilisées par les tableaux de bord et rapports.
 *
 * <p>Conventions des requêtes JPQL ci-dessous (documentées, non modifiées) :
 * <ul>
 *   <li>sauf mention contraire, elles filtrent {@code t.isActive = true} et un
 *       ensemble de projets {@code t.project.projectId IN :projectIds} ; ces
 *       listes sont supposées non vides (les appelants vérifient le périmètre
 *       avant appel) ;</li>
 *   <li>les intervalles de dates sont semi-ouverts {@code [start, end)} ;</li>
 *   <li>les méthodes renvoyant {@code List<Object[]>} attendent des lignes
 *       {@code [clé, comptage]} (nom de statut ou identifiant de projet selon la
 *       requête) ;</li>
 *   <li>les variantes "AndAssignedTo" / "ByUser" ajoutent une jointure sur les
 *       assignés ({@code JOIN t.assignees a}) et restreignent à
 *       {@code a.usersId = :userId} ;</li>
 *   <li>l'"en retard" est défini par {@code dueDate < :today} et un statut
 *       différent du statut terminé fourni ; les tâches sans échéance ou à
 *       statut {@code null} ne sont pas comptées.</li>
 * </ul>
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    /** Toutes les tâches d'un projet, actives ou non. */
    List<Task> findByProjectProjectId(Long projectId);

    /** Tâches actives d'un projet. */
    List<Task> findByProjectProjectIdAndIsActiveTrue(Long projectId);

    /** Toutes les tâches actives. */
    List<Task> findByIsActiveTrue();

    @Query("SELECT t FROM Task t WHERE t.project.projectId = :projectId AND t.status.statusId = :statusId ORDER BY t.sortOrder ASC")
List<Task> findByProjectProjectIdAndStatusIdOrderBySortOrderAsc(@Param("projectId") Long projectId, @Param("statusId") Long statusId);

    List<Task> findByParentIsNull();

    /** Nombre de tâches actives dans les projets donnés. */
    long countByIsActiveTrueAndProjectProjectIdIn(List<Long> projectIds);

    /**
     * Nombre de tâches actives terminées ({@code completedAt} non nul) dont la
     * date d'achèvement est dans {@code [start, end)}.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end")
    long countCompletedBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Nombre de tâches actives en retard : échéance non nulle strictement
     * antérieure à {@code :today} et statut différent de
     * {@code :completedStatusName}.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :today " +
           "AND t.status.name <> :completedStatusName")
    long countOverdue(
            @Param("projectIds") List<Long> projectIds,
            @Param("today") LocalDate today,
            @Param("completedStatusName") String completedStatusName);

    /**
     * Nombre de tâches actives créées dans {@code [start, end)}.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.createdAt >= :start AND t.createdAt < :end")
    long countCreatedBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Comptage des tâches actives par nom de statut ; lignes
     * {@code [statusName, count]}.
     */
    @Query("SELECT t.status.name, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "GROUP BY t.status.name")
    List<Object[]> countByStatusGrouped(@Param("projectIds") List<Long> projectIds);

    /**
     * Dates de création des tâches actives de {@code [start, end)}, triées
     * croissantes (alimente les séries d'évolution).
     */
    @Query("SELECT t.createdAt FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.createdAt >= :start AND t.createdAt < :end " +
           "ORDER BY t.createdAt")
    List<LocalDateTime> findCreatedDatesBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Dates d'achèvement (non nulles) des tâches actives de {@code [start, end)},
     * triées croissantes.
     */
    @Query("SELECT t.completedAt FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end " +
           "ORDER BY t.completedAt")
    List<LocalDateTime> findCompletedDatesBetween(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Comptage des tâches actives par projet ; lignes {@code [projectId, count]}.
     */
    @Query("SELECT t.project.projectId, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "GROUP BY t.project.projectId")
    List<Object[]> countActiveByProjectGrouped(@Param("projectIds") List<Long> projectIds);

    /**
     * Comptage des tâches actives par projet pour un statut donné ; lignes
     * {@code [projectId, count]}.
     */
    @Query("SELECT t.project.projectId, COUNT(t) FROM Task t " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.status.name = :statusName " +
           "GROUP BY t.project.projectId")
    List<Object[]> countByStatusAndProjectGrouped(
            @Param("projectIds") List<Long> projectIds,
            @Param("statusName") String statusName);

    /**
     * Nombre de tâches actives assignées à {@code :userId} (jointure sur les
     * assignés).
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.assignees a " +
           "WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND a.usersId = :userId")
    long countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(
            @Param("projectIds") List<Long> projectIds,
            @Param("userId") Long userId);

    /**
     * Tâches actives terminées dans {@code [start, end)} et assignées à
     * {@code :userId}.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.assignees a " +
           "WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end " +
           "AND a.usersId = :userId")
    long countCompletedBetweenAndAssignedTo(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("userId") Long userId);

    /**
     * Tâches actives en retard (voir {@link #countOverdue}) et assignées à
     * {@code :userId}.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "JOIN t.assignees a " +
           "WHERE t.isActive = true " +
           "AND t.project.projectId IN :projectIds " +
           "AND t.dueDate IS NOT NULL " +
           "AND t.dueDate < :today " +
           "AND t.status.name <> :completedStatusName " +
           "AND a.usersId = :userId")
    long countOverdueAndAssignedTo(
            @Param("projectIds") List<Long> projectIds,
            @Param("today") LocalDate today,
            @Param("completedStatusName") String completedStatusName,
            @Param("userId") Long userId);

    /**
     * Dates de création des tâches actives assignées à {@code :userId} sur
     * {@code [start, end)}, triées croissantes.
     */
    @Query("SELECT t.createdAt FROM Task t " +
           "JOIN t.assignees a " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.createdAt >= :start AND t.createdAt < :end " +
           "AND a.usersId = :userId " +
           "ORDER BY t.createdAt")
    List<LocalDateTime> findCreatedDatesBetweenAndAssignedTo(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("userId") Long userId);

    /**
     * Dates d'achèvement des tâches actives assignées à {@code :userId} sur
     * {@code [start, end)}, triées croissantes.
     */
    @Query("SELECT t.completedAt FROM Task t " +
           "JOIN t.assignees a " +
           "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
           "AND t.completedAt IS NOT NULL " +
           "AND t.completedAt >= :start AND t.completedAt < :end " +
           "AND a.usersId = :userId " +
           "ORDER BY t.completedAt")
    List<LocalDateTime> findCompletedDatesBetweenAndAssignedTo(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("userId") Long userId);

    /**
     * Comptage par statut des tâches actives assignées à {@code :userId} ;
     * lignes {@code [statusName, count]}.
     */
    @Query("SELECT t.status.name, COUNT(t) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true AND t.project.projectId IN :projectIds " +
            "AND a.usersId = :userId " +
            "GROUP BY t.status.name")
    List<Object[]> countByStatusGroupedAndAssignedTo(
            @Param("projectIds") List<Long> projectIds,
            @Param("userId") Long userId);

    /**
     * Tâches actives assignées à {@code :userId} ; même sémantique que
     * {@link #countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains}.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds " +
            "AND a.usersId = :userId")
    long countActiveByUserAndProjects(
            @Param("projectIds") List<Long> projectIds,
            @Param("userId") Long userId);

    /**
     * Tâches actives terminées sur la période et assignées à {@code :userId} ;
     * même sémantique que {@link #countCompletedBetweenAndAssignedTo}.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds " +
            "AND t.completedAt IS NOT NULL " +
            "AND t.completedAt >= :start AND t.completedAt < :end " +
            "AND a.usersId = :userId")
    long countCompletedByUserAndPeriod(
            @Param("projectIds") List<Long> projectIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("userId") Long userId);

    /**
     * Tâches actives d'un statut donné assignées à {@code :userId} (comptage
     * global, non groupé).
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds " +
            "AND t.status.name = :statusName " +
            "AND a.usersId = :userId")
    long countByStatusAndUserAndProjects(
            @Param("projectIds") List<Long> projectIds,
            @Param("statusName") String statusName,
            @Param("userId") Long userId);

    /**
     * Tâches actives en retard assignées à {@code :userId} ; même sémantique
     * que {@link #countOverdueAndAssignedTo}.
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds " +
            "AND t.dueDate IS NOT NULL " +
            "AND t.dueDate < :today " +
            "AND t.status.name <> :completedStatusName " +
            "AND a.usersId = :userId")
    long countOverdueByUser(
            @Param("projectIds") List<Long> projectIds,
            @Param("today") LocalDate today,
            @Param("completedStatusName") String completedStatusName,
            @Param("userId") Long userId);

    /**
     * Nombre d'utilisateurs distincts assignés à au moins une tâche active des
     * projets ; les assignés sont dédupliqués.
     */
    @Query("SELECT COUNT(DISTINCT a.usersId) FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds")
    long countDistinctContributors(@Param("projectIds") List<Long> projectIds);

    /**
     * Identifiants des assignés des tâches actives ; un même utilisateur
     * apparaît autant de fois qu'il a de tâches (pas de déduplication).
     */
    @Query("SELECT a.usersId FROM Task t " +
            "JOIN t.assignees a " +
            "WHERE t.isActive = true " +
            "AND t.project.projectId IN :projectIds")
    List<Long> findAssigneeIdsByProjectId(@Param("projectIds") List<Long> projectIds);
}
