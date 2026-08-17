package com.school.security.dtos.responses;

import java.time.LocalDate;

public record TaskResDto(
        Long taskId,
        String title,
        String description,
        LocalDate dueDate,
        Boolean isActive,
        Long projectId,
        String projectTitle,
        Long priorityId,
        String priorityName,
        Long statusId,
        String statusName,
        Long parentTaskId) {}
