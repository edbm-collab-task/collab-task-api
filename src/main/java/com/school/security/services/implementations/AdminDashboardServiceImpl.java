package com.school.security.services.implementations;

import com.school.security.dtos.responses.*;
import com.school.security.entities.*;
import com.school.security.enums.RoleType;
import com.school.security.repositories.*;
import com.school.security.services.contracts.AdminDashboardService;
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
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private TaskRepository taskRepository;
    private ActivityRepository activityRepository;

    private static final String COMPLETED_STATUS = "Termine";
    private static final String IN_PROGRESS_STATUS = "En cours";
    private static final String TODO_STATUS = "A faire";

    @Override
    public AdminDashboardStatsResDto getAdminDashboardStats(Long userId, String period, String startDate, String endDate) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return emptyStats();
        }

        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.SUPER_ADMIN);

        // Get accessible projects
        List<Project> accessibleProjects = isSuperAdmin
                ? projectRepository.findByIsActiveTrue()
                : projectRepository.findAccessibleProjectsByUserId(userId);

        List<Long> projectIds = accessibleProjects.stream()
                .map(Project::getProjectId)
                .collect(Collectors.toList());

        // Resolve dates
        LocalDate today = LocalDate.now();
        LocalDateTime[] dates = resolvePeriodDates(period, startDate, endDate, today);
        LocalDateTime periodStart = dates[0];
        LocalDateTime periodEnd = dates[1];

        // Global counts
        long totalUsers = userRepository.count();
        long totalProjects = projectRepository.findByIsActiveTrue().size();
        long activeProjects = accessibleProjects.size();
        long totalTasks = taskRepository.countByIsActiveTrueAndProjectProjectIdIn(accessibleProjects.stream()
                .map(Project::getProjectId).collect(Collectors.toList()));
        long completedTasks = taskRepository.countCompletedBetween(accessibleProjects.stream()
                .map(Project::getProjectId).collect(Collectors.toList()), periodStart, periodEnd);
        long overdueTasks = taskRepository.countOverdue(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), LocalDate.now(), COMPLETED_STATUS);
        long inProgressTasks = taskRepository.countByStatusAndProjectGrouped(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), IN_PROGRESS_STATUS)
                .stream().mapToLong(arr -> ((Number) arr[1]).longValue()).sum();
        long todoTasks = taskRepository.countByStatusAndProjectGrouped(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), TODO_STATUS)
                .stream().mapToLong(arr -> ((Number) arr[1]).longValue()).sum();

        // All stats
        List<UserStatsResDto> topUsers = getTopUsers(accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()));
        List<ProjectStatsResDto> topProjects = getTopProjects(accessibleProjects);
        List<EvolutionPointResDto> evolution = computeEvolution(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), periodStart, periodEnd);
        List<PriorityDistributionResDto> priorityDistribution = computePriorityDistribution(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()));
        List<AssigneeWorkloadResDto> assigneeWorkload = getAssigneeWorkload(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()));
        List<RecentActivityResDto> recentActivity = computeRecentActivity(
                accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), periodStart, periodEnd);

        return new AdminDashboardStatsResDto(
                userRepository.count(),
                projectRepository.findByIsActiveTrue().size(),
                activeProjects,
                taskRepository.countByIsActiveTrueAndProjectProjectIdIn(accessibleProjects.stream()
                        .map(Project::getProjectId).collect(Collectors.toList())),
                completedTasks,
                overdueTasks,
                inProgressTasks,
                todoTasks,
                evolution,
                computePriorityDistribution(accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList())),
                getAssigneeWorkload(accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList())),
                getTopUsers(accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList())),
                getTopProjects(accessibleProjects),
                List.of(),
                computeRecentActivity(accessibleProjects.stream().map(Project::getProjectId).collect(Collectors.toList()), periodStart, periodEnd)
        );
    }

    private List<UserStatsResDto> getTopUsers(List<Long> projectIds) {
        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> u.getRoles().stream().noneMatch(r -> r.getName() == RoleType.SUPER_ADMIN))
                .map(user -> {
                    long assignedTasks = taskRepository.countActiveByUserAndProjects(projectIds, user.getUsersId());
                    long completedTasks = taskRepository.countCompletedByUserAndPeriod(
                            projectIds, LocalDateTime.now().minusDays(30), LocalDateTime.now(), user.getUsersId());
                    long inProgressTasks = taskRepository.countByStatusAndUserAndProjects(projectIds, IN_PROGRESS_STATUS, user.getUsersId());
                    long overdueTasks = taskRepository.countOverdueByUser(
                            projectIds, LocalDate.now(), COMPLETED_STATUS, user.getUsersId());

                    return new UserStatsResDto(
                            user.getUsersId(),
                            user.getFirstname(),
                            user.getLastname(),
                            user.getEmail(),
                            user.getRoles().stream().findFirst().map(Role::getName).map(Enum::name).orElse("USER"),
                            user.getDirection() != null ? user.getDirection().getName() : "—",
                            assignedTasks,
                            completedTasks,
                            inProgressTasks,
                            overdueTasks,
                            "—"
                    );
                })
                .sorted(Comparator.comparingLong((UserStatsResDto u) -> u.assignedTasks()).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<ProjectStatsResDto> getTopProjects(List<Project> accessibleProjects) {
        return accessibleProjects.stream()
                .map(project -> {
                    List<Long> projectIdList = List.of(project.getProjectId());
                    long totalTasks = taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIdList);
                    long completedTasks = taskRepository.countCompletedBetween(projectIdList,
                            LocalDateTime.now().minusDays(30), LocalDateTime.now());
                    long overdueTasks = taskRepository.countOverdue(projectIdList, LocalDate.now(), COMPLETED_STATUS);
                    int progress = totalTasks > 0 ? (int) ((completedTasks * 100) / totalTasks) : 0;
                    String status = project.getIsActive() ? "Actif" : "Archivé";

                    return new ProjectStatsResDto(
                            project.getProjectId(),
                            project.getTitle(),
                            project.getOwner().getFirstname() + " " + project.getOwner().getLastname(),
                            "—",
                            totalTasks,
                            completedTasks,
                            overdueTasks,
                            progress,
                            status
                    );
                })
                .sorted(Comparator.comparingLong(ProjectStatsResDto::totalTasks).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<EvolutionPointResDto> computeEvolution(List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> createdDates = taskRepository.findCreatedDatesBetween(projectIds, start, end);
        List<LocalDateTime> completedDates = taskRepository.findCompletedDatesBetween(projectIds, start, end);

        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        
        // Determine grouping: by day if <= 31 days, by week if <= 90 days, by month otherwise
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

    private List<RecentActivityResDto> computeRecentActivity(List<Long> projectIds, LocalDateTime start, LocalDateTime end) {
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
        return new LocalDateTime[]{start, today.atTime(LocalTime.MAX)};
    }

    private AdminDashboardStatsResDto emptyStats() {
        return new AdminDashboardStatsResDto(
                0, 0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }
}