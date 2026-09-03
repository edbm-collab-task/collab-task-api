package com.school.security.services.contracts;

import com.school.security.enums.RoleType;

public interface AdminDashboardReportService {
    byte[] generateReport(Long userId, RoleType role, String period, String startDate, String endDate);
    byte[] generateReport(Long userId, String period, String startDate, String endDate);
}