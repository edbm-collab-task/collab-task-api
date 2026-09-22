package com.school.security.services.contracts;

public interface AdminDashboardReportService {
    byte[] generateReport(Long userId, String role, String period, String startDate, String endDate);
    byte[] generateReport(Long userId, String period, String startDate, String endDate);
}