package com.school.security.repositories;

import com.school.security.entities.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository des pièces jointes (tâches et messages).
 *
 * <p>Les pièces jointes de message ne sont pas persistées explicitement : elles
 * le sont par cascade depuis {@code Message#attachments}. Les méthodes
 * déclarées ici couvrent les pièces jointes de tâche, gérées directement par
 * le contrôleur.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskTaskIdOrderByUploadedAtDesc(Long taskId);
    List<Attachment> findByMessageMessageIdOrderByUploadedAtAsc(Long messageId);
    void deleteByTaskTaskId(Long taskId);
    long countByTaskTaskId(Long taskId);
}
