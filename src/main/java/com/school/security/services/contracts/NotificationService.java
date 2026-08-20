package com.school.security.services.contracts;

import com.school.security.dtos.responses.NotificationResDto;
import com.school.security.enums.NotificationType;
import java.util.List;

public interface NotificationService {
    List<NotificationResDto> findByUserId(Long userId);
    long countUnread(Long userId);
    NotificationResDto markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    void createNotification(Long userId, String message, NotificationType type, Long projectId, Long taskId);
}
