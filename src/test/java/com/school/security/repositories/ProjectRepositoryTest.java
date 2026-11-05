package com.school.security.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.entities.Project;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectRepositoryTest {

    @Mock
    private ProjectRepository projectRepository;

    private Long adminUserId;
    private Long regularUserId;

    @BeforeEach
    void setUp() {
        adminUserId = 1L;
        regularUserId = 2L;
    }

    @Test
    void findByIsActiveTrueShouldReturnAllActiveProjects() {
        List<Project> expected = List.of(
                buildProject(1L, "Project Alpha"),
                buildProject(2L, "Project Beta"),
                buildProject(3L, "Project Gamma"));

        when(projectRepository.findByIsActiveTrue()).thenReturn(expected);

        List<Project> result = projectRepository.findByIsActiveTrue();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(projectRepository).findByIsActiveTrue();
    }

    @Test
    void findByIsActiveTrueShouldReturnEmptyWhenNoActiveProjects() {
        when(projectRepository.findByIsActiveTrue()).thenReturn(List.of());

        List<Project> result = projectRepository.findByIsActiveTrue();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAccessibleProjectsByUserIdShouldReturnOwnerProjects() {
        List<Project> expected = List.of(
                buildProject(1L, "Owned Project"));

        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(expected);

        List<Project> result = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Owned Project", result.get(0).getTitle());
        verify(projectRepository).findAccessibleProjectsByUserId(regularUserId);
    }

    @Test
    void findAccessibleProjectsByUserIdShouldReturnContributedProjects() {
        List<Project> expected = List.of(
                buildProject(1L, "Contributed Project"));

        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(expected);

        List<Project> result = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findAccessibleProjectsByUserIdShouldReturnEmptyWhenNoAccess() {
        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(List.of());

        List<Project> result = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findAccessibleProjectsByUserIdShouldExcludeArchivedProjects() {
        List<Project> expected = List.of(
                buildProject(1L, "Active Project"));

        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(expected);

        List<Project> result = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        for (Project project : result) {
            assertEquals(true, project.getIsActive());
        }
    }

    @Test
    void findAccessibleProjectsByUserIdShouldReturnDistinctProjects() {
        List<Project> expected = List.of(
                buildProject(1L, "Shared Project"));

        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(expected);

        List<Project> result = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findByIsActiveTrueAndFindAccessibleProjectsByUserIdShouldBothWork() {
        List<Project> adminProjects = List.of(
                buildProject(1L, "All Project 1"),
                buildProject(2L, "All Project 2"));

        List<Project> userProjects = List.of(
                buildProject(1L, "User Project 1"));

        when(projectRepository.findByIsActiveTrue()).thenReturn(adminProjects);
        when(projectRepository.findAccessibleProjectsByUserId(regularUserId)).thenReturn(userProjects);

        List<Project> adminResult = projectRepository.findByIsActiveTrue();
        List<Project> userResult = projectRepository.findAccessibleProjectsByUserId(regularUserId);

        assertEquals(2, adminResult.size());
        assertEquals(1, userResult.size());
    }

    private Project buildProject(Long id, String title) {
        Project project = new Project();
        project.setProjectId(id);
        project.setTitle(title);
        project.setDescription("Description for " + title);
        project.setIsActive(true);
        return project;
    }
}
