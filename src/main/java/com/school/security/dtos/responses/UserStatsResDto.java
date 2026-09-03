package com.school.security.dtos.responses;

public record UserStatsResDto(
        Long userId,
        String firstname,
        String lastname,
        String email,
        String role,
        String direction,
        long assignedTasks,
        long completedTasks,
        long inProgressTasks,
        long overdueTasks,
        String lastActivity
) {}