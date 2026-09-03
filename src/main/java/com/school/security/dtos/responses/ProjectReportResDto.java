package com.school.security.dtos.responses;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectReportResDto(
        Long projectId,
        String projectTitle,
        String projectDescription,
        LocalDate startDate,
        LocalDate endDate,
        String ownerName,
        String ownerEmail,
        String directionName,
        String status,
        int progressPercent,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        int overdueTasks,
        int todoTasks,
        String globalStatus,
        String globalStatusLabel,
        List<TaskReportResDto> tasks,
        List<MilestoneResDto> milestones,
        List<BudgetItemResDto> budgetItems,
        List<RiskResDto> risks,
        List<ActionItemResDto> actionItems,
        String projectManagerName,
        String projectManagerEmail,
        LocalDateTime generatedAt
) {}