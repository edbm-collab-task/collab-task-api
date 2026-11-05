package com.school.security.services.contracts;

import com.school.security.dtos.responses.DashboardDataResDto;

public interface DashboardService {
    DashboardDataResDto getDashboardStats(Long userId, String period, String startDate, String endDate, Long projectId);
}
