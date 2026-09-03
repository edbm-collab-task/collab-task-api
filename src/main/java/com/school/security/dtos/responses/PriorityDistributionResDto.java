package com.school.security.dtos.responses;

public record PriorityDistributionResDto(
        String priorityName,
        String color,
        long count
) {}