package com.school.security.dtos.responses;

public record DashboardStatsResDto(
        long projects,
        long tasks,
        long completedTasks,
        long overdueTasks,
        long totalUsers) {}
