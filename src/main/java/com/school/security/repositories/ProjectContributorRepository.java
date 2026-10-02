package com.school.security.repositories;

import com.school.security.entities.ProjectContributor;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectContributorRepository extends JpaRepository<ProjectContributor, Long> {

    /**
     * Contributeurs d'un projet, du plus récent au plus ancien.
     *
     * <p>L'{@link EntityGraph} précharge {@code user} et {@code project} dans la
     * requête. Sans lui, la conversion en DTO (qui lit les deux associations)
     * déclenchait deux requêtes supplémentaires par contributeur.
     */
    @EntityGraph(attributePaths = {"user", "project"})
    List<ProjectContributor> findByProjectProjectIdOrderByAddedAtDesc(Long projectId);

    boolean existsByProjectProjectIdAndUserUsersId(Long projectId, Long userId);
    void deleteByProjectProjectIdAndUserUsersId(Long projectId, Long userId);
    long countByProjectProjectId(Long projectId);
    List<ProjectContributor> findByProjectProjectId(Long projectId);
}
