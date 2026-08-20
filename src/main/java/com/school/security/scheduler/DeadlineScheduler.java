package com.school.security.scheduler;

import com.school.security.entities.Project;
import com.school.security.entities.ProjectContributor;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.enums.NotificationType;
import com.school.security.repositories.NotificationRepository;
import com.school.security.repositories.ProjectContributorRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeadlineScheduler {

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private ProjectContributorRepository contributorRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkDeadlines() {
        checkProjectDeadlines();
        checkTaskDeadlines();
    }

    private void checkProjectDeadlines() {
        LocalDate today = LocalDate.now();
        List<Project> projects = projectRepository.findByIsActiveTrue();

        for (Project project : projects) {
            if (project.getEndDate() == null) continue;
            if (!project.getEndDate().isBefore(today.plusDays(1))) continue;

            Long projectId = project.getProjectId();
            String message = "Le projet \"" + project.getTitle() + "\" arrive à échéance le " + project.getEndDate();

            Long ownerId = project.getOwner().getUsersId();
            if (!notificationRepository.existsByUserUsersIdAndTypeAndProjectIdAndTaskId(
                    ownerId, NotificationType.PROJECT_DEADLINE, projectId, null)) {
                notificationRepository.save(
                        createNotification(ownerId, message, NotificationType.PROJECT_DEADLINE, projectId, null));
            }

            List<ProjectContributor> contributors = contributorRepository.findByProjectProjectId(projectId);
            for (ProjectContributor contributor : contributors) {
                Long userId = contributor.getUser().getUsersId();
                if (userId.equals(ownerId)) continue;
                if (!notificationRepository.existsByUserUsersIdAndTypeAndProjectIdAndTaskId(
                        userId, NotificationType.PROJECT_DEADLINE, projectId, null)) {
                    notificationRepository.save(
                            createNotification(userId, message, NotificationType.PROJECT_DEADLINE, projectId, null));
                }
            }
        }
    }

    private void checkTaskDeadlines() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findByIsActiveTrue();

        for (Task task : tasks) {
            if (task.getDueDate() == null) continue;
            if (!task.getDueDate().isBefore(today.plusDays(1))) continue;

            Long taskId = task.getTaskId();
            Long projectId = task.getProject().getProjectId();
            String message = "La tâche \"" + task.getTitle() + "\" arrive à échéance le " + task.getDueDate();

            for (User assignee : task.getAssignees()) {
                Long userId = assignee.getUsersId();
                if (!notificationRepository.existsByUserUsersIdAndTypeAndProjectIdAndTaskId(
                        userId, NotificationType.TASK_DEADLINE, projectId, taskId)) {
                    notificationRepository.save(
                            createNotification(userId, message, NotificationType.TASK_DEADLINE, projectId, taskId));
                }
            }
        }
    }

    private com.school.security.entities.Notification createNotification(
            Long userId, String message, NotificationType type, Long projectId, Long taskId) {
        com.school.security.entities.Notification notif = new com.school.security.entities.Notification();
        notif.setUser(userRepository.getReferenceById(userId));
        notif.setMessage(message);
        notif.setType(type);
        notif.setProjectId(projectId);
        notif.setTaskId(taskId);
        notif.setIsRead(false);
        return notif;
    }
}
