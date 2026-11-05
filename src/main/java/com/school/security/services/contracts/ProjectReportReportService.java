package com.school.security.services.contracts;

import com.school.security.dtos.responses.ProjectReportResDto;
import com.school.security.enums.RoleType;

public interface ProjectReportReportService {
    byte[] generateReport(Long userId, String period, String startDate, String endDate);
    byte[] generateReport(Long userId, Long projectId, RoleType role, String period, String startDate, String endDate);
}