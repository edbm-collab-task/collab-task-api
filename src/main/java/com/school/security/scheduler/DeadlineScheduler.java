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

/**
 * Tâche planifiée de vérification quotidienne des échéances (projets et
 * tâches) et de génération des notifications correspondantes.
 *
 * <p>Comportement général constaté (documenté, non modifié) :
 * <ul>
 *   <li>déclenchement par cron {@code "0 0 9 * * *"}, soit tous les jours à
 *       09:00 (heure du serveur) ;</li>
 *   <li>seuls les projets et tâches ACTIFS sont examinés
 *       ({@code findByIsActiveTrue()}) ;</li>
 *   <li>une échéance est considérée comme atteinte lorsque la date est
 *       AUJOURD'HUI ou déjà passée (voir la condition de date dans les
 *       méthodes) ; les entités sans date de fin sont ignorées ;</li>
 *   <li>les notifications sont dédupliquées par l'existence d'une ligne
 *       {@code (utilisateur, type, projectId, taskId)} : une notification
 *       d'échéance n'est donc créée qu'UNE fois par cible, même si
 *       l'échéance reste dépassée les jours suivants ;</li>
 *   <li>aucun filtre sur le statut des tâches : une tâche déjà terminée mais
 *       toujours active et dont l'échéance est dépassée déclenche une
 *       alerte ;</li>
 *   <li>ce composant n'est pas transactionnel ; {@code Task.assignees} étant
 *       une collection paresseuse (LAZY), son accès s'effectue hors
 *       transaction explicite.</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class DeadlineScheduler {

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private ProjectContributorRepository contributorRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;

    /**
     * Point d'entrée planifié : exécute successivement la vérification des
     * échéances de projets puis celles des tâches, chaque jour à 09:00.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkDeadlines() {
        checkProjectDeadlines();
        checkTaskDeadlines();
    }

    /**
     * Alerte les parties prenantes des projets dont l'échéance est atteinte
     * (date de fin aujourd'hui ou passée).
     *
     * <p>Destinataires : le propriétaire ({@code project.getOwner()}) puis
     * chaque contributeur du projet, le propriétaire étant explicitement
     * ignoré dans la boucle des contributeurs pour éviter un doublon.
     *
     * <p>Déduplication : la clé est
     * {@code (utilisateur, PROJECT_DEADLINE, projectId, null)} (pas de
     * {@code taskId} pour une alerte projet). Aucune notification n'est
     * supprimée si l'échéance change ou est repoussée.
     */
    private void checkProjectDeadlines() {
        LocalDate today = LocalDate.now();
        List<Project> projects = projectRepository.findByIsActiveTrue();

        for (Project project : projects) {
            // Projet sans date de fin : rien à surveiller.
            if (project.getEndDate() == null) continue;
            // On ne traite que si endDate est strictement antérieure à
            // demain, c'est-à-dire l'échéance atteinte aujourd'hui ou avant.
            if (!project.getEndDate().isBefore(today.plusDays(1))) continue;

            Long projectId = project.getProjectId();
            String message = "Le projet \"" + project.getTitle() + "\" arrive à échéance le " + project.getEndDate();

            // Propriétaire d'abord.
            Long ownerId = project.getOwner().getUsersId();
            if (!notificationRepository.existsByUserUsersIdAndTypeAndProjectIdAndTaskId(
                    ownerId, NotificationType.PROJECT_DEADLINE, projectId, null)) {
                notificationRepository.save(
                        createNotification(ownerId, message, NotificationType.PROJECT_DEADLINE, projectId, null));
            }

            // Puis chaque contributeur, en excluant le propriétaire déjà
            // notifié ci-dessus.
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

    /**
     * Alerte les assignés des tâches dont l'échéance est atteinte (date
     * d'échéance aujourd'hui ou passée).
     *
     * <p>Destinataires : tous les {@code task.getAssignees()} (aucune
     * exclusion). Déduplication par
     * {@code (utilisateur, TASK_DEADLINE, projectId, taskId)} : une alerte
     * distincte est donc possible par tâche. Aucun filtre sur le statut ni
     * sur l'activité de l'assigné.
     */
    private void checkTaskDeadlines() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findByIsActiveTrue();

        for (Task task : tasks) {
            // Tâche sans échéance : rien à surveiller.
            if (task.getDueDate() == null) continue;
            // Échéance atteinte = dueDate strictement antérieure à demain.
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

    /**
     * Construit une notification non lue pour un utilisateur.
     *
     * <p>L'utilisateur est référencé via {@code getReferenceById} (proxy
     * paresseux, sans requête d'existence) ; {@code projectId} et
     * {@code taskId} sont stockés comme identifiants simples (et non comme
     * associations). {@code isRead} est initialisé à {@code false}.
     */
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
