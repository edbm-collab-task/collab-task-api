package com.school.security.dtos.responses;

public record DashboardRecentProjectResDto(
        Long projectId,
        String title,
        String ownerName,
        int progress) {}
