package com.school.security.services.implementations;

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

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class ProjectReportServiceImpl implements ProjectReportService {

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ActivityRepository activityRepository;
    private DirectionRepository directionRepository;
    private MilestoneRepository milestoneRepository;
    private RiskRepository riskRepository;
    private ActionItemRepository actionItemRepository;
    private BudgetItemRepository budgetItemRepository;

    private static final String COMPLETED_STATUS = "Termine";
    private static final String IN_PROGRESS_STATUS = "En cours";
    private static final String TODO_STATUS = "A faire";

    @Override
    public ProjectReportResDto getProjectReport(Long userId, Long projectId) {
        var user = userRepository.findById(userId).orElse(null);
        var project = projectRepository.findById(projectId).orElse(null);

        if (user == null || project == null) {
            return null;
        }

        // Check access
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.SUPER_ADMIN);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.ADMIN);
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
                getMilestones(projectId),
                getBudgetItems(project.getProjectId()),
                getRisks(projectId),
                getActionItems(projectId),
                project.getOwner().getFirstname() + " " + project.getOwner().getLastname(),
                project.getOwner().getEmail(),
                LocalDateTime.now()
        );
    }

    private String calculateGlobalStatus(int progressPercent, int overdueTasks) {
        if (overdueTasks > 3) return "DIFFICULTY";
        if (progressPercent >= 80) return "ON_TRACK";
        if (progressPercent >= 50) return "MONITORING";
        return "MONITORING";
    }

    private String getGlobalStatusLabel(String status) {
        return switch (status) {
            case "ON_TRACK" -> "Sur la bonne voie";
            case "MONITORING" -> "Surveillance";
            case "DIFFICULTY" -> "En difficulté";
            default -> "Inconnu";
        };
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "Termine" -> "✅ Terminé";
            case "En cours" -> "▣ En cours";
            case "A faire" -> "□ À faire";
            default -> status;
        };
    }

    private List<MilestoneResDto> getMilestones(Long projectId) {
        return milestoneRepository.findByProjectProjectId(projectId).stream()
                .map(m -> new MilestoneResDto(
                        m.getMilestoneId(),
                        m.getTitle(),
                        m.getPlannedDate(),
                        m.getActualDate(),
                        m.getStatus().name(),
                        getMilestoneStatusLabel(m.getStatus().name())
                ))
                .collect(Collectors.toList());
    }

    private String getMilestoneStatusLabel(String status) {
        return switch (status) {
            case "DONE" -> "✅ Fait";
            case "IN_PROGRESS" -> "🔄 En cours";
            case "OVERDUE" -> "⚠️ Retard";
            case "PLANNED" -> "🎯 Prochaine étape";
            default -> status;
        };
    }

    private List<BudgetItemResDto> getBudgetItems(Long projectId) {
        // Simplified - would come from BudgetItemRepository
        return List.of(
                new BudgetItemResDto("Design", 5000, 5500, 10.0),
                new BudgetItemResDto("Développement", 15000, 14000, -6.0),
                new BudgetItemResDto("Tests", 5000, 4500, -10.0),
                new BudgetItemResDto("Déploiement", 5000, 5000, 0.0)
        );
    }

    private List<RiskResDto> getRisks(Long projectId) {
        // Simplified - would come from RiskRepository
        return List.of(
                new RiskResDto(1L, "Départ d'un développeur", "Élevé", "Recrutement urgent", "Sponsor"),
                new RiskResDto(2L, "Panne serveur", "Moyen", "Backup externe", "DevOps"),
                new RiskResDto(3L, "Retard client", "Faible", "Relance hebdomadaire", "Chef de projet")
        );
    }

    private List<ActionItemResDto> getActionItems(Long projectId) {
        // Simplified - would come from ActionItemRepository
        return List.of(
                new ActionItemResDto(1L, "Valider le budget supplémentaire", "Sponsors", java.time.LocalDate.now().plusDays(3), "PENDING"),
                new ActionItemResDto(2L, "Recruter un testeur", "RH", java.time.LocalDate.now().plusDays(5), "PENDING")
        );
    }

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

    private List<PriorityDistributionResDto> computePriorityDistribution(List<Long> projectIds) {
        // Return empty for now - would need priority distribution query
        return List.of();
    }

    private List<AssigneeWorkloadResDto> getAssigneeWorkload(List<Long> projectIds) {
        // Return empty for now
        return List.of();
    }

    private LocalDateTime[] resolvePeriodDates(String period, String startDate, String endDate, LocalDate today) {
        LocalDateTime start;
        LocalDateTime end = today.atTime(LocalTime.MAX);

        switch (period != null ? period : "LAST_30_DAYS") {
            case "TODAY":
                start = today.atStartOfDay();
                break;
            case "LAST_7_DAYS":
                start = today.minusDays(7).atStartOfDay();
                break;
            case "LAST_30_DAYS":
                start = today.minusDays(30).atStartOfDay();
                break;
            case "LAST_3_MONTHS":
                start = today.minusMonths(3).atStartOfDay();
                break;
            case "THIS_YEAR":
                start = today.withDayOfYear(1).atStartOfDay();
                break;
            case "CUSTOM":
                try {
                    start = LocalDate.parse(startDate).atStartOfDay();
                } catch (Exception e) {
                    start = today.minusDays(30).atStartOfDay();
                }
                break;
            default:
                start = today.minusDays(30).atStartOfDay();
        }
        return new LocalDateTime[]{start, end};
    }

    private AdminDashboardStatsResDto emptyStats() {
        return new AdminDashboardStatsResDto(
                0, 0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }
}