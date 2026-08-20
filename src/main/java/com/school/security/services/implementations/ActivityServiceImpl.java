package com.school.security.services.implementations;

import com.school.security.dtos.responses.ActivityResDto;
import com.school.security.entities.Activity;
import com.school.security.entities.Project;
import com.school.security.entities.User;
import com.school.security.enums.ActivityType;
import com.school.security.repositories.ActivityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.ActivityService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private ActivityRepository activityRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;

    @Override
    public List<ActivityResDto> findByProjectId(Long projectId) {
        return activityRepository.findByProjectProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void logActivity(Long projectId, Long userId, ActivityType type, String description, Long taskId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (project == null || user == null) return;

        Activity activity = new Activity();
        activity.setProject(project);
        activity.setUser(user);
        activity.setType(type);
        activity.setDescription(description);
        activity.setTaskId(taskId);
        activityRepository.save(activity);
    }

    private ActivityResDto toDto(Activity entity) {
        return new ActivityResDto(
                entity.getActivityId(),
                entity.getProject().getProjectId(),
                entity.getProject().getTitle(),
                entity.getUser().getUsersId(),
                entity.getUser().getFirstname() + " " + entity.getUser().getLastname(),
                entity.getType(),
                entity.getDescription(),
                entity.getTaskId(),
                entity.getCreatedAt()
        );
    }
}
