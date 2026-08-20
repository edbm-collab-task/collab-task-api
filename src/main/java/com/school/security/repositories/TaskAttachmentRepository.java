package com.school.security.repositories;

import com.school.security.entities.TaskAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskTaskIdOrderByUploadedAtDesc(Long taskId);
    void deleteByTaskTaskId(Long taskId);
    long countByTaskTaskId(Long taskId);
}
