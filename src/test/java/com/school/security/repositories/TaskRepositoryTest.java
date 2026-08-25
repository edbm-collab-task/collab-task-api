package com.school.security.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.entities.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskRepositoryTest {

    @Mock
    private TaskRepository taskRepository;

    private List<Long> projectIds;

    @BeforeEach
    void setUp() {
        projectIds = List.of(1L, 2L, 3L);
    }

    @Test
    void countByIsActiveTrueAndProjectProjectIdInShouldReturnTotalActiveTasks() {
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds)).thenReturn(42L);

        long result = taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds);

        assertEquals(42L, result);
        verify(taskRepository).countByIsActiveTrueAndProjectProjectIdIn(projectIds);
    }

    @Test
    void countByIsActiveTrueAndProjectProjectIdInShouldReturnZeroWhenNoTasks() {
        when(taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds)).thenReturn(0L);

        long result = taskRepository.countByIsActiveTrueAndProjectProjectIdIn(projectIds);

        assertEquals(0L, result);
    }

    @Test
    void countCompletedBetweenShouldReturnCompletedTasksInPeriod() {
        LocalDateTime start = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        when(taskRepository.countCompletedBetween(projectIds, start, end)).thenReturn(15L);

        long result = taskRepository.countCompletedBetween(projectIds, start, end);

        assertEquals(15L, result);
        verify(taskRepository).countCompletedBetween(projectIds, start, end);
    }

    @Test
    void countCompletedBetweenShouldReturnZeroWhenNoCompletions() {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        when(taskRepository.countCompletedBetween(projectIds, start, end)).thenReturn(0L);

        long result = taskRepository.countCompletedBetween(projectIds, start, end);

        assertEquals(0L, result);
    }

    @Test
    void countOverdueShouldReturnTasksPastDueNotCompleted() {
        LocalDate today = LocalDate.now();

        when(taskRepository.countOverdue(projectIds, today, "Termine")).thenReturn(5L);

        long result = taskRepository.countOverdue(projectIds, today, "Termine");

        assertEquals(5L, result);
        verify(taskRepository).countOverdue(projectIds, today, "Termine");
    }

    @Test
    void countOverdueShouldReturnZeroWhenNoOverdueTasks() {
        LocalDate today = LocalDate.now();

        when(taskRepository.countOverdue(projectIds, today, "Termine")).thenReturn(0L);

        long result = taskRepository.countOverdue(projectIds, today, "Termine");

        assertEquals(0L, result);
    }

    @Test
    void countCreatedBetweenShouldReturnTasksCreatedInPeriod() {
        LocalDateTime start = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        when(taskRepository.countCreatedBetween(projectIds, start, end)).thenReturn(20L);

        long result = taskRepository.countCreatedBetween(projectIds, start, end);

        assertEquals(20L, result);
        verify(taskRepository).countCreatedBetween(projectIds, start, end);
    }

    @Test
    void countByStatusGroupedShouldReturnStatusCounts() {
        List<Object[]> expected = List.of(
                new Object[]{"A faire", 10L},
                new Object[]{"En cours", 8L},
                new Object[]{"Termine", 7L});

        when(taskRepository.countByStatusGrouped(projectIds)).thenReturn(expected);

        List<Object[]> result = taskRepository.countByStatusGrouped(projectIds);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("A faire", result.get(0)[0]);
        assertEquals(10L, result.get(0)[1]);
        assertEquals("Termine", result.get(2)[0]);
        assertEquals(7L, result.get(2)[1]);
    }

    @Test
    void countByStatusGroupedShouldReturnEmptyListWhenNoTasks() {
        when(taskRepository.countByStatusGrouped(projectIds)).thenReturn(List.of());

        List<Object[]> result = taskRepository.countByStatusGrouped(projectIds);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findCreatedDatesBetweenShouldReturnCreatedAtDates() {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        List<LocalDateTime> expected = List.of(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1));

        when(taskRepository.findCreatedDatesBetween(projectIds, start, end)).thenReturn(expected);

        List<LocalDateTime> result = taskRepository.findCreatedDatesBetween(projectIds, start, end);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void findCompletedDatesBetweenShouldReturnCompletedAtDates() {
        LocalDateTime start = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        List<LocalDateTime> expected = List.of(
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(2));

        when(taskRepository.findCompletedDatesBetween(projectIds, start, end)).thenReturn(expected);

        List<LocalDateTime> result = taskRepository.findCompletedDatesBetween(projectIds, start, end);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void countActiveByProjectGroupedShouldReturnProjectTaskCounts() {
        List<Object[]> expected = List.of(
                new Object[]{1L, 15L},
                new Object[]{2L, 10L});

        when(taskRepository.countActiveByProjectGrouped(projectIds)).thenReturn(expected);

        List<Object[]> result = taskRepository.countActiveByProjectGrouped(projectIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0)[0]);
        assertEquals(15L, result.get(0)[1]);
        assertEquals(2L, result.get(1)[0]);
        assertEquals(10L, result.get(1)[1]);
    }

    @Test
    void countByStatusAndProjectGroupedShouldReturnCompletedByProject() {
        List<Object[]> expected = List.of(
                new Object[]{1L, 5L},
                new Object[]{2L, 3L});

        when(taskRepository.countByStatusAndProjectGrouped(projectIds, "Termine")).thenReturn(expected);

        List<Object[]> result = taskRepository.countByStatusAndProjectGrouped(projectIds, "Termine");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0)[0]);
        assertEquals(5L, result.get(0)[1]);
    }

    @Test
    void countByStatusAndProjectGroupedShouldReturnEmptyWhenNoCompletedTasks() {
        when(taskRepository.countByStatusAndProjectGrouped(projectIds, "Termine")).thenReturn(List.of());

        List<Object[]> result = taskRepository.countByStatusAndProjectGrouped(projectIds, "Termine");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void overdueShouldUseStatusNameNotId() {
        LocalDate today = LocalDate.now();

        when(taskRepository.countOverdue(anyList(), any(LocalDate.class), eq("Termine"))).thenReturn(0L);

        taskRepository.countOverdue(projectIds, today, "Termine");

        verify(taskRepository).countOverdue(projectIds, today, "Termine");
    }

    @Test
    void countCompletedBetweenShouldUseCompletedAtField() {
        LocalDateTime start = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        when(taskRepository.countCompletedBetween(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        taskRepository.countCompletedBetween(projectIds, start, end);

        verify(taskRepository).countCompletedBetween(projectIds, start, end);
    }
}
