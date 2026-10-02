package com.school.security.services.contracts;

public interface DashboardReportService {
    byte[] generateReport(Long userId, String role, String period, String startDate, String endDate, Long projectId);
}
