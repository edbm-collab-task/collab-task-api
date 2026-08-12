package com.school.security.dtos.requests;

import java.time.LocalDate;

public record TaskReqDto(
        String title,
        String description,
        LocalDate dueDate,
        Long projectId,
        Long priorityId,
        Long statusId,
        Long parentTaskId) {}
