package com.school.security.services.implementations;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Priority;
import com.school.security.entities.Project;
import com.school.security.entities.Status;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.enums.NotificationType;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.TaskMapper;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.NotificationService;
import com.school.security.services.contracts.TaskService;
import java.util.ArrayList;
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
    private UserRepository userRepository;
    private TaskMapper taskMapper;
    private NotificationService notificationService;
    private com.school.security.services.contracts.ActivityService activityService;
    private ProjectRepository projectRepository;

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

                Long oldPriorityId = taskToUpdate.getPriority().getPriorityId();
                List<Long> oldAssigneeIds = taskToUpdate.getAssignees().stream()
                        .map(User::getUsersId).collect(Collectors.toList());

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
                updateAssignees(taskToUpdate, toSave.assigneeIds());
                Task saved = this.taskRepository.save(taskToUpdate);

                autoUnarchiveProject(saved.getProject());

                Long currentUserId = getCurrentUserId();
                Long projId = saved.getProject().getProjectId();

                activityService.logActivity(projId, currentUserId,
                        com.school.security.enums.ActivityType.TASK_UPDATED,
                        "La tâche \"" + saved.getTitle() + "\" a été mise à jour", saved.getTaskId());

                if (toSave.assigneeIds() != null) {
                    for (Long userId : toSave.assigneeIds()) {
                        if (!oldAssigneeIds.contains(userId)) {
                            notifyAssignee(userId, saved, "Vous avez été assigné(e) à la tâche");
                            activityService.logActivity(projId, userId,
                                    com.school.security.enums.ActivityType.TASK_ASSIGNED,
                                    "Assigné(e) à la tâche \"" + saved.getTitle() + "\"", saved.getTaskId());
                        }
                    }
                }

                if (!oldPriorityId.equals(toSave.priorityId())) {
                    notifyPriorityChange(saved, oldPriorityId);
                    activityService.logActivity(projId, currentUserId,
                            com.school.security.enums.ActivityType.TASK_PRIORITY_CHANGED,
                            "Priorité changée pour la tâche \"" + saved.getTitle() + "\"", saved.getTaskId());
                }

                return this.taskMapper.toDto(saved);
            }
        }
        Task taskToSave = this.taskMapper.fromDto(toSave);
        if (toSave.parentTaskId() != null) {
            checkParentRules(taskToSave, toSave.parentTaskId());
        }
        Task saved = this.taskRepository.save(taskToSave);

        autoUnarchiveProject(saved.getProject());

        Long currentUserId = getCurrentUserId();
        activityService.logActivity(saved.getProject().getProjectId(), currentUserId,
                com.school.security.enums.ActivityType.TASK_CREATED,
                "La tâche \"" + saved.getTitle() + "\" a été créée", saved.getTaskId());

        if (toSave.assigneeIds() != null) {
            for (Long userId : toSave.assigneeIds()) {
                notifyAssignee(userId, saved, "Vous avez été assigné(e) à la tâche");
                activityService.logActivity(saved.getProject().getProjectId(), userId,
                        com.school.security.enums.ActivityType.TASK_ASSIGNED,
                        "Assigné(e) à la tâche \"" + saved.getTitle() + "\"", saved.getTaskId());
            }
        }

        return this.taskMapper.toDto(saved);
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
            Long currentUserId = getCurrentUserId();
            activityService.logActivity(task.getProject().getProjectId(), currentUserId,
                    com.school.security.enums.ActivityType.TASK_DELETED,
                    "La tâche \"" + task.getTitle() + "\" a été supprimée", task.getTaskId());
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
                String oldStatus = task.getStatus().getName();
                task.setStatus(statusOptional.get());
                Long currentUserId = getCurrentUserId();
                activityService.logActivity(task.getProject().getProjectId(), currentUserId,
                        com.school.security.enums.ActivityType.TASK_STATUS_CHANGED,
                        "Statut changé de \"" + oldStatus + "\" à \"" + statusOptional.get().getName() + "\" pour la tâche \"" + task.getTitle() + "\"",
                        task.getTaskId());
                Task saved = this.taskRepository.save(task);
                autoUnarchiveProject(saved.getProject());
                return this.taskMapper.toDto(saved);
            }
            throw new EntityException("Status not found");
        }
        throw new EntityException("Task not found");
    }

    private Long getCurrentUserId() {
        try {
            String email = com.school.security.securities.utils.SecurityUtils.getCurrentUsername();
            return userRepository.findByEmail(email).map(User::getUsersId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyAssignee(Long userId, Task task, String message) {
        String notifMessage = message + " \"" + task.getTitle() + "\" dans le projet \"" + task.getProject().getTitle() + "\"";
        notificationService.createNotification(userId, notifMessage, NotificationType.TASK_ASSIGNED, task.getProject().getProjectId(), task.getTaskId());
    }

    private void notifyPriorityChange(Task task, Long oldPriorityId) {
        Priority newPriority = task.getPriority();
        Priority oldPriority = priorityRepository.findById(oldPriorityId).orElse(null);
        if (oldPriority == null) return;

        String notifMessage = "La priorité de la tâche \"" + task.getTitle() + "\" est passée de " + oldPriority.getName() + " à " + newPriority.getName();

        for (User assignee : task.getAssignees()) {
            notificationService.createNotification(
                    assignee.getUsersId(),
                    notifMessage,
                    NotificationType.PRIORITY_CHANGED,
                    task.getProject().getProjectId(),
                    task.getTaskId()
            );
        }
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

    private void updateAssignees(Task task, List<Long> assigneeIds) {
        if (assigneeIds == null) {
            return;
        }
        List<User> assignees = new ArrayList<>();
        for (Long userId : assigneeIds) {
            this.userRepository.findById(userId).ifPresent(assignees::add);
        }
        task.setAssignees(assignees);
    }

    private void autoUnarchiveProject(Project project) {
        if (Boolean.FALSE.equals(project.getIsActive())) {
            project.setIsActive(true);
            projectRepository.save(project);
        }
    }
}
