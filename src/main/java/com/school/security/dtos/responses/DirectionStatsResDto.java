package com.school.security.dtos.responses;

public record DirectionStatsResDto(
        Long directionId,
        String name,
        long totalUsers,
        long totalProjects,
        long totalTasks,
        long completedTasks
) {}