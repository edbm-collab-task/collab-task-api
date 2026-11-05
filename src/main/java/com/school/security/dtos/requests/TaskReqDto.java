package com.school.security.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TaskReqDto(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        String description,
        LocalDate dueDate,
        @NotNull(message = "Project is required") Long projectId,
        @NotNull(message = "Priority is required") Long priorityId,
        @NotNull(message = "Status is required") Long statusId,
        Long parentTaskId,
        List<Long> assigneeIds) {}
