package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.Direction;
import com.school.security.entities.Project;
import com.school.security.entities.User;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.ProjectMapper;
import com.school.security.repositories.ProjectRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;

    @Mock private ProjectMapper projectMapper;

    @InjectMocks private ProjectServiceImpl projectService;

    private Project projectOne;
    private Project projectTwo;
    private ProjectResDto projectOneDto;
    private ProjectResDto projectTwoDto;
    private ProjectReqDto projectReqDto;

    @BeforeEach
    void setUp() {
        projectOne = buildProject(1L, "Project One", "Desc 1", true);
        projectTwo = buildProject(2L, "Project Two", "Desc 2", true);
        projectOneDto =
                new ProjectResDto(
                        1L,
                        "Project One",
                        "Desc 1",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        true,
                        10L,
                        "Jane Doe",
                        false);
        projectTwoDto =
                new ProjectResDto(
                        2L,
                        "Project Two",
                        "Desc 2",
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 2, 28),
                        true,
                        11L,
                        "John Smith",
                        false);
        projectReqDto =
                new ProjectReqDto(
                        "New Project",
                        "New description",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31));
    }

    @Test
    void findAllShouldReturnMappedActiveProjects() {
        when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(projectOne, projectTwo));
        when(projectMapper.toDto(projectOne)).thenReturn(projectOneDto);
        when(projectMapper.toDto(projectTwo)).thenReturn(projectTwoDto);

        List<ProjectResDto> result = projectService.findAll();

        assertEquals(List.of(projectOneDto, projectTwoDto), result);
        verify(projectRepository, times(1)).findByIsActiveTrue();
        verify(projectMapper, times(1)).toDto(projectOne);
        verify(projectMapper, times(1)).toDto(projectTwo);
    }

    @Test
    void findByIdShouldReturnProjectWhenFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectOne));
        when(projectMapper.toDto(projectOne)).thenReturn(projectOneDto);

        ProjectResDto result = projectService.findById(1L);

        assertEquals(projectOneDto, result);
        verify(projectRepository, times(1)).findById(1L);
        verify(projectMapper, times(1)).toDto(projectOne);
    }

    @Test
    void findByIdShouldThrowEntityExceptionWhenMissing() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> projectService.findById(99L));

        assertEquals("Project not found", exception.getMessage());
        verify(projectRepository, times(1)).findById(99L);
        verifyNoInteractions(projectMapper);
    }

    @Test
    void createOrUpdateShouldCreateProjectWhenNoIdIsProvided() {
        Project projectToCreate = buildProject(null, "New Project", "New description", true);
        ProjectResDto createdDto =
                new ProjectResDto(
                        3L,
                        "New Project",
                        "New description",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31),
                        true,
                        10L,
                        "Jane Doe",
                        false);
        when(projectMapper.fromDto(projectReqDto)).thenReturn(projectToCreate);
        when(projectRepository.save(projectToCreate)).thenReturn(projectToCreate);
        when(projectMapper.toDto(projectToCreate)).thenReturn(createdDto);

        ProjectResDto result = projectService.createOrUpdate(projectReqDto);

        assertEquals(createdDto, result);
        verify(projectMapper, times(1)).fromDto(projectReqDto);
        verify(projectRepository, times(1)).save(projectToCreate);
        verify(projectMapper, times(1)).toDto(projectToCreate);
    }

    @Test
    void saveShouldUpdateExistingProjectWhenIdExists() {
        Project updatedProject = buildProject(1L, "Updated Project", "Updated description", true);
        ProjectReqDto updateRequest =
                new ProjectReqDto(
                        "Updated Project",
                        "Updated description",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30));
        ProjectResDto updatedDto =
                new ProjectResDto(
                        1L,
                        "Updated Project",
                        "Updated description",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30),
                        true,
                        10L,
                        "Jane Doe",
                        false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectOne));
        when(projectRepository.save(projectOne)).thenReturn(updatedProject);
        when(projectMapper.toDto(updatedProject)).thenReturn(updatedDto);

        ProjectResDto result = projectService.save(updateRequest, 1L);

        assertEquals(updatedDto, result);
        assertEquals("Updated Project", projectOne.getTitle());
        assertEquals("Updated description", projectOne.getDescription());
        assertEquals(LocalDate.of(2026, 4, 1), projectOne.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 30), projectOne.getEndDate());
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(projectOne);
        verify(projectMapper, times(1)).toDto(updatedProject);
    }

    @Test
    void saveShouldCreateProjectWhenIdDoesNotExist() {
        Project projectToCreate =
                buildProject(null, "Fallback Project", "Fallback description", true);
        ProjectReqDto fallbackRequest =
                new ProjectReqDto(
                        "Fallback Project",
                        "Fallback description",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31));
        ProjectResDto createdDto =
                new ProjectResDto(
                        4L,
                        "Fallback Project",
                        "Fallback description",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        true,
                        10L,
                        "Jane Doe",
                        false);
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());
        when(projectMapper.fromDto(fallbackRequest)).thenReturn(projectToCreate);
        when(projectRepository.save(projectToCreate)).thenReturn(projectToCreate);
        when(projectMapper.toDto(projectToCreate)).thenReturn(createdDto);

        ProjectResDto result = projectService.save(fallbackRequest, 404L);

        assertEquals(createdDto, result);
        verify(projectRepository, times(1)).findById(404L);
        verify(projectMapper, times(1)).fromDto(fallbackRequest);
        verify(projectRepository, times(1)).save(projectToCreate);
        verify(projectMapper, times(1)).toDto(projectToCreate);
    }

    @Test
    void deleteByIdShouldArchiveExistingProject() {
        Project archivedProject = buildProject(1L, "Project One", "Desc 1", false);
        ProjectResDto archivedDto =
                new ProjectResDto(
                        1L,
                        "Project One",
                        "Desc 1",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        false,
                        10L,
                        "Jane Doe",
                        false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(projectOne));
        when(projectRepository.save(projectOne)).thenReturn(archivedProject);
        when(projectMapper.toDto(archivedProject)).thenReturn(archivedDto);

        ProjectResDto result = projectService.deleteById(1L);

        assertEquals(archivedDto, result);
        assertEquals(Boolean.FALSE, projectOne.getIsActive());
        verify(projectRepository, times(1)).findById(1L);
        verify(projectRepository, times(1)).save(projectOne);
        verify(projectMapper, times(1)).toDto(archivedProject);
    }

    @Test
    void deleteByIdShouldThrowEntityExceptionWhenMissing() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> projectService.deleteById(404L));

        assertEquals("Project not found", exception.getMessage());
        verify(projectRepository, times(1)).findById(404L);
        verifyNoInteractions(projectMapper);
    }

    private Project buildProject(Long id, String title, String description, Boolean active) {
        Direction direction = new Direction();
        direction.setDirectionId(1L);
        direction.setName("Direction");

        User owner = new User();
        owner.setUsersId(10L);
        owner.setFirstname("Jane");
        owner.setLastname("Doe");
        owner.setDirection(direction);

        Project project = new Project();
        project.setProjectId(id);
        project.setTitle(title);
        project.setDescription(description);
        project.setStartDate(id != null && id.equals(1L) ? LocalDate.of(2026, 1, 1) : LocalDate.of(2026, 2, 1));
        project.setEndDate(
                id != null && id.equals(1L)
                        ? LocalDate.of(2026, 1, 31)
                        : LocalDate.of(2026, 2, 28));
        project.setOwner(owner);
        project.setIsActive(active);
        return project;
    }
}
