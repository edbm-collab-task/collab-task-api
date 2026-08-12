package com.school.security.mappers;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Task;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper implements Mapper<TaskReqDto, Task, TaskResDto> {

    private final ProjectRepository projectRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final TaskRepository taskRepository;

    public TaskMapper(
            ProjectRepository projectRepository,
            PriorityRepository priorityRepository,
            StatusRepository statusRepository,
            TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.taskRepository = taskRepository;
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
        return task;
    }

    @Override
    public TaskResDto toDto(Task entity) {
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
                entity.getParent() != null ? entity.getParent().getTaskId() : null);
    }
}
