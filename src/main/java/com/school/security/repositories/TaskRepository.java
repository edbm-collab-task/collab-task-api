package com.school.security.repositories;

import com.school.security.entities.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectProjectId(Long projectId);

    List<Task> findByIsActiveTrue();

    List<Task> findByParentIsNull();
}
