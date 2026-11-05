package com.school.security.services.implementations;

import com.school.security.common.PeriodUtils;
import com.school.security.dtos.responses.*;
import com.school.security.entities.*;
import com.school.security.enums.RoleType;
import com.school.security.repositories.*;
import com.school.security.services.contracts.ProjectReportService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation de l'agrégation de données du rapport de projet.
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>l'accès est autorisé pour un SUPER_ADMIN, un ADMIN, le propriétaire du
 *       projet ou un contributeur ; sinon {@code getProjectReport} renvoie
 *       {@code null} (c'est le contrôleur qui en déduit le statut HTTP) ;</li>
 *   <li>un ADMIN n'est PAS restreint à ses projets accessibles : il peut
 *       consulter le rapport de n'importe quel projet existant ;</li>
 *   <li>seules les tâches actives ({@code isActive = true}) sont comptabilisées ;</li>
 *   <li>le statut global et son libellé placés dans le DTO sont figés à
 *       "ON_TRACK" / "Sur la bonne voie" ; les valeurs calculées par
 *       {@link #calculateGlobalStatus} ne sont pas utilisées (code mort) ;</li>
 *   <li>plusieurs méthodes privées (activité, évolution, distributions,
 *       résolution de période, statistiques vides) sont déclarées mais jamais
 *       appelées dans ce service.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class ProjectReportServiceImpl implements ProjectReportService {

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ActivityRepository activityRepository;
    private DirectionRepository directionRepository;

    private static final String COMPLETED_STATUS = "Termine";
    private static final String IN_PROGRESS_STATUS = "En cours";
    private static final String TODO_STATUS = "A faire";

    /**
     * Construit le rapport d'un projet : contrôle d'accès, comptages par statut,
     * liste des contributeurs et table des tâches triée par statut puis
     * échéance. Renvoie {@code null} si l'utilisateur, le projet ou l'accès est
     * invalide.
     */
    @Override
    public ProjectReportResDto getProjectReport(Long userId, Long projectId) {
        var user = userRepository.findById(userId).orElse(null);
        var project = projectRepository.findById(projectId).orElse(null);

        if (user == null || project == null) {
            return null;
        }

        // Check access
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleType.SUPER_ADMIN.name()));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleType.ADMIN.name()));
        boolean isOwner = project.getOwner().getUsersId().equals(userId);
        boolean isContributor = project.getContributors().stream()
                .anyMatch(c -> c.getUser().getUsersId().equals(userId));

        if (!isSuperAdmin && !isAdmin && !isOwner && !isContributor) {
            return null;
        }

        // Get all tasks for this project
        List<Task> allTasks = taskRepository.findByProjectProjectIdAndIsActiveTrue(projectId);

        // Calculate task counts
        int totalTasks = allTasks.size();
        int completedTasks = (int) allTasks.stream()
                .filter(t -> COMPLETED_STATUS.equals(t.getStatus().getName()))
                .count();
        int inProgressTasks = (int) allTasks.stream()
                .filter(t -> IN_PROGRESS_STATUS.equals(t.getStatus().getName()))
                .count();
        int todoTasks = (int) allTasks.stream()
                .filter(t -> TODO_STATUS.equals(t.getStatus().getName()))
                .count();
        int overdueTasks = (int) allTasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(LocalDate.now())
                        && !COMPLETED_STATUS.equals(t.getStatus().getName()))
                .count();

        int totalTasksAll = allTasks.size();
        int progressPercent = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;

        // Global status
        String globalStatus = calculateGlobalStatus(progressPercent, overdueTasks);
        String globalStatusLabel = getGlobalStatusLabel(globalStatus);

        // Get contributors list
        List<String> contributors = project.getContributors().stream()
                .map(c -> c.getUser().getFirstname() + " " + c.getUser().getLastname())
                .collect(Collectors.toList());

        // Get tasks with assignees
        List<TaskReportResDto> tasks = allTasks.stream()
                .map(t -> new TaskReportResDto(
                        t.getTaskId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getStatus().getName(),
                        getStatusLabel(t.getStatus().getName()),
                        t.getAssignees().stream()
                                .map(u -> u.getFirstname() + " " + u.getLastname())
                                .collect(Collectors.joining(", ")),
                        t.getPriority() != null ? t.getPriority().getName() : "—",
                        t.getDueDate()
                ))
                .sorted(Comparator.comparing(TaskReportResDto::status)
                        .thenComparing(TaskReportResDto::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return new ProjectReportResDto(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getOwner().getFirstname() + " " + project.getOwner().getLastname(),
                project.getOwner().getEmail(),
                project.getDirection() != null ? project.getDirection().getName() : "—",
                project.getIsActive() ? "Actif" : "Archivé",
                totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0,
                totalTasks,
                completedTasks,
                inProgressTasks,
                overdueTasks,
                todoTasks,
                "ON_TRACK",
                "Sur la bonne voie",
                tasks,
                project.getOwner().getFirstname() + " " + project.getOwner().getLastname(),
                project.getOwner().getEmail(),
                LocalDateTime.now()
        );
    }

    /**
     * Statut global théorique : "DIFFICULTY" au-delà de 3 tâches en retard,
     * "ON_TRACK" à partir de 80 % de progression, "MONITORING" sinon. Non
     * utilisé par le DTO actuel.
     */
    private String calculateGlobalStatus(int progressPercent, int overdueTasks) {
        if (overdueTasks > 3) return "DIFFICULTY";
        if (progressPercent >= 80) return "ON_TRACK";
        if (progressPercent >= 50) return "MONITORING";
        return "MONITORING";
    }

    /** Libellé français d'un statut global ; "Inconnu" par défaut. */
    private String getGlobalStatusLabel(String status) {
        return switch (status) {
            case "ON_TRACK" -> "Sur la bonne voie";
            case "MONITORING" -> "Surveillance";
            case "DIFFICULTY" -> "En difficulté";
            default -> "Inconnu";
        };
    }

    /** Libellé d'affichage d'un statut (préfixé d'un pictogramme) ; statut inconnu retourné tel quel. */
    private String getStatusLabel(String status) {
        return switch (status) {
            case "Termine" -> "✅ Terminé";
            case "En cours" -> "▣ En cours";
            case "A faire" -> "□ À faire";
            default -> status;
        };
    }

    /** Activités récentes de la période, plafonnées à 20 (non appelée actuellement). */
    private List<RecentActivityResDto> getRecentActivity(List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
        List<Activity> activities = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);
        return activities.stream()
                .limit(20)
                .map(a -> new RecentActivityResDto(
                        a.getActivityId(),
                        a.getType().name(),
                        a.getDescription(),
                        a.getUser().getFirstname() + " " + a.getUser().getLastname(),
                        a.getProject() != null ? a.getProject().getTitle() : "—",
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Série créées/terminées, regroupée par jour si la période fait au plus
     * 31 jours, par mois sinon (non appelée actuellement).
     */
    private List<EvolutionPointResDto> computeEvolution(List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> createdDates = taskRepository.findCreatedDatesBetween(projectIds, start, end);
        List<LocalDateTime> completedDates = taskRepository.findCompletedDatesBetween(projectIds, start, end);

        Duration duration = Duration.between(start, end);
        long days = duration.toDays();

        // Determine grouping: by day if <= 31 days, by month otherwise
        boolean groupByDay = days <= 31;
        DateTimeFormatter formatter = groupByDay ? DateTimeFormatter.ofPattern("dd/MM") : DateTimeFormatter.ofPattern("MMM yyyy");

        Map<String, Long> createdMap = new LinkedHashMap<>();
        Map<String, Long> completedMap = new LinkedHashMap<>();

        // Initialize all periods with 0
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            String label = current.format(formatter);
            createdMap.put(label, 0L);
            completedMap.put(label, 0L);
            current = groupByDay ? current.plusDays(1) : current.plusMonths(1);
        }

        // Count created tasks per period
        for (LocalDateTime dt : createdDates) {
            String label = dt.format(formatter);
            createdMap.merge(label, 1L, Long::sum);
        }

        // Count completed tasks per period
        for (LocalDateTime dt : completedDates) {
            String label = dt.format(formatter);
            completedMap.merge(label, 1L, Long::sum);
        }

        List<EvolutionPointResDto> result = new ArrayList<>();
        for (String label : createdMap.keySet()) {
            result.add(new EvolutionPointResDto(
                    label,
                    createdMap.getOrDefault(label, 0L),
                    completedMap.getOrDefault(label, 0L)
            ));
        }

        return result;
    }

    /** Non implémenté et non appelé : renvoie toujours une liste vide. */
    private List<PriorityDistributionResDto> computePriorityDistribution(List<Long> projectIds) {
        // Return empty for now - would need priority distribution query
        return List.of();
    }

    /** Non implémenté et non appelé : renvoie toujours une liste vide. */
    private List<AssigneeWorkloadResDto> getAssigneeWorkload(List<Long> projectIds) {
        // Return empty for now
        return List.of();
    }

    /** Délègue à PeriodUtils (non appelée actuellement). */
    private LocalDateTime[] resolvePeriodDates(String period, String startDate, String endDate, LocalDate today) {
        return PeriodUtils.resolvePeriodDates(period, startDate, endDate, today);
    }

    /** Statistiques vides (non utilisées actuellement ; type de retour incohérent avec ce service). */
    private AdminDashboardStatsResDto emptyStats() {
        return new AdminDashboardStatsResDto(
                0, 0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }
}