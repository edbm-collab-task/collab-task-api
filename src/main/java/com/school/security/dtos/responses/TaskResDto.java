package com.school.security.dtos.responses;

import java.time.LocalDate;
import java.util.List;

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
        Long parentTaskId,
        List<AssigneeResDto> assignees) {

    public record AssigneeResDto(
            Long userId,
            String firstname,
            String lastname,
            String email,
            String imagePath
    ) {}
}
