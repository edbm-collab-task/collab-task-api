package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.school.security.dtos.responses.AdminDashboardStatsResDto;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.repositories.ActivityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
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

/** Vérifie que le tableau de bord admin accepte un utilisateur sans direction. */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ActivityRepository activityRepository;

    @InjectMocks private AdminDashboardServiceImpl adminDashboardService;

    private User adminWithoutDirection;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRolesId(1L);
        role.setName("ADMIN");
        role.setCodeRole("ADM");

        adminWithoutDirection = new User();
        adminWithoutDirection.setUsersId(1L);
        adminWithoutDirection.setFirstname("Admin");
        adminWithoutDirection.setLastname("Sans direction");
        adminWithoutDirection.setEmail("admin@test.com");
        adminWithoutDirection.setRoles(new ArrayList<>(List.of(role)));
    }

    @Test
    void getAdminDashboardStatsShouldHandleUserWithoutDirection() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminWithoutDirection));
        when(projectRepository.findAccessibleProjectsByUserId(1L)).thenReturn(List.of());
        when(projectRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of(adminWithoutDirection));
        when(taskRepository.countByStatusAndProjectGrouped(anyList(), anyString()))
                .thenReturn(List.of());
        when(taskRepository.findCreatedDatesBetween(
                        anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(taskRepository.findCompletedDatesBetween(
                        anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(activityRepository.findRecentByProjectIdsAndPeriod(
                        anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        AdminDashboardStatsResDto result =
                adminDashboardService.getAdminDashboardStats(1L, "TODAY", null, null);

        assertEquals(1, result.topUsers().size());
        assertEquals("—", result.topUsers().getFirst().direction());
    }
}
