package com.school.security.services.contracts;

import com.school.security.dtos.responses.AdminDashboardStatsResDto;

import java.util.List;

public interface AdminDashboardService {
    AdminDashboardStatsResDto getAdminDashboardStats(Long userId, String period, String startDate, String endDate);
}