package com.school.security.dtos.responses;

public record AssigneeWorkloadResDto(
        Long userId,
        String firstname,
        String lastname,
        String email,
        String direction,
        long totalAssigned,
        long completed,
        long inProgress,
        long overdue
) {}