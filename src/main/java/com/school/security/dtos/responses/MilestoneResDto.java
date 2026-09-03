package com.school.security.dtos.responses;

import java.time.LocalDate;

public record MilestoneResDto(
        Long milestoneId,
        String title,
        LocalDate plannedDate,
        LocalDate actualDate,
        String status,
        String statusLabel
) {}