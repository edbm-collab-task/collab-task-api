package com.school.security.repositories;

import com.school.security.entities.Notification;
import com.school.security.enums.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Accès aux notifications.
 *
 * <p>Requêtes dérivées simples : liste d'un utilisateur triée par date
 * décroissante, comptage des non lues, et suppression (dérivée, nécessitant un
 * contexte transactionnel) de toutes les notifications d'un utilisateur.
 *
 * <p>Point non évident : {@code existsByUserUsersIdAndTypeAndProjectIdAndTaskId}
 * sert de clé de déduplication (utilisateur, type, projet, tâche) ; un
 * {@code taskId} ou {@code projectId} à {@code null} est traité comme
 * {@code IS NULL} par Spring Data, ce qui permet la déduplication des alertes
 * au niveau projet (sans tâche).
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserUsersIdOrderByCreatedAtDesc(Long userId);
    long countByUserUsersIdAndIsReadFalse(Long userId);
    void deleteByUserUsersId(Long userId);
    boolean existsByUserUsersIdAndTypeAndProjectIdAndTaskId(Long userId, NotificationType type, Long projectId, Long taskId);
}
