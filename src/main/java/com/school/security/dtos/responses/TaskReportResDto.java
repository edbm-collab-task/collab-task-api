package com.school.security.dtos.responses;

import java.time.LocalDate;

public record TaskReportResDto(
        Long taskId,
        String title,
        String description,
        String status,
        String statusLabel,
        String assigneeNames,
        String priority,
        LocalDate dueDate
) {}