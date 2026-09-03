package com.school.security.services.contracts;

import com.school.security.enums.RoleType;

public interface DashboardReportService {
    byte[] generateReport(Long userId, RoleType role, String period, String startDate, String endDate, Long projectId);
}
