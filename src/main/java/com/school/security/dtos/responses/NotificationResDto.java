package com.school.security.dtos.responses;

import com.school.security.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationResDto(
        Long notificationId,
        String message,
        NotificationType type,
        Boolean isRead,
        LocalDateTime createdAt,
        Long projectId,
        Long taskId
) {}
