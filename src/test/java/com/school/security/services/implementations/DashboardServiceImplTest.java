package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.entities.Activity;
import com.school.security.entities.Direction;
import com.school.security.entities.Project;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.ActivityType;
import com.school.security.enums.RoleType;
import com.school.security.repositories.ActivityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private DashboardServiceImpl dashboardService;

    private User adminUser;
    private User regularUser;
    private Project projectOne;
    private Project projectTwo;
    private List<Project> adminProjects;
    private List<Project> userProjects;

    @BeforeEach
    void setUp() {
        adminUser = buildUser(1L, "Admin", "User", RoleType.ADMIN);
        regularUser = buildUser(2L, "Regular", "User", RoleType.USER);

        projectOne = buildProject(1L, "Project Alpha", adminUser);
        projectTwo = buildProject(2L, "Project Beta", adminUser);

        adminProjects = List.of(projectOne, projectTwo);
        userProjects = List.of(projectOne);
    }

    @Test
    void getDashboardStatsShouldReturnDataForAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(25L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(3L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 10L},
                new Object[]{"En cours", 8L},
                new Object[]{"Termine", 7L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1)));
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(2)));
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of(
                new Object[]{1L, 15L},
                new Object[]{2L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.of(
                new Object[]{1L, 5L},
                new Object[]{2L, 2L}));
        when(userRepository.count()).thenReturn(42L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals("LAST_30_DAYS", result.period());
        assertEquals(2, result.stats().projects());
        assertEquals(25, result.stats().tasks());
        assertEquals(12, result.stats().completedTasks());
        assertEquals(3, result.stats().overdueTasks());
        assertEquals(42, result.stats().totalUsers());
        assertNotNull(result.evolution());
        assertNotNull(result.distribution());
        assertEquals(3, result.distribution().items().size());
        assertNotNull(result.recentActivity());
        assertNotNull(result.recentProjects());
        assertEquals(2, result.recentProjects().size());
        verify(projectRepository).findAccessibleProjectsByUserId(1L);
    }

    @Test
    void getDashboardStatsShouldReturnDataForRegularUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L))).thenReturn(10L);
        when(taskRepository.countCompletedBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(5L);
        when(taskRepository.countOverdueAndAssignedTo(anyList(), any(LocalDate.class), eq("Termine"), eq(2L))).thenReturn(1L);
        when(taskRepository.countByStatusGroupedAndAssignedTo(anyList(), eq(2L))).thenReturn(List.of(
                new Object[]{"A faire", 4L},
                new Object[]{"En cours", 3L},
                new Object[]{"Termine", 3L}));
        when(taskRepository.findCreatedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 3L}));
        when(userRepository.count()).thenReturn(15L);

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_7_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals("LAST_7_DAYS", result.period());
        assertEquals(1, result.stats().projects());
        assertEquals(10, result.stats().tasks());
        assertEquals(5, result.stats().completedTasks());
        assertEquals(1, result.stats().overdueTasks());
        assertEquals(15, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldReturnEmptyDashboardWhenNoProjects() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(List.of());

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(0, result.stats().projects());
        assertEquals(0, result.stats().tasks());
        assertEquals(0, result.stats().completedTasks());
        assertEquals(0, result.stats().overdueTasks());
        assertEquals(0, result.stats().totalUsers());
        assertEquals(0, result.evolution().points().size());
        assertEquals(0, result.distribution().items().size());
        assertEquals(0, result.recentActivity().size());
        assertEquals(0, result.recentProjects().size());
    }

    @Test
    void getDashboardStatsShouldHandleTodayPeriod() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(5L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 2L},
                new Object[]{"En cours", 2L},
                new Object[]{"Termine", 1L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
                LocalDateTime.now()));
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(42L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "TODAY", null, null, null);

        assertNotNull(result);
        assertEquals("TODAY", result.period());
        assertEquals(5, result.stats().tasks());
        assertEquals(2, result.stats().completedTasks());
        assertEquals(0, result.stats().overdueTasks());
        assertEquals(42, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldHandleCustomPeriod() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(10L);

        DashboardDataResDto result = dashboardService.getDashboardStats(
                1L, "CUSTOM", "2026-08-01", "2026-08-25", null);

        assertNotNull(result);
        assertEquals("CUSTOM", result.period());
        assertEquals(0, result.stats().completedTasks());
        assertEquals(10, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldReturnEmptyWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        DashboardDataResDto result = dashboardService.getDashboardStats(99L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(0, result.stats().projects());
    }

    @Test
    void getDashboardStatsShouldShowRecentActivityWithLimit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(5L);

        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Activity activity = new Activity();
            activity.setActivityId((long) i);
            activity.setProject(projectOne);
            activity.setUser(adminUser);
            activity.setType(ActivityType.TASK_CREATED);
            activity.setDescription("Activity " + i);
            activity.setCreatedAt(LocalDateTime.now().minusHours(i));
            activities.add(activity);
        }
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(activities);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(10, result.recentActivity().size());
    }

    @Test
    void getDashboardStatsShouldLimitRecentProjectsToFive() {
        User owner = buildUser(1L, "Owner", "User", RoleType.ADMIN);
        List<Project> manyProjects = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            manyProjects.add(buildProject((long) i, "Project " + i, owner));
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(manyProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(20L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(5, result.recentProjects().size());
        assertEquals(20, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldReturnDataForSuperAdmin() {
        User superAdmin = buildUser(3L, "Super", "Admin", RoleType.SUPER_ADMIN);
        when(userRepository.findById(3L)).thenReturn(Optional.of(superAdmin));
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(20L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(8L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(2L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 6L},
                new Object[]{"En cours", 6L},
                new Object[]{"Termine", 8L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(100L);

        DashboardDataResDto result = dashboardService.getDashboardStats(3L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals("LAST_30_DAYS", result.period());
        assertEquals(2, result.stats().projects());
        assertEquals(20, result.stats().tasks());
        assertEquals(8, result.stats().completedTasks());
        assertEquals(2, result.stats().overdueTasks());
        assertEquals(100, result.stats().totalUsers());
        verify(projectRepository).findByIsActiveTrue();
    }

    @Test
    void getDashboardStatsShouldUsePeriodSpecificCompletedTasks() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(15L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 5L},
                new Object[]{"En cours", 4L},
                new Object[]{"Termine", 6L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        when(userRepository.count()).thenReturn(30L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "TODAY", null, null, null);

        assertNotNull(result);
        assertEquals(3, result.stats().completedTasks());
        assertEquals(15, result.stats().tasks());
        assertEquals(30, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldHandleEmptyEvolution() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(5L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 4L},
                new Object[]{"Termine", 1L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(8L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_7_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(0, result.evolution().points().size());
        assertEquals(1, result.stats().completedTasks());
        assertEquals(8, result.stats().totalUsers());
    }

    @Test
    void getDashboardStatsShouldCalculateProgressCorrectly() {
        User owner = buildUser(1L, "Owner", "User", RoleType.ADMIN);
        List<Project> projects = List.of(buildProject(1L, "Project A", owner));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(projects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(10L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 6L}));
        when(userRepository.count()).thenReturn(25L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(1, result.recentProjects().size());
        assertEquals(60, result.recentProjects().get(0).progress());
        assertEquals(25, result.stats().totalUsers());
    }

    @Test
    void superAdminShouldSeeAllProjects() {
        User superAdmin = buildUser(3L, "Super", "Admin", RoleType.SUPER_ADMIN);
        when(userRepository.findById(3L)).thenReturn(Optional.of(superAdmin));
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(20L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(8L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(2L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(100L);

        dashboardService.getDashboardStats(3L, "LAST_30_DAYS", null, null, null);

        verify(projectRepository).findByIsActiveTrue();
        verify(projectRepository, never()).findAccessibleProjectsByUserId(any());
    }

    @Test
    void adminShouldSeeAccessibleProjectsOnly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(15L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(1L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(50L);

        dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        verify(projectRepository).findAccessibleProjectsByUserId(1L);
        verify(projectRepository, never()).findByIsActiveTrue();
    }

    @Test
    void userShouldSeePersonalTaskStats() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L))).thenReturn(8L);
        when(taskRepository.countCompletedBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(3L);
        when(taskRepository.countOverdueAndAssignedTo(anyList(), any(LocalDate.class), eq("Termine"), eq(2L))).thenReturn(1L);
        when(taskRepository.countByStatusGroupedAndAssignedTo(anyList(), eq(2L))).thenReturn(List.of(
                new Object[]{"A faire", 3L},
                new Object[]{"En cours", 2L},
                new Object[]{"Termine", 3L}));
        when(taskRepository.findCreatedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 3L}));
        when(userRepository.count()).thenReturn(30L);

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(8, result.stats().tasks());
        assertEquals(3, result.stats().completedTasks());
        assertEquals(1, result.stats().overdueTasks());
        assertEquals(30, result.stats().totalUsers());
        verify(taskRepository).countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L));
        verify(taskRepository, never()).countByIsActiveTrueAndProjectProjectIdIn(anyList());
    }

    @Test
    void userShouldSeeProjectLevelActivity() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L))).thenReturn(0L);
        when(taskRepository.countCompletedBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(0L);
        when(taskRepository.countOverdueAndAssignedTo(anyList(), any(LocalDate.class), eq("Termine"), eq(2L))).thenReturn(0L);
        when(taskRepository.countByStatusGroupedAndAssignedTo(anyList(), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(20L);

        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Activity activity = new Activity();
            activity.setActivityId((long) i);
            activity.setProject(projectOne);
            activity.setUser(regularUser);
            activity.setType(ActivityType.TASK_CREATED);
            activity.setDescription("Activity " + i);
            activity.setCreatedAt(LocalDateTime.now().minusHours(i));
            activities.add(activity);
        }
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(activities);

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(5, result.recentActivity().size());
        verify(activityRepository).findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void userShouldSeeProjectLevelProgress() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L))).thenReturn(0L);
        when(taskRepository.countCompletedBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(0L);
        when(taskRepository.countOverdueAndAssignedTo(anyList(), any(LocalDate.class), eq("Termine"), eq(2L))).thenReturn(0L);
        when(taskRepository.countByStatusGroupedAndAssignedTo(anyList(), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 6L}));
        when(userRepository.count()).thenReturn(15L);

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(1, result.recentProjects().size());
        assertEquals(60, result.recentProjects().get(0).progress());
        assertEquals(15, result.stats().totalUsers());
        verify(taskRepository).countActiveByProjectGrouped(anyList());
        verify(taskRepository).countByStatusAndProjectGrouped(anyList(), eq("Termine"));
    }

    @Test
    void adminShouldSeeProjectLevelStats() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(25L);
        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(3L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());
        when(userRepository.count()).thenReturn(40L);

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(25, result.stats().tasks());
        assertEquals(40, result.stats().totalUsers());
        verify(taskRepository).countByIsActiveTrueAndProjectProjectIdIn(anyList());
        verify(taskRepository, never()).countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), any());
    }

    @Test
    void userWithNoAssignedTasksShouldSeeZeros() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdInAndAssigneesContains(anyList(), eq(2L))).thenReturn(0L);
        when(taskRepository.countCompletedBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(0L);
        when(taskRepository.countOverdueAndAssignedTo(anyList(), any(LocalDate.class), eq("Termine"), eq(2L))).thenReturn(0L);
        when(taskRepository.countByStatusGroupedAndAssignedTo(anyList(), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetweenAndAssignedTo(anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(2L))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 3L}));
        when(userRepository.count()).thenReturn(5L);

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null, null);

        assertNotNull(result);
        assertEquals(0, result.stats().tasks());
        assertEquals(0, result.stats().completedTasks());
        assertEquals(0, result.stats().overdueTasks());
        assertEquals(5, result.stats().totalUsers());
        assertEquals(0, result.evolution().points().size());
        assertEquals(0, result.distribution().items().size());
        assertEquals(1, result.stats().projects());
        assertEquals(1, result.recentProjects().size());
    }

    private User buildUser(Long id, String firstname, String lastname, RoleType roleType) {
        Direction direction = new Direction();
        direction.setDirectionId(1L);
        direction.setName("DSI");

        Role role = new Role();
        role.setRolesId(roleType == RoleType.ADMIN ? 1L : 2L);
        role.setName(roleType);

        User user = new User();
        user.setUsersId(id);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(firstname.toLowerCase() + "." + lastname.toLowerCase() + "@test.com");
        user.setDirection(direction);
        user.setRoles(List.of(role));
        return user;
    }

    private Project buildProject(Long id, String title, User owner) {
        Project project = new Project();
        project.setProjectId(id);
        project.setTitle(title);
        project.setDescription("Description for " + title);
        project.setOwner(owner);
        project.setIsActive(true);
        return project;
    }
}
