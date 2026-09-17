package com.school.security.services.implementations;

import com.school.security.dtos.responses.NotificationResDto;
import com.school.security.entities.Notification;
import com.school.security.entities.User;
import com.school.security.enums.NotificationType;
import com.school.security.repositories.NotificationRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.NotificationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de notifications internes (persistance en base
 * uniquement).
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>{@link #markAsRead} ne vérifie PAS que la notification appartient à
 *       l'utilisateur courant : n'importe quel identifiant existant peut être
 *       marqué comme lu ;</li>
 *   <li>{@link #createNotification} effectue un simple insert, sans déduplication
 *       ni contrôle d'existence préalable ;</li>
 *   <li>ce service ne diffuse rien en temps réel (pas de dépendance WebSocket) :
 *       il se contente d'écrire/lire la table des notifications.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;

    /** Retourne toutes les notifications d'un utilisateur, de la plus récente à la plus ancienne. */
    @Override
    public List<NotificationResDto> findByUserId(Long userId) {
        return notificationRepository.findByUserUsersIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Compte les notifications non lues d'un utilisateur. */
    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserUsersIdAndIsReadFalse(userId);
    }

    /**
     * Marque une notification comme lue (sans contrôle de propriétaire) ; lève
     * une {@code RuntimeException} si l'identifiant n'existe pas.
     */
    @Override
    public NotificationResDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        notification.setIsRead(true);
        return toDto(notificationRepository.save(notification));
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues ; réécrit
     * également celles déjà lues.
     */
    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserUsersIdOrderByCreatedAtDesc(userId);
        for (Notification n : notifications) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * Crée une notification non lue pour un utilisateur ; lève une
     * {@code RuntimeException} si l'utilisateur n'existe pas.
     */
    @Override
    public void createNotification(Long userId, String message, NotificationType type, Long projectId, Long taskId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setProjectId(projectId);
        notification.setTaskId(taskId);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    /** Convertit l'entité de notification en DTO de réponse. */
    private NotificationResDto toDto(Notification entity) {
        return new NotificationResDto(
                entity.getNotificationId(),
                entity.getMessage(),
                entity.getType(),
                entity.getIsRead(),
                entity.getCreatedAt(),
                entity.getProjectId(),
                entity.getTaskId()
        );
    }
}
