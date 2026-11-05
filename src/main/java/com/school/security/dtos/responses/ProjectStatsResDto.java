package com.school.security.dtos.responses;

public record ProjectStatsResDto(
        Long projectId,
        String title,
        String ownerName,
        String direction,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        int progressPercent,
        String status
) {}