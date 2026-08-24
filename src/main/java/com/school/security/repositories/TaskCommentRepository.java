package com.school.security.repositories;

import com.school.security.entities.TaskComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskTaskIdAndParentCommentIsNullOrderByCreatedAtDesc(Long taskId);
    long countByTaskTaskId(Long taskId);
}
