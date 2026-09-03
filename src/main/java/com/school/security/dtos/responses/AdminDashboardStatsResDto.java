package com.school.security.dtos.responses;

import java.util.List;

public record AdminDashboardStatsResDto(
        long totalUsers,
        long totalProjects,
        long activeProjects,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        long inProgressTasks,
        long todoTasks,
        List<EvolutionPointResDto> evolution,
        List<PriorityDistributionResDto> priorityDistribution,
        List<AssigneeWorkloadResDto> assigneeWorkload,
        List<UserStatsResDto> topUsers,
        List<ProjectStatsResDto> topProjects,
        List<DirectionStatsResDto> directionStats,
        List<RecentActivityResDto> recentActivity
) {}