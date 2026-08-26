package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.entities.Direction;
import com.school.security.entities.Priority;
import com.school.security.entities.Project;
import com.school.security.entities.Status;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.TaskMapper;
import com.school.security.repositories.PriorityRepository;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.TaskRepository;
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
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;

    @Mock private PriorityRepository priorityRepository;

    @Mock private StatusRepository statusRepository;

    @Mock private TaskMapper taskMapper;

    @InjectMocks private TaskServiceImpl taskService;

    private Task taskOne;
    private Task taskTwo;
    private TaskResDto taskOneDto;
    private TaskResDto taskTwoDto;
    private TaskReqDto createTaskReqDto;
    private Priority priority;
    private Status status;
    private Project project;

    @BeforeEach
    void setUp() {
        project = buildProject(10L, "Project Alpha");
        priority = buildPriority(3L, "High");
        status = buildStatus(2L, "In progress", 2);
        taskOne = buildTask(1L, "Task One", project, priority, status, null, true);
        taskTwo = buildTask(2L, "Task Two", project, priority, status, null, true);
        taskOneDto =
                new TaskResDto(
                        1L,
                        "Task One",
                        "Desc 1",
                        LocalDate.of(2026, 1, 15),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        taskTwoDto =
                new TaskResDto(
                        2L,
                        "Task Two",
                        "Desc 2",
                        LocalDate.of(2026, 1, 20),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        createTaskReqDto =
                new TaskReqDto(
                        "New Task",
                        "New task description",
                        LocalDate.of(2026, 6, 15),
                        10L,
                        3L,
                        2L,
                        null,
                        List.of());
    }

    @Test
    void findAllShouldReturnMappedActiveTasks() {
        when(taskRepository.findByIsActiveTrue()).thenReturn(List.of(taskOne, taskTwo));
        when(taskMapper.toDto(taskOne)).thenReturn(taskOneDto);
        when(taskMapper.toDto(taskTwo)).thenReturn(taskTwoDto);

        List<TaskResDto> result = taskService.findAll();

        assertEquals(List.of(taskOneDto, taskTwoDto), result);
        verify(taskRepository, times(1)).findByIsActiveTrue();
        verify(taskMapper, times(1)).toDto(taskOne);
        verify(taskMapper, times(1)).toDto(taskTwo);
    }

    @Test
    void findByIdShouldReturnTaskWhenFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskOne));
        when(taskMapper.toDto(taskOne)).thenReturn(taskOneDto);

        TaskResDto result = taskService.findById(1L);

        assertEquals(taskOneDto, result);
        verify(taskRepository, times(1)).findById(1L);
        verify(taskMapper, times(1)).toDto(taskOne);
    }

    @Test
    void findByIdShouldThrowEntityExceptionWhenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.findById(99L));

        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(99L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void findByProjectShouldReturnMappedTasksForGivenProject() {
        when(taskRepository.findByProjectProjectIdAndIsActiveTrue(10L))
                .thenReturn(List.of(taskOne, taskTwo));
        when(taskMapper.toDto(taskOne)).thenReturn(taskOneDto);
        when(taskMapper.toDto(taskTwo)).thenReturn(taskTwoDto);

        List<TaskResDto> result = taskService.findByProject(10L);

        assertEquals(List.of(taskOneDto, taskTwoDto), result);
        verify(taskRepository, times(1)).findByProjectProjectIdAndIsActiveTrue(10L);
        verify(taskMapper, times(1)).toDto(taskOne);
        verify(taskMapper, times(1)).toDto(taskTwo);
    }

    @Test
    void createOrUpdateShouldCreateTaskWhenNoIdIsProvided() {
        Task taskToCreate = buildTask(null, "New Task", project, priority, status, null, true);
        TaskResDto createdDto =
                new TaskResDto(
                        3L,
                        "New Task",
                        "New task description",
                        LocalDate.of(2026, 6, 15),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        when(taskMapper.fromDto(createTaskReqDto)).thenReturn(taskToCreate);
        when(taskRepository.save(taskToCreate)).thenReturn(taskToCreate);
        when(taskMapper.toDto(taskToCreate)).thenReturn(createdDto);

        TaskResDto result = taskService.createOrUpdate(createTaskReqDto);

        assertEquals(createdDto, result);
        verify(taskMapper, times(1)).fromDto(createTaskReqDto);
        verify(taskRepository, times(1)).save(taskToCreate);
        verify(taskMapper, times(1)).toDto(taskToCreate);
    }

    @Test
    void saveShouldUpdateExistingTaskWhenIdExists() {
        Task updatedTask = buildTask(1L, "Updated Task", project, priority, status, null, true);
        TaskReqDto updateRequest =
                new TaskReqDto(
                        "Updated Task",
                        "Updated description",
                        LocalDate.of(2026, 7, 1),
                        10L,
                        3L,
                        2L,
                        null,
                        List.of());
        TaskResDto updatedDto =
                new TaskResDto(
                        1L,
                        "Updated Task",
                        "Updated description",
                        LocalDate.of(2026, 7, 1),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskOne));
        when(priorityRepository.getReferenceById(3L)).thenReturn(priority);
        when(statusRepository.getReferenceById(2L)).thenReturn(status);
        when(taskRepository.save(taskOne)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(updatedDto);

        TaskResDto result = taskService.save(updateRequest, 1L);

        assertEquals(updatedDto, result);
        assertEquals("Updated Task", taskOne.getTitle());
        assertEquals("Updated description", taskOne.getDescription());
        assertEquals(LocalDate.of(2026, 7, 1), taskOne.getDueDate());
        verify(taskRepository, times(1)).findById(1L);
        verify(priorityRepository, times(1)).getReferenceById(3L);
        verify(statusRepository, times(1)).getReferenceById(2L);
        verify(taskRepository, times(1)).save(taskOne);
        verify(taskMapper, times(1)).toDto(updatedTask);
    }

    @Test
    void saveShouldCreateTaskWhenIdDoesNotExist() {
        Task taskToCreate = buildTask(null, "Fallback Task", project, priority, status, null, true);
        TaskReqDto fallbackRequest =
                new TaskReqDto(
                        "Fallback Task",
                        "Fallback description",
                        LocalDate.of(2026, 8, 1),
                        10L,
                        3L,
                        2L,
                        null,
                        List.of());
        TaskResDto createdDto =
                new TaskResDto(
                        4L,
                        "Fallback Task",
                        "Fallback description",
                        LocalDate.of(2026, 8, 1),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());
        when(taskMapper.fromDto(fallbackRequest)).thenReturn(taskToCreate);
        when(taskRepository.save(taskToCreate)).thenReturn(taskToCreate);
        when(taskMapper.toDto(taskToCreate)).thenReturn(createdDto);

        TaskResDto result = taskService.save(fallbackRequest, 404L);

        assertEquals(createdDto, result);
        verify(taskRepository, times(1)).findById(404L);
        verify(taskMapper, times(1)).fromDto(fallbackRequest);
        verify(taskRepository, times(1)).save(taskToCreate);
        verify(taskMapper, times(1)).toDto(taskToCreate);
    }

    @Test
    void saveShouldThrowWhenParentIsSelf() {
        TaskReqDto invalidRequest =
                new TaskReqDto("Task", "Desc", LocalDate.of(2026, 9, 1), 10L, 3L, 2L, 1L, List.of());
        Task existingTask = buildTask(1L, "Task One", project, priority, status, null, true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.save(invalidRequest, 1L));

        assertEquals("A task cannot be its own parent", exception.getMessage());
        verify(taskRepository, times(1)).findById(1L);
        verify(priorityRepository, times(1)).getReferenceById(3L);
        verify(statusRepository, times(1)).getReferenceById(2L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void saveShouldThrowWhenParentTaskDoesNotExist() {
        TaskReqDto invalidRequest =
                new TaskReqDto("Task", "Desc", LocalDate.of(2026, 9, 1), 10L, 3L, 2L, 99L, List.of());
        Task existingTask = buildTask(1L, "Task One", project, priority, status, null, true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(priorityRepository.getReferenceById(3L)).thenReturn(priority);
        when(statusRepository.getReferenceById(2L)).thenReturn(status);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.save(invalidRequest, 1L));

        assertEquals("Parent task not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(1L);
        verify(priorityRepository, times(1)).getReferenceById(3L);
        verify(statusRepository, times(1)).getReferenceById(2L);
        verify(taskRepository, times(1)).findById(99L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void saveShouldThrowWhenParentTaskBelongsToAnotherProject() {
        Project otherProject = buildProject(20L, "Project Beta");
        Task parentTask = buildTask(99L, "Parent Task", otherProject, priority, status, null, true);
        Task existingTask = buildTask(1L, "Task One", project, priority, status, null, true);
        TaskReqDto invalidRequest =
                new TaskReqDto("Task", "Desc", LocalDate.of(2026, 9, 1), 10L, 3L, 2L, 99L, List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(priorityRepository.getReferenceById(3L)).thenReturn(priority);
        when(statusRepository.getReferenceById(2L)).thenReturn(status);
        when(taskRepository.findById(99L)).thenReturn(Optional.of(parentTask));

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.save(invalidRequest, 1L));

        assertEquals("Parent task must belong to the same project", exception.getMessage());
        verify(taskRepository, times(1)).findById(1L);
        verify(priorityRepository, times(1)).getReferenceById(3L);
        verify(statusRepository, times(1)).getReferenceById(2L);
        verify(taskRepository, times(1)).findById(99L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void deleteByIdShouldArchiveExistingTask() {
        Task archivedTask = buildTask(1L, "Task One", project, priority, status, null, false);
        TaskResDto archivedDto =
                new TaskResDto(
                        1L,
                        "Task One",
                        "Desc 1",
                        LocalDate.of(2026, 1, 15),
                        false,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        2L,
                        "In progress",
                        null,
                        List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskOne));
        when(taskRepository.save(taskOne)).thenReturn(archivedTask);
        when(taskMapper.toDto(archivedTask)).thenReturn(archivedDto);

        TaskResDto result = taskService.deleteById(1L);

        assertEquals(archivedDto, result);
        assertEquals(Boolean.FALSE, taskOne.getIsActive());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(taskOne);
        verify(taskMapper, times(1)).toDto(archivedTask);
    }

    @Test
    void deleteByIdShouldThrowWhenTaskDoesNotExist() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.deleteById(404L));

        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(404L);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void changerStatutShouldUpdateStatusWhenTaskAndStatusExist() {
        Status done = buildStatus(3L, "Done", 3);
        Task updatedTask = buildTask(1L, "Task One", project, priority, done, null, true);
        TaskResDto updatedDto =
                new TaskResDto(
                        1L,
                        "Task One",
                        "Desc 1",
                        LocalDate.of(2026, 1, 15),
                        true,
                        10L,
                        "Project Alpha",
                        3L,
                        "High",
                        3L,
                        "Done",
                        null,
                        List.of());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskOne));
        when(statusRepository.findById(3L)).thenReturn(Optional.of(done));
        when(taskRepository.save(taskOne)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(updatedDto);

        TaskResDto result = taskService.changerStatut(1L, 3L);

        assertEquals(updatedDto, result);
        assertEquals(done, taskOne.getStatus());
        verify(taskRepository, times(1)).findById(1L);
        verify(statusRepository, times(1)).findById(3L);
        verify(taskRepository, times(1)).save(taskOne);
        verify(taskMapper, times(1)).toDto(updatedTask);
    }

    @Test
    void changerStatutShouldThrowWhenTaskDoesNotExist() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.changerStatut(404L, 3L));

        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(404L);
        verifyNoInteractions(statusRepository);
        verifyNoInteractions(taskMapper);
    }

    @Test
    void changerStatutShouldThrowWhenStatusDoesNotExist() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskOne));
        when(statusRepository.findById(404L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(EntityException.class, () -> taskService.changerStatut(1L, 404L));

        assertEquals("Status not found", exception.getMessage());
        verify(taskRepository, times(1)).findById(1L);
        verify(statusRepository, times(1)).findById(404L);
        verifyNoInteractions(taskMapper);
    }

    private Task buildTask(
            Long id,
            String title,
            Project project,
            Priority priority,
            Status status,
            Task parent,
            Boolean active) {
        Task task = new Task();
        task.setTaskId(id);
        task.setTitle(title);
        task.setDescription(id != null && id.equals(1L) ? "Desc 1" : "Desc 2");
        task.setDueDate(
                id != null && id.equals(1L)
                        ? LocalDate.of(2026, 1, 15)
                        : LocalDate.of(2026, 1, 20));
        task.setProject(project);
        task.setPriority(priority);
        task.setStatus(status);
        task.setParent(parent);
        task.setIsActive(active);
        return task;
    }

    private Project buildProject(Long id, String title) {
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
        project.setDescription("Project description");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setOwner(owner);
        project.setIsActive(true);
        return project;
    }

    private Priority buildPriority(Long id, String name) {
        Priority priority = new Priority();
        priority.setPriorityId(id);
        priority.setName(name);
        return priority;
    }

    private Status buildStatus(Long id, String name, Integer sortOrder) {
        Status status = new Status();
        status.setStatusId(id);
        status.setName(name);
        status.setSortOrder(sortOrder);
        return status;
    }
}
