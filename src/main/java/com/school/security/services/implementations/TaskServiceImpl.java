package com.school.security.services.implementations;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Status;
import com.school.security.entities.Task;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.TaskMapper;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.services.contracts.TaskService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private TaskRepository taskRepository;
    private PriorityRepository priorityRepository;
    private StatusRepository statusRepository;
    private TaskMapper taskMapper;

    @Override
    public TaskResDto createOrUpdate(TaskReqDto toSave) {
        return save(toSave, null);
    }

    @Override
    public TaskResDto save(TaskReqDto toSave, Long id) {
        if (id != null) {
            Optional<Task> taskOptional = this.taskRepository.findById(id);
            if (taskOptional.isPresent()) {
                Task taskToUpdate = taskOptional.get();
                taskToUpdate.setTitle(toSave.title());
                taskToUpdate.setDescription(toSave.description());
                taskToUpdate.setDueDate(toSave.dueDate());
                taskToUpdate.setPriority(this.priorityRepository.getReferenceById(toSave.priorityId()));
                taskToUpdate.setStatus(this.statusRepository.getReferenceById(toSave.statusId()));
                if (toSave.parentTaskId() != null) {
                    checkParentRules(taskToUpdate, toSave.parentTaskId());
                    taskToUpdate.setParent(this.taskRepository.getReferenceById(toSave.parentTaskId()));
                } else {
                    taskToUpdate.setParent(null);
                }
                return this.taskMapper.toDto(this.taskRepository.save(taskToUpdate));
            }
        }
        Task taskToSave = this.taskMapper.fromDto(toSave);
        if (toSave.parentTaskId() != null) {
            checkParentRules(taskToSave, toSave.parentTaskId());
        }
        return this.taskMapper.toDto(this.taskRepository.save(taskToSave));
    }

    @Override
    public List<TaskResDto> findAll() {
        return this.taskRepository.findByIsActiveTrue().stream()
                .map(this.taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResDto> findByProject(Long projectId) {
        return this.taskRepository.findByProjectProjectIdAndIsActiveTrue(projectId).stream()
                .map(this.taskMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskResDto findById(Long id) {
        Optional<Task> taskOptional = this.taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            return this.taskMapper.toDto(taskOptional.get());
        }
        throw new EntityException("Task not found");
    }

    @Override
    public TaskResDto deleteById(Long id) {
        return archiver(id);
    }

    @Override
    public TaskResDto archiver(Long id) {
        Optional<Task> taskOptional = this.taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setIsActive(false);
            return this.taskMapper.toDto(this.taskRepository.save(task));
        }
        throw new EntityException("Task not found");
    }

    @Override
    public TaskResDto changerStatut(Long taskId, Long statusId) {
        Optional<Task> taskOptional = this.taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Optional<Status> statusOptional = this.statusRepository.findById(statusId);
            if (statusOptional.isPresent()) {
                Task task = taskOptional.get();
                task.setStatus(statusOptional.get());
                return this.taskMapper.toDto(this.taskRepository.save(task));
            }
            throw new EntityException("Status not found");
        }
        throw new EntityException("Task not found");
    }

    private void checkParentRules(Task task, Long parentTaskId) {
        if (parentTaskId.equals(task.getTaskId())) {
            throw new EntityException("A task cannot be its own parent");
        }
        Optional<Task> parentOptional = this.taskRepository.findById(parentTaskId);
        if (parentOptional.isPresent()) {
            if (!parentOptional
                    .get()
                    .getProject()
                    .getProjectId()
                    .equals(task.getProject().getProjectId())) {
                throw new EntityException("Parent task must belong to the same project");
            }
        } else {
            throw new EntityException("Parent task not found");
        }
    }
}
