package com.school.security.services.contracts;

import com.school.security.dtos.responses.ActivityResDto;
import com.school.security.enums.ActivityType;
import java.util.List;

public interface ActivityService {
    List<ActivityResDto> findByProjectId(Long projectId);
    void logActivity(Long projectId, Long userId, ActivityType type, String description, Long taskId);
}
