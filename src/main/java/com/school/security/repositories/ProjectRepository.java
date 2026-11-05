package com.school.security.repositories;

import com.school.security.entities.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Accès aux projets.
 *
 * <p>Requêtes dérivées triviales : {@code findByIsActiveTrue} (projets actifs)
 * et {@code findByIsActiveFalse} (projets inactifs/archivés).
 *
 * <p>Requêtes JPQL "accessibles" : un projet est accessible à un utilisateur
 * s'il en est propriétaire OU s'il figure parmi ses contributeurs
 * ({@code ProjectContributor}), avec {@code DISTINCT} pour éviter les doublons
 * issus de la jointure. Point constaté : {@link #findAccessibleProjectsByUserId}
 * est identique à {@link #findActiveByOwnerOrContributor} (duplication).
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsActiveTrue();

    List<Project> findByIsActiveFalse();

    /** Projets actifs dont l'utilisateur est propriétaire ou contributeur. */
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectContributor pc ON pc.project = p " +
           "WHERE p.isActive = true AND (p.owner.usersId = :userId OR pc.user.usersId = :userId)")
    List<Project> findActiveByOwnerOrContributor(@Param("userId") Long userId);

    /** Tous les projets (actifs ou non) dont l'utilisateur est propriétaire ou contributeur. */
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectContributor pc ON pc.project = p " +
           "WHERE p.owner.usersId = :userId OR pc.user.usersId = :userId")
    List<Project> findAllByOwnerOrContributor(@Param("userId") Long userId);

    /**
     * Projets actifs accessibles à l'utilisateur ; requête identique à
     * {@link #findActiveByOwnerOrContributor}.
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN ProjectContributor pc ON pc.project = p " +
           "WHERE p.isActive = true AND (p.owner.usersId = :userId OR pc.user.usersId = :userId)")
    List<Project> findAccessibleProjectsByUserId(@Param("userId") Long userId);
}
