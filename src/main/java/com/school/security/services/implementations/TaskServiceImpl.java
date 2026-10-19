package com.school.security.services.implementations;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Priority;
import com.school.security.entities.Project;
import com.school.security.entities.Status;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.enums.NotificationType;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.TaskMapper;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskCommentRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.NotificationService;
import com.school.security.services.contracts.TaskService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère les règles métier liées aux tâches.
 *
 * <p>Une tâche est archivée ({@code isActive = false}) plutôt que physiquement
 * supprimée, afin de préserver l'historique et la traçabilité. Le passage au
 * statut "Termine" (nom exact en base, sans accent) fige la date de complétion
 * ({@code completedAt}). Travailler une tâche d'un projet archivé réactive
 * automatiquement le projet. Chaque création, mise à jour, assignation,
 * changement de statut ou de priorité est journalisé dans le fil d'activité du
 * projet et les assignés concernés sont notifiés.
 */
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
    private TaskCommentRepository taskCommentRepository;

    @Override
    public TaskResDto createOrUpdate(TaskReqDto toSave) {
        return save(toSave, null);
    }

    /**
     * Crée ou met à jour une tâche.
     *
     * <p>La mise à jour compare l'état avec la version précédente : seule la
     * complétion (statut "Termine") renseigne {@code completedAt}, et seuls les
     * nouveaux assignés ou un changement de priorité déclenchent notification et
     * activité. Un projet archivé est automatiquement réactivé.
     */
    @Override
    public TaskResDto save(TaskReqDto toSave, Long id) {
        if (id != null) {
            Optional<Task> taskOptional = this.taskRepository.findById(id);
            if (taskOptional.isPresent()) {
                Task taskToUpdate = taskOptional.get();

                validateDueDate(toSave.dueDate(), taskToUpdate.getDueDate());

                // Règle métier : l'échéance de la tâche ne peut pas dépasser la
                // date de fin (deadline) du projet auquel elle appartient.
                validateDueDateAgainstProjectDeadline(
                        toSave.dueDate(),
                        taskToUpdate.getProject().getEndDate()
                );

                Long oldPriorityId = taskToUpdate.getPriority().getPriorityId();
                List<Long> oldAssigneeIds = taskToUpdate.getAssignees().stream()
                        .map(User::getUsersId).collect(Collectors.toList());

                taskToUpdate.setTitle(toSave.title());
                taskToUpdate.setDescription(toSave.description());
                taskToUpdate.setDueDate(toSave.dueDate());
                taskToUpdate.setPriority(this.priorityRepository.getReferenceById(toSave.priorityId()));
                Status newStatus = this.statusRepository.getReferenceById(toSave.statusId());
                taskToUpdate.setStatus(newStatus);

                // Statut "Termine" (sans accent, tel que seedé en base) : fige la
                // date de complétion à la première occurrence ; quitter ce statut
                // la réinitialise.
                if ("Termine".equals(newStatus.getName()) && taskToUpdate.getCompletedAt() == null) {
                    taskToUpdate.setCompletedAt(LocalDateTime.now());
                } else if (!"Termine".equals(newStatus.getName())) {
                    taskToUpdate.setCompletedAt(null);
                }

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

                return this.taskMapper.toDto(saved, this.taskCommentRepository.countByTaskTaskId(saved.getTaskId()));
            }
        }
        validateDueDate(toSave.dueDate(), null);
        Task taskToSave = this.taskMapper.fromDto(toSave);

        // Règle métier : l'échéance de la tâche ne peut pas dépasser la date de
        // fin (deadline) du projet auquel elle appartient.
        validateDueDateAgainstProjectDeadline(
                toSave.dueDate(),
                taskToSave.getProject().getEndDate()
        );
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

        return this.taskMapper.toDto(saved, this.taskCommentRepository.countByTaskTaskId(saved.getTaskId()));
    }

    @Override
    public List<TaskResDto> findAll() {
        List<Task> tasks = this.taskRepository.findByIsActiveTrue();
        Map<Long, Long> counts = commentCountMap(tasks);
        return tasks.stream()
                .map(task -> this.taskMapper.toDto(task, counts.getOrDefault(task.getTaskId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResDto> findByProject(Long projectId) {
        List<Task> tasks = this.taskRepository.findByProjectProjectIdAndIsActiveTrue(projectId);
        Map<Long, Long> counts = commentCountMap(tasks);
        return tasks.stream()
                .map(task -> this.taskMapper.toDto(task, counts.getOrDefault(task.getTaskId(), 0L)))
                .collect(Collectors.toList());
    }

    public List<TaskResDto> findTasksByProjectAndStatusOrderBySortOrder(Long projectId, Long statusId) {
        List<Task> tasks = this.taskRepository.findByProjectProjectIdAndStatusIdOrderBySortOrderAsc(projectId, statusId);
        Map<Long, Long> counts = commentCountMap(tasks);
        return tasks.stream()
                .map(task -> this.taskMapper.toDto(task, counts.getOrDefault(task.getTaskId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public TaskResDto findById(Long id) {
        Optional<Task> taskOptional = this.taskRepository.findById(id);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            return this.taskMapper.toDto(task, this.taskCommentRepository.countByTaskTaskId(task.getTaskId()));
        }
        throw new EntityException("Task not found");
    }

    @Override
    public TaskResDto deleteById(Long id) {
        return archiver(id);
    }

    /**
     * Archive une tâche (soft delete) au lieu de la supprimer physiquement, afin
     * de conserver l'historique. L'opération journalise une activité de type
     * {@code TASK_DELETED} malgré le nom : il s'agit bien d'une suppression
     * logique, la tâche reste en base avec {@code isActive = false}.
     */
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
            Task saved = this.taskRepository.save(task);
            return this.taskMapper.toDto(saved, this.taskCommentRepository.countByTaskTaskId(saved.getTaskId()));
        }
        throw new EntityException("Task not found");
    }

    /**
     * Change le statut d'une tâche et journalise l'activité correspondante avec
     * l'ancien et le nouveau statut. Applique la même règle de complétion que la
     * mise à jour (statut "Termine" fige {@code completedAt}).
     */
    @Override
    public TaskResDto changerStatut(Long taskId, Long statusId) {
        Optional<Task> taskOptional = this.taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Optional<Status> statusOptional = this.statusRepository.findById(statusId);
            if (statusOptional.isPresent()) {
                Task task = taskOptional.get();
                Status oldStatus = task.getStatus();
                String oldStatusName = oldStatus.getName();
                Status newStatus = statusOptional.get();
                task.setStatus(newStatus);

                // Même règle que lors d'une mise à jour : "Termine" fige la date
                // de complétion, tout autre statut la réinitialise.
                if ("Termine".equals(newStatus.getName()) && task.getCompletedAt() == null) {
                    task.setCompletedAt(LocalDateTime.now());
                } else if (!"Termine".equals(newStatus.getName())) {
                    task.setCompletedAt(null);
                }

                Task saved = this.taskRepository.save(task);
                autoUnarchiveProject(saved.getProject());

                // Journalise uniquement si le statut change réellement
                // (comparaison par id : plusieurs statuts partagent le nom "OK").
                if (!oldStatus.getStatusId().equals(newStatus.getStatusId())) {
                    Long currentUserId = getCurrentUserId();
                    activityService.logActivity(saved.getProject().getProjectId(), currentUserId,
                            com.school.security.enums.ActivityType.TASK_STATUS_CHANGED,
                            buildStatusChangedMessage(oldStatusName, newStatus.getName(), saved.getTitle()),
                            saved.getTaskId());
                }
                return this.taskMapper.toDto(saved, this.taskCommentRepository.countByTaskTaskId(saved.getTaskId()));
            }
            throw new EntityException("Status not found");
        }
        throw new EntityException("Task not found");
    }

    @Override
    public TaskResDto updateTaskSortOrder(Long taskId, Integer sortOrder) {
        Optional<Task> taskOptional = this.taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            Task task = taskOptional.get();
            task.setSortOrder(sortOrder);
            Task saved = this.taskRepository.save(task);
            return this.taskMapper.toDto(saved, this.taskCommentRepository.countByTaskTaskId(saved.getTaskId()));
        }
        throw new EntityException("Task not found");
    }

    /**
     * Déplace une tâche (drag & drop) dans une colonne ou une priorité.
     *
     * <p>Le statut cible est appliqué (avec la même règle de complétion que
     * {@link #changerStatut}). Si une carte de référence {@code beforeTaskId}
     * est fournie, la tâche adopte la priorité de cette carte et s'insère juste
     * avant elle dans son groupe (statut + priorité) ; sinon elle garde sa
     * priorité et rejoint la fin de ce groupe dans le nouveau statut. Le
     * {@code sortOrder} du groupe cible puis de l'ancien groupe est renuméroté
     * de 1 à N. Les changements réels de statut et/ou de priorité sont
     * journalisés dans le fil d'activité.
     */
    @Override
    public TaskResDto moveTask(Long taskId, Long statusId, Long beforeTaskId) {
        Optional<Task> taskOptional = this.taskRepository.findById(taskId);
        if (taskOptional.isEmpty()) {
            throw new EntityException("Task not found");
        }
        Task task = taskOptional.get();

        Optional<Status> statusOptional = this.statusRepository.findById(statusId);
        if (statusOptional.isEmpty()) {
            throw new EntityException("Status not found");
        }
        Status newStatus = statusOptional.get();

        Priority oldPriority = task.getPriority();
        Status oldStatus = task.getStatus();
        String oldStatusName = oldStatus.getName();

        // Statut + règle de complétion identiques à changerStatut : "Termine"
        // fige la date de complétion, tout autre statut la réinitialise.
        task.setStatus(newStatus);
        if ("Termine".equals(newStatus.getName()) && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (!"Termine".equals(newStatus.getName())) {
            task.setCompletedAt(null);
        }

        // Priorité cible : celle de la carte de référence si fournie, sinon la
        // priorité actuelle de la tâche (déplacement simple de colonne).
        Priority targetPriority = oldPriority;
        if (beforeTaskId != null) {
            Optional<Task> beforeOptional = this.taskRepository.findById(beforeTaskId);
            if (beforeOptional.isEmpty()) {
                throw new EntityException("Before task not found");
            }
            targetPriority = beforeOptional.get().getPriority();
        }
        task.setPriority(targetPriority);

        // Groupe cible (statut + priorité), tâche déplacée exclue, trié par
        // sortOrder, puis insertion juste avant la carte de référence (ou en fin
        // de groupe si non fournie / absente du groupe) et renumérotation 1..N.
        List<Task> targetGroup = new ArrayList<>(this.taskRepository
                .findByIsActiveTrueAndStatusStatusIdAndPriorityPriorityId(
                        newStatus.getStatusId(), targetPriority.getPriorityId()));
        targetGroup.removeIf(other -> other.getTaskId().equals(taskId));
        targetGroup.sort(Comparator.comparing(Task::getSortOrder,
                Comparator.nullsLast(Comparator.naturalOrder())));

        int insertIndex = targetGroup.size();
        if (beforeTaskId != null) {
            for (int i = 0; i < targetGroup.size(); i++) {
                if (targetGroup.get(i).getTaskId().equals(beforeTaskId)) {
                    insertIndex = i;
                    break;
                }
            }
        }
        targetGroup.add(insertIndex, task);
        for (int i = 0; i < targetGroup.size(); i++) {
            targetGroup.get(i).setSortOrder(i + 1);
        }
        this.taskRepository.saveAll(targetGroup);

        // Ancien groupe (statut + priorité de départ) si différent du groupe
        // cible : renumérotation des tâches restantes pour combler la trouée.
        if (!oldStatus.getStatusId().equals(newStatus.getStatusId())
                || !Objects.equals(oldPriority.getPriorityId(), targetPriority.getPriorityId())) {
            List<Task> oldGroup = new ArrayList<>(this.taskRepository
                    .findByIsActiveTrueAndStatusStatusIdAndPriorityPriorityId(
                            oldStatus.getStatusId(), oldPriority.getPriorityId()));
            oldGroup.removeIf(other -> other.getTaskId().equals(taskId));
            oldGroup.sort(Comparator.comparing(Task::getSortOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (int i = 0; i < oldGroup.size(); i++) {
                oldGroup.get(i).setSortOrder(i + 1);
            }
            this.taskRepository.saveAll(oldGroup);
        }

        // Journalise un changement de statut et/ou de priorité selon ce qui a
        // réellement changé (comparaison par id) ; un simple réordonnancement au
        // sein d'un même groupe n'est pas une activité.
        boolean statusChanged = !oldStatus.getStatusId().equals(newStatus.getStatusId());
        boolean priorityChanged = !Objects.equals(oldPriority.getPriorityId(), targetPriority.getPriorityId());
        if (statusChanged) {
            Long currentUserId = getCurrentUserId();
            activityService.logActivity(task.getProject().getProjectId(), currentUserId,
                    com.school.security.enums.ActivityType.TASK_STATUS_CHANGED,
                    buildStatusChangedMessage(oldStatusName, newStatus.getName(), task.getTitle()),
                    task.getTaskId());
        }
        if (priorityChanged) {
            Long currentUserId = getCurrentUserId();
            activityService.logActivity(task.getProject().getProjectId(), currentUserId,
                    com.school.security.enums.ActivityType.TASK_PRIORITY_CHANGED,
                    "Priorité changée de \"" + oldPriority.getName() + "\" à \"" + targetPriority.getName()
                            + "\" pour la tâche \"" + task.getTitle() + "\"",
                    task.getTaskId());
            notifyPriorityChange(task, oldPriority.getPriorityId());
        }

        return this.taskMapper.toDto(task, this.taskCommentRepository.countByTaskTaskId(task.getTaskId()));
    }

    /**
     * Message d'activité pour un changement de statut. Si l'ancien et le
     * nouveau statut partagent le même nom (plusieurs statuts "OK" en base),
     * l'aller-retour "X à X" est omis pour éviter un libellé trompeur.
     */
    private String buildStatusChangedMessage(String oldName, String newName, String title) {
        if (Objects.equals(oldName, newName)) {
            return "Statut changé pour la tâche \"" + title + "\"";
        }
        return "Statut changé de \"" + oldName + "\" à \"" + newName + "\" pour la tâche \"" + title + "\"";
    }

    private Long getCurrentUserId() {
        try {
            String email = com.school.security.securities.utils.SecurityUtils.getCurrentUsername();
            return userRepository.findByEmail(email).map(User::getUsersId).orElse(null);
        } catch (Exception e) {
            // Requête non authentifiée : retourne null afin de continuer le
            // traitement, l'activité sera alors journalisée sans utilisateur.
            return null;
        }
    }

    /**
     * Compte les commentaires (racines + réponses) de toutes les tâches de la
     * liste en UNE seule requête GROUP BY, puis retourne une map
     * {@code taskId -> count}. Les tâches sans commentaire sont simplement
     * absentes de la map (valeur par défaut {@code 0} chez l'appelant).
     */
    private Map<Long, Long> commentCountMap(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> taskIds = tasks.stream().map(Task::getTaskId).collect(Collectors.toList());
        return this.taskCommentRepository.countByTaskIds(taskIds).stream()
                .collect(Collectors.toMap(
                        TaskCommentRepository.TaskCommentCount::getTaskId,
                        TaskCommentRepository.TaskCommentCount::getCnt));
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

        // Notifie chacun des assignés actuels de la tâche sur un changement de
        // priorité (pas uniquement le nouvel assigné).
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

    /**
     * Contraintes de hiérarchie d'une tâche : elle ne peut pas être son propre
     * parent et son parent doit exister et appartenir au même projet, afin de
     * garantir la cohérence de l'arbre de tâches à l'intérieur d'un projet.
     */
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

    /**
     * Remplace intégralement la liste des assignés de la tâche (il ne s'agit pas
     * d'un ajout incrémental). Les utilisateurs sont résolus individuellement ;
     * un identifiant inconnu est simplement ignoré.
     *
     * <p>Le comportement actuel ne vérifie pas explicitement que les utilisateurs
     * ajoutés sont membres ou contributeurs du projet auquel la tâche est
     * associée.
     */
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

    /**
     * Réactive automatiquement un projet archivé dès qu'une tâche y est créée,
     * modifiée ou voit son statut changer : un projet est considéré comme actif
     * dès qu'on y travaille, il ne doit pas rester désactivé avec des tâches
     * manipulées.
     */
    private void autoUnarchiveProject(Project project) {
        if (Boolean.FALSE.equals(project.getIsActive())) {
            project.setIsActive(true);
            projectRepository.save(project);
        }
    }

    /**
     * Contrôle la date d'échéance : elle est obligatoire et ne peut pas être
     * passée, sauf si elle n'a pas été modifiée depuis l'enregistrement précédent
     * (ce qui permet de mettre à jour une tâche existante sans être bloqué par
     * une échéance déjà écoulée).
     */
    private void validateDueDate(LocalDate dueDate, LocalDate originalDueDate) {
        if (dueDate == null) {
            throw new BadRequestException(
                    "La date d'échéance est obligatoire."
            );
        }
        boolean dueDateChanged = originalDueDate == null || !originalDueDate.equals(dueDate);
        if (dueDateChanged && dueDate.isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "La date d'échéance ne peut pas être dans le passé."
            );
        }
    }

    /**
     * Règle métier : l'échéance d'une tâche ne peut pas dépasser la date de fin
     * (deadline) de son projet. Aucune contrainte n'est appliquée si la tâche
     * n'a pas d'échéance ou si le projet n'a pas de date de fin.
     */
    private void validateDueDateAgainstProjectDeadline(LocalDate dueDate, LocalDate projectEndDate) {
        if (dueDate == null || projectEndDate == null) {
            return;
        }
        if (dueDate.isAfter(projectEndDate)) {
            throw new BadRequestException(
                    "La date d'échéance de la tâche ne peut pas dépasser la date de fin du projet."
            );
        }
    }
}
