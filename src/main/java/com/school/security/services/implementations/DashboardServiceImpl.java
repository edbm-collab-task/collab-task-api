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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public DashboardDataResDto getDashboardStats(Long userId, String period, String startDate, String endDate) {
        List<Project> accessibleProjects = findAccessibleProjects(userId);
        List<Long> projectIds = accessibleProjects.stream()
                .map(Project::getProjectId)
                .collect(Collectors.toList());

        if (projectIds.isEmpty()) {
            return buildEmptyDashboard(period);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime[] dates = resolvePeriodDates(period, startDate, endDate, today);

        DashboardStatsResDto stats = computeStats(projectIds, today);
        DashboardEvolutionResDto evolution = computeEvolution(projectIds, dates[0], dates[1]);
        DashboardDistributionResDto distribution = computeDistribution(projectIds);
        List<DashboardActivityItemResDto> recentActivity = computeRecentActivity(projectIds, dates[0], dates[1]);
        List<DashboardRecentProjectResDto> recentProjects = computeRecentProjects(accessibleProjects);

        return new DashboardDataResDto(
                period,
                stats,
                evolution,
                distribution,
                recentActivity,
                recentProjects);
    }

    private List<Project> findAccessibleProjects(Long userId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.ADMIN || role.getName() == RoleType.SUPER_ADMIN);
        if (isAdmin) {
            return projectRepository.findByIsActiveTrue();
        }
        return projectRepository.findAccessibleProjectsByUserId(userId);
    }

    private LocalDateTime[] resolvePeriodDates(String period, String startDate, String endDate, LocalDate today) {
        LocalDateTime start;
        LocalDateTime end = today.atTime(LocalTime.MAX);

        switch (period) {
            case "TODAY":
                start = today.atStartOfDay();
                break;
            case "LAST_7_DAYS":
                start = today.minusDays(6).atStartOfDay();
                break;
            case "LAST_30_DAYS":
                start = today.minusDays(29).atStartOfDay();
                break;
            case "LAST_3_MONTHS":
                start = today.minusMonths(3).atStartOfDay();
                break;
            case "THIS_YEAR":
                start = today.withDayOfYear(1).atStartOfDay();
                break;
            case "CUSTOM":
                start = (startDate != null && !startDate.isEmpty())
                        ? LocalDate.parse(startDate).atStartOfDay()
                        : today.minusDays(29).atStartOfDay();
                end = (endDate != null && !endDate.isEmpty())
                        ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                        : today.atTime(LocalTime.MAX);
                break;
            default:
                start = today.minusDays(29).atStartOfDay();
                break;
        }

        return new LocalDateTime[]{start, end};
    }

    private DashboardStatsResDto computeStats(List<Long> projectIds, LocalDate today) {
        long totalTasks = taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds);
        long overdueTasks = taskRepository.countOverdue(projectIds, today, COMPLETED_STATUS_NAME);

        List<Object[]> statusCounts = taskRepository.countByStatusGrouped(projectIds);
        long completedTasks = statusCounts.stream()
                .filter(row -> COMPLETED_STATUS_NAME.equals(row[0]))
                .mapToLong(row -> (Long) row[1])
                .sum();

        return new DashboardStatsResDto(
                projectIds.size(),
                totalTasks,
                completedTasks,
                overdueTasks);
    }

    private DashboardEvolutionResDto computeEvolution(List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> createdDates = taskRepository.findCreatedDatesBetween(projectIds, start, end);
        List<LocalDateTime> completedDates = taskRepository.findCompletedDatesBetween(projectIds, start, end);

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

    private DashboardDistributionResDto computeDistribution(List<Long> projectIds) {
        List<Object[]> statusCounts = taskRepository.countByStatusGrouped(projectIds);
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
                            resolveDotClass(statusName),
                            resolveColorClass(statusName));
                })
                .collect(Collectors.toList());

        return new DashboardDistributionResDto(items, total);
    }

    private String resolveDotClass(String statusName) {
        return switch (statusName) {
            case "A faire" -> "bg-amber-400";
            case "En cours" -> "bg-blue-500";
            case "Termine" -> "bg-emerald-500";
            default -> "bg-gray-400";
        };
    }

    private String resolveColorClass(String statusName) {
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

    private List<DashboardRecentProjectResDto> computeRecentProjects(List<Project> projects) {
        List<Long> projectIds = projects.stream()
                .map(Project::getProjectId)
                .collect(Collectors.toList());

        Map<Long, Long> totalByProject = taskRepository.countActiveByProjectGrouped(projectIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        Map<Long, Long> completedByProject = taskRepository
                .countByStatusAndProjectGrouped(projectIds, COMPLETED_STATUS_NAME).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        return projects.stream()
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
                new DashboardStatsResDto(0, 0, 0, 0),
                new DashboardEvolutionResDto(List.of()),
                new DashboardDistributionResDto(List.of(), 0),
                List.of(),
                List.of());
    }
}
