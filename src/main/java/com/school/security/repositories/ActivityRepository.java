package com.school.security.repositories;

import com.school.security.entities.Activity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);
}
