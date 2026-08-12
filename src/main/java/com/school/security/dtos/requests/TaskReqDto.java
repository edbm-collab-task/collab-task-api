package com.school.security.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TaskReqDto(
        @NotBlank(message = "Title is required") String title,
        String description,
        LocalDate dueDate,
        @NotNull(message = "Project is required") Long projectId,
        @NotNull(message = "Priority is required") Long priorityId,
        @NotNull(message = "Status is required") Long statusId,
        Long parentTaskId) {}
