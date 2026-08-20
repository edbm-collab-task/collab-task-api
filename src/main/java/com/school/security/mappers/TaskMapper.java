package com.school.security.mappers;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper implements Mapper<TaskReqDto, Task, TaskResDto> {

    private final ProjectRepository projectRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskMapper(
            ProjectRepository projectRepository,
            PriorityRepository priorityRepository,
            StatusRepository statusRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Task fromDto(TaskReqDto d) {
        Task task = new Task();
        task.setTitle(d.title());
        task.setDescription(d.description());
        task.setDueDate(d.dueDate());
        task.setProject(projectRepository.getReferenceById(d.projectId()));
        task.setPriority(priorityRepository.getReferenceById(d.priorityId()));
        task.setStatus(statusRepository.getReferenceById(d.statusId()));
        if (d.parentTaskId() != null) {
            task.setParent(taskRepository.getReferenceById(d.parentTaskId()));
        }
        if (d.assigneeIds() != null && !d.assigneeIds().isEmpty()) {
            List<User> assignees = new ArrayList<>();
            for (Long userId : d.assigneeIds()) {
                userRepository.findById(userId).ifPresent(assignees::add);
            }
            task.setAssignees(assignees);
        }
        return task;
    }

    @Override
    public TaskResDto toDto(Task entity) {
        List<TaskResDto.AssigneeResDto> assignees = Collections.emptyList();
        if (entity.getAssignees() != null && !entity.getAssignees().isEmpty()) {
            assignees = entity.getAssignees().stream()
                    .map(u -> new TaskResDto.AssigneeResDto(
                            u.getUsersId(),
                            u.getFirstname(),
                            u.getLastname(),
                            u.getEmail(),
                            u.getImagePath()))
                    .collect(Collectors.toList());
        }
        return new TaskResDto(
                entity.getTaskId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDueDate(),
                entity.getIsActive(),
                entity.getProject().getProjectId(),
                entity.getProject().getTitle(),
                entity.getPriority().getPriorityId(),
                entity.getPriority().getName(),
                entity.getStatus().getStatusId(),
                entity.getStatus().getName(),
                entity.getParent() != null ? entity.getParent().getTaskId() : null,
                assignees);
    }
}
