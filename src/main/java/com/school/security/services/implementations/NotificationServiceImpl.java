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

@Service
@Transactional
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;

    @Override
    public List<NotificationResDto> findByUserId(Long userId) {
        return notificationRepository.findByUserUsersIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserUsersIdAndIsReadFalse(userId);
    }

    @Override
    public NotificationResDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));
        notification.setIsRead(true);
        return toDto(notificationRepository.save(notification));
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserUsersIdOrderByCreatedAtDesc(userId);
        for (Notification n : notifications) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

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
