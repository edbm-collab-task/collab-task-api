package com.school.security.repositories;

import com.school.security.entities.TaskComment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskTaskIdAndParentCommentIsNullOrderByCreatedAtDesc(Long taskId);
    long countByTaskTaskId(Long taskId);

    /**
     * Projection du nombre total de commentaires (racines + réponses) par tâche.
     */
    interface TaskCommentCount {
        Long getTaskId();
        Long getCnt();
    }

    /**
     * Compte, en UNE seule requête GROUP BY, les commentaires de toutes les
     * tâches passées en paramètre. Évite le N+1 dans les listes de tâches.
     */
    @Query("SELECT tc.task.taskId AS taskId, COUNT(tc) AS cnt FROM TaskComment tc "
         + "WHERE tc.task.taskId IN :taskIds GROUP BY tc.task.taskId")
    List<TaskCommentCount> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}