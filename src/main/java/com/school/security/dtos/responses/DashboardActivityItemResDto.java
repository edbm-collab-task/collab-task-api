package com.school.security.dtos.responses;

import java.time.LocalDateTime;

public record DashboardActivityItemResDto(
        Long id,
        String type,
        String description,
        String userName,
        String projectName,
        LocalDateTime createdAt) {}
