package com.school.security.dtos.responses;

import com.school.security.enums.ActivityType;
import java.time.LocalDateTime;

public record ActivityResDto(
        Long activityId,
        Long projectId,
        String projectTitle,
        Long userId,
        String userName,
        ActivityType type,
        String description,
        Long taskId,
        LocalDateTime createdAt
) {}
