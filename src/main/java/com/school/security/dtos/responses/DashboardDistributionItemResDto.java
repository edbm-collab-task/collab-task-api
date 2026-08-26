package com.school.security.dtos.responses;

public record DashboardDistributionItemResDto(
        String key,
        String name,
        long count,
        String dot,
        String color) {}
