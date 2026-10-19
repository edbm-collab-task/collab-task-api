package com.school.security.dtos.responses;

public record PermissionSummary(
        String name,
        Integer totalActions,
        Integer totalActionsAccordees) {}