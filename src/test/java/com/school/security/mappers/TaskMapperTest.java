package com.school.security.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Priority;
import com.school.security.entities.Project;
import com.school.security.entities.Status;
import com.school.security.entities.Task;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Vérifie que {@code TaskMapper.toDto} expose l'ordre métier de la priorité via
 * {@code Priority.sortOrder} et non via l'identifiant de priorité.
 */
@ExtendWith(MockitoExtension.class)
class TaskMapperTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PriorityRepository priorityRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        taskMapper = new TaskMapper(
                projectRepository, priorityRepository, statusRepository, taskRepository, userRepository);
    }

    @Test
    void toDtoShouldExposeUrgenteSortOrderAsOne() {
        Task task = buildTaskWithPriority(1L, "Urgente", 1);

        TaskResDto dto = taskMapper.toDto(task, 0L);

        assertEquals("Urgente", dto.priorityName());
        assertEquals(1, dto.prioritySortOrder());
    }

    @Test
    void toDtoShouldExposeHauteSortOrderAsTwo() {
        Task task = buildTaskWithPriority(1L, "Haute", 2);

        TaskResDto dto = taskMapper.toDto(task, 0L);

        assertEquals("Haute", dto.priorityName());
        assertEquals(2, dto.prioritySortOrder());
    }

    @Test
    void toDtoShouldExposeMoyenneSortOrderAsThree() {
        Task task = buildTaskWithPriority(1L, "Moyenne", 3);

        TaskResDto dto = taskMapper.toDto(task, 0L);

        assertEquals("Moyenne", dto.priorityName());
        assertEquals(3, dto.prioritySortOrder());
    }

    @Test
    void toDtoShouldExposeBasseSortOrderAsFour() {
        Task task = buildTaskWithPriority(1L, "Basse", 4);

        TaskResDto dto = taskMapper.toDto(task, 0L);

        assertEquals("Basse", dto.priorityName());
        assertEquals(4, dto.prioritySortOrder());
    }

    @Test
    void toDtoShouldDetermineOrderFromSortOrderNotPriorityId() {
        // priorityId inverses de l'ordre métier : seul Priority.sortOrder compte.
        Task urgente = buildTaskWithPriority(99L, "Urgente", 1);
        Task basse = buildTaskWithPriority(2L, "Basse", 4);

        TaskResDto urgenteDto = taskMapper.toDto(urgente, 0L);
        TaskResDto basseDto = taskMapper.toDto(basse, 0L);

        assertEquals(99L, urgenteDto.priorityId());
        assertEquals(1, urgenteDto.prioritySortOrder());
        assertEquals(2L, basseDto.priorityId());
        assertEquals(4, basseDto.prioritySortOrder());
    }

    @Test
    void toDtoShouldExposeNullSortOrderWhenPrioritySortOrderIsMissing() {
        Task task = buildTaskWithPriority(1L, "Urgente", null);

        TaskResDto dto = taskMapper.toDto(task, 0L);

        assertNull(dto.prioritySortOrder());
    }

    private Task buildTaskWithPriority(Long priorityId, String priorityName, Integer prioritySortOrder) {
        Priority priority = new Priority();
        priority.setPriorityId(priorityId);
        priority.setName(priorityName);
        priority.setSortOrder(prioritySortOrder);

        Project project = new Project();
        project.setProjectId(1L);
        project.setTitle("Project Alpha");

        Status status = new Status();
        status.setStatusId(1L);
        status.setName("A faire");

        Task task = new Task();
        task.setTaskId(1L);
        task.setTitle("Task One");
        task.setDescription("Desc 1");
        task.setDueDate(LocalDate.of(2026, 1, 15));
        task.setProject(project);
        task.setPriority(priority);
        task.setStatus(status);
        task.setIsActive(true);
        task.setAssignees(List.of());
        return task;
    }
}
