package com.school.security.dtos.responses;

public record RiskResDto(
        Long riskId,
        String title,
        String impact,
        String mitigationPlan,
        String assigneeName
) {}