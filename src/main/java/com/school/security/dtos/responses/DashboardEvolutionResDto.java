package com.school.security.dtos.responses;

import java.util.List;

public record DashboardEvolutionResDto(
        List<DashboardEvolutionPointResDto> points) {}
