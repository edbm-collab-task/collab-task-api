package com.school.security.dtos.responses;

public record BudgetItemResDto(
        String category,
        double plannedAmount,
        double actualAmount,
        double variancePercent
) {}