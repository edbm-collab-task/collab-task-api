package com.school.security.services.contracts;

import com.school.security.dtos.responses.ProjectReportResDto;

import java.util.List;

public interface ProjectReportService {
    ProjectReportResDto getProjectReport(Long userId, Long projectId);
}