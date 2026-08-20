package com.school.security.repositories;

import com.school.security.entities.ProjectContributor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectContributorRepository extends JpaRepository<ProjectContributor, Long> {
    List<ProjectContributor> findByProjectProjectIdOrderByAddedAtDesc(Long projectId);
    boolean existsByProjectProjectIdAndUserUsersId(Long projectId, Long userId);
    void deleteByProjectProjectIdAndUserUsersId(Long projectId, Long userId);
    long countByProjectProjectId(Long projectId);
    List<ProjectContributor> findByProjectProjectId(Long projectId);
}
