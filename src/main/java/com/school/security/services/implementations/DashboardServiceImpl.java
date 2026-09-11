package com.school.security.services.implementations;

import com.school.security.dtos.responses.DashboardActivityItemResDto;
import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.dtos.responses.DashboardDistributionItemResDto;
import com.school.security.dtos.responses.DashboardDistributionResDto;
import com.school.security.dtos.responses.DashboardEvolutionPointResDto;
import com.school.security.dtos.responses.DashboardEvolutionResDto;
import com.school.security.dtos.responses.DashboardRecentProjectResDto;
import com.school.security.dtos.responses.DashboardStatsResDto;
import com.school.security.entities.Project;
import com.school.security.enums.RoleType;
import com.school.security.repositories.ActivityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.DashboardService;
import com.school.security.common.PeriodUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service du tableau de bord utilisateur et administrateur.
 * Fournit les statistiques, l'évolution, la distribution et l'activité récente
 * en fonction de la période et du projet sélectionné.
 * Utilise PeriodUtils pour éviter la duplication de la résolution des périodes.
 */
@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private ActivityRepository activityRepository;
    private UserRepository userRepository;

    private static final String COMPLETED_STATUS_NAME = "Termine";
    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    @Override
    public DashboardDataResDto getDashboardStats(Long userId, String period, String startDate, String endDate, Long projectId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return buildEmptyDashboard(period);
        }

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.SUPER_ADMIN);
        boolean isPersonal = user.getRoles().stream()
                .noneMatch(role -> role.getName() == RoleType.SUPER_ADMIN
                                || role.getName() == RoleType.ADMIN);

        List<Project> accessibleProjects = isSuperAdmin
                ? projectRepository.findByIsActiveTrue()
                : projectRepository.findAccessibleProjectsByUserId(userId);

        List<Long> projectIds = accessibleProjects.stream()
                .map(Project::getProjectId)
                .collect(Collectors.toList());

        if (projectId != null) {
            if (!projectIds.contains(projectId)) {
                return buildEmptyDashboard(period);
            }
            projectIds = List.of(projectId);
        }

        if (projectIds.isEmpty()) {
            return buildEmptyDashboard(period);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime[] dates = resolvePeriodDates(period, startDate, endDate, today);

        Long personalUserId = isPersonal ? userId : null;

        long totalUsers = userRepository.count();
        DashboardStatsResDto stats = computeStats(projectIds, personalUserId, today, dates[0], dates[1], totalUsers);
        DashboardEvolutionResDto evolution = computeEvolution(projectIds, personalUserId, dates[0], dates[1]);
        DashboardDistributionResDto distribution = computeDistribution(projectIds, personalUserId);
        List<DashboardActivityItemResDto> recentActivity = computeRecentActivity(projectIds, dates[0], dates[1]);
        List<DashboardRecentProjectResDto> recentProjects = computeRecentProjects(accessibleProjects, personalUserId);

        return new DashboardDataResDto(
                period,
                stats,
                evolution,
                distribution,
                recentActivity,
                recentProjects);
    }

    /**
     * Délègue à PeriodUtils pour éviter la duplication avec AdminDashboardServiceImpl et ProjectReportServiceImpl.
     */
    private LocalDateTime[] resolvePeriodDates(String period, String startDate, String endDate, LocalDate today) {
        return PeriodUtils.resolvePeriodDates(period, startDate, endDate, today);
    }

    private DashboardStatsResDto computeStats(
            List<Long> projectIds, Long personalUserId, LocalDate today, LocalDateTime start, LocalDateTime end,
            long totalUsers) {
        long totalTasks = personalUserId == null
                ? taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds)
                : taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(projectIds, personalUserId);
        long completedTasks = personalUserId == null
                ? taskRepository.countCompletedBetween(projectIds, start, end)
                : taskRepository.countCompletedBetweenAndAssignedTo(projectIds, start, end, personalUserId);
        long overdueTasks = personalUserId == null
                ? taskRepository.countOverdue(projectIds, today, COMPLETED_STATUS_NAME)
                : taskRepository.countOverdueAndAssignedTo(projectIds, today, COMPLETED_STATUS_NAME, personalUserId);
        long totalContributors = taskRepository.countDistinctContributors(projectIds);

        return new DashboardStatsResDto(
                projectIds.size(),
                totalTasks,
                completedTasks,
                overdueTasks,
                totalUsers,
                totalContributors);
    }

    private DashboardEvolutionResDto computeEvolution(
            List<Long> projectIds, Long personalUserId, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> createdDates = personalUserId == null
                ? taskRepository.findCreatedDatesBetween(projectIds, start, end)
                : taskRepository.findCreatedDatesBetweenAndAssignedTo(projectIds, start, end, personalUserId);
        List<LocalDateTime> completedDates = personalUserId == null
                ? taskRepository.findCompletedDatesBetween(projectIds, start, end)
                : taskRepository.findCompletedDatesBetweenAndAssignedTo(projectIds, start, end, personalUserId);

        String period = resolvePeriodType(start, end);
        boolean useMonthly = "MONTH".equals(period);

        Map<String, long[]> evolutionMap = new LinkedHashMap<>();

        for (LocalDateTime date : createdDates) {
            String label = useMonthly
                    ? date.format(MONTH_LABEL_FORMAT)
                    : date.format(DAY_LABEL_FORMAT);
            evolutionMap.computeIfAbsent(label, k -> new long[]{0, 0})[0]++;
        }

        for (LocalDateTime date : completedDates) {
            String label = useMonthly
                    ? date.format(MONTH_LABEL_FORMAT)
                    : date.format(DAY_LABEL_FORMAT);
            evolutionMap.computeIfAbsent(label, k -> new long[]{0, 0})[1]++;
        }

        List<DashboardEvolutionPointResDto> points = evolutionMap.entrySet().stream()
                .map(entry -> new DashboardEvolutionPointResDto(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1]))
                .collect(Collectors.toList());

        return new DashboardEvolutionResDto(points);
    }

    private String resolvePeriodType(LocalDateTime start, LocalDateTime end) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        if (days > 90) {
            return "MONTH";
        }
        return "DAY";
    }

    private DashboardDistributionResDto computeDistribution(List<Long> projectIds, Long personalUserId) {
        List<Object[]> statusCounts = personalUserId == null
                ? taskRepository.countByStatusGrouped(projectIds)
                : taskRepository.countByStatusGroupedAndAssignedTo(projectIds, personalUserId);
        long total = statusCounts.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        List<DashboardDistributionItemResDto> items = statusCounts.stream()
                .map(row -> {
                    String statusName = (String) row[0];
                    long count = (Long) row[1];
                    return new DashboardDistributionItemResDto(
                            statusName,
                            statusName,
                            count,
                            resolveStatusCssClass(statusName),
                            resolveStatusCssClass(statusName));
                })
                .collect(Collectors.toList());

        return new DashboardDistributionResDto(items, total);
    }

    private String resolveStatusCssClass(String statusName) {
        return switch (statusName) {
            case "A faire" -> "bg-amber-400";
            case "En cours" -> "bg-blue-500";
            case "Termine" -> "bg-emerald-500";
            default -> "bg-gray-400";
        };
    }

    private List<DashboardActivityItemResDto> computeRecentActivity(
            List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
        return activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end).stream()
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(activity -> new DashboardActivityItemResDto(
                        activity.getActivityId(),
                        activity.getType().name(),
                        activity.getDescription(),
                        activity.getUser().getFirstname() + " " + activity.getUser().getLastname(),
                        activity.getProject().getTitle(),
                        activity.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private List<DashboardRecentProjectResDto> computeRecentProjects(List<Project> projects, Long personalUserId) {
        // Pour USER : ne garder que les projets où il a au moins une tâche assignée (cohérent avec stats personnelles)
        List<Project> filtered = projects;
        if (personalUserId != null) {
            filtered = projects.stream()
                    .filter(p -> taskRepository.countActiveByUserAndProjects(List.of(p.getProjectId()), personalUserId) > 0)
                    .toList();
        }
        List<Long> projectIds = filtered.stream()
                .map(Project::getProjectId)
                .collect(Collectors.toList());

        Map<Long, Long> totalByProject = projectIds.isEmpty() ? Map.of() : taskRepository.countActiveByProjectGrouped(projectIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        Map<Long, Long> completedByProject = projectIds.isEmpty() ? Map.of() : taskRepository
                .countByStatusAndProjectGrouped(projectIds, COMPLETED_STATUS_NAME).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        return filtered.stream()
                .limit(RECENT_PROJECTS_LIMIT)
                .map(project -> {
                    long total = totalByProject.getOrDefault(project.getProjectId(), 0L);
                    long completed = completedByProject.getOrDefault(project.getProjectId(), 0L);
                    int progress = total > 0 ? (int) ((completed * 100) / total) : 0;
                    return new DashboardRecentProjectResDto(
                            project.getProjectId(),
                            project.getTitle(),
                            project.getOwner().getFirstname() + " " + project.getOwner().getLastname(),
                            progress);
                })
                .collect(Collectors.toList());
    }

    private DashboardDataResDto buildEmptyDashboard(String period) {
        return new DashboardDataResDto(
                period,
                new DashboardStatsResDto(0, 0, 0, 0, 0, 0),
                new DashboardEvolutionResDto(List.of()),
                new DashboardDistributionResDto(List.of(), 0),
                List.of(),
                List.of());
    }
}
