package com.school.security.repositories;

import com.school.security.entities.Risk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskRepository extends JpaRepository<Risk, Long> {
    List<Risk> findByProjectProjectId(Long projectId);
}