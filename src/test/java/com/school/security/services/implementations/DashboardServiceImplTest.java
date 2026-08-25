package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(25L);
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

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null);

        assertNotNull(result);
        assertEquals("LAST_30_DAYS", result.period());
        assertEquals(2, result.stats().projects());
        assertEquals(25, result.stats().tasks());
        assertEquals(7, result.stats().completedTasks());
        assertEquals(3, result.stats().overdueTasks());
        assertNotNull(result.evolution());
        assertNotNull(result.distribution());
        assertEquals(3, result.distribution().items().size());
        assertNotNull(result.recentActivity());
        assertNotNull(result.recentProjects());
        assertEquals(2, result.recentProjects().size());
    }

    @Test
    void getDashboardStatsShouldReturnDataForRegularUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(userProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(10L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(1L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.of(
                new Object[]{"A faire", 4L},
                new Object[]{"En cours", 3L},
                new Object[]{"Termine", 3L}));
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 10L}));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of(
                new Object[]{1L, 3L}));

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_7_DAYS", null, null);

        assertNotNull(result);
        assertEquals("LAST_7_DAYS", result.period());
        assertEquals(1, result.stats().projects());
        assertEquals(10, result.stats().tasks());
        assertEquals(3, result.stats().completedTasks());
        assertEquals(1, result.stats().overdueTasks());
    }

    @Test
    void getDashboardStatsShouldReturnEmptyDashboardWhenNoProjects() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(projectRepository.findAccessibleProjectsByUserId(2L)).thenReturn(List.of());

        DashboardDataResDto result = dashboardService.getDashboardStats(2L, "LAST_30_DAYS", null, null);

        assertNotNull(result);
        assertEquals(0, result.stats().projects());
        assertEquals(0, result.stats().tasks());
        assertEquals(0, result.stats().completedTasks());
        assertEquals(0, result.stats().overdueTasks());
        assertEquals(0, result.evolution().points().size());
        assertEquals(0, result.distribution().items().size());
        assertEquals(0, result.recentActivity().size());
        assertEquals(0, result.recentProjects().size());
    }

    @Test
    void getDashboardStatsShouldHandleTodayPeriod() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(5L);
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

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "TODAY", null, null);

        assertNotNull(result);
        assertEquals("TODAY", result.period());
        assertEquals(5, result.stats().tasks());
    }

    @Test
    void getDashboardStatsShouldHandleCustomPeriod() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());

        DashboardDataResDto result = dashboardService.getDashboardStats(
                1L, "CUSTOM", "2026-08-01", "2026-08-25");

        assertNotNull(result);
        assertEquals("CUSTOM", result.period());
    }

    @Test
    void getDashboardStatsShouldReturnEmptyWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        DashboardDataResDto result = dashboardService.getDashboardStats(99L, "LAST_30_DAYS", null, null);

        assertNotNull(result);
        assertEquals(0, result.stats().projects());
    }

    @Test
    void getDashboardStatsShouldShowRecentActivityWithLimit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());

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

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null);

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
        when(projectRepository.findByIsActiveTrue()).thenReturn(manyProjects);
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(anyList())).thenReturn(0L);
        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);
        when(taskRepository.countByStatusGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.findCreatedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.countActiveByProjectGrouped(anyList())).thenReturn(List.<Object[]>of());
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), eq("Termine"))).thenReturn(List.<Object[]>of());

        DashboardDataResDto result = dashboardService.getDashboardStats(1L, "LAST_30_DAYS", null, null);

        assertNotNull(result);
        assertEquals(5, result.recentProjects().size());
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
