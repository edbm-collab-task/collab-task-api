package com.school.security.controllers.api;

import com.school.security.dtos.responses.ActivityResDto;
import com.school.security.services.contracts.ActivityService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityResDto>> getActivities(@PathVariable Long projectId) {
        return ResponseEntity.ok(activityService.findByProjectId(projectId));
    }
}
