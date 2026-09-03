package com.school.security.dtos.responses;

import java.time.LocalDate;

public record ActionItemResDto(
        Long actionId,
        String description,
        String assigneeName,
        LocalDate dueDate,
        String status
) {}