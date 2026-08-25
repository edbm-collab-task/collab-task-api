package com.school.security.dtos.responses;

import java.util.List;

public record DashboardDistributionResDto(
        List<DashboardDistributionItemResDto> items,
        long total) {}
