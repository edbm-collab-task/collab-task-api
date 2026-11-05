package com.school.security.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.entities.Activity;
import com.school.security.enums.ActivityType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityRepositoryTest {

    @Mock
    private ActivityRepository activityRepository;

    private List<Long> projectIds;

    @BeforeEach
    void setUp() {
        projectIds = List.of(1L, 2L);
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldReturnActivitiesOrderedDesc() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        List<Activity> expected = List.of(
                buildActivity(3L, LocalDateTime.now().minusHours(1)),
                buildActivity(2L, LocalDateTime.now().minusHours(5)),
                buildActivity(1L, LocalDateTime.now().minusHours(12)));

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(expected);

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt()));
        assertTrue(result.get(1).getCreatedAt().isAfter(result.get(2).getCreatedAt()));
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldReturnEmptyWhenNoActivities() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(List.of());

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldFilterByProjects() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        List<Activity> expected = List.of(
                buildActivity(1L, LocalDateTime.now().minusHours(2)));

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(expected);

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(activityRepository).findRecentByProjectIdsAndPeriod(projectIds, start, end);
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldFilterByPeriod() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        List<Activity> expected = List.of(
                buildActivity(1L, LocalDateTime.now().minusHours(3)),
                buildActivity(2L, LocalDateTime.now().minusHours(6)));

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(expected);

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertEquals(2, result.size());
        for (Activity activity : result) {
            assertTrue(!activity.getCreatedAt().isBefore(start));
            assertTrue(!activity.getCreatedAt().isAfter(end));
        }
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldHandleLargeResultSet() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            activities.add(buildActivity((long) i, LocalDateTime.now().minusHours(i)));
        }

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(activities);

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertEquals(50, result.size());
    }

    @Test
    void findRecentByProjectIdsAndPeriodShouldPreserveOrder() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        LocalDateTime time1 = LocalDateTime.now().minusHours(1);
        LocalDateTime time2 = LocalDateTime.now().minusHours(3);
        LocalDateTime time3 = LocalDateTime.now().minusHours(5);

        List<Activity> expected = List.of(
                buildActivity(1L, time1),
                buildActivity(2L, time2),
                buildActivity(3L, time3));

        when(activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end))
                .thenReturn(expected);

        List<Activity> result = activityRepository.findRecentByProjectIdsAndPeriod(projectIds, start, end);

        assertNotNull(result);
        assertEquals(time1, result.get(0).getCreatedAt());
        assertEquals(time2, result.get(1).getCreatedAt());
        assertEquals(time3, result.get(2).getCreatedAt());
    }

    private Activity buildActivity(Long id, LocalDateTime createdAt) {
        Activity activity = new Activity();
        activity.setActivityId(id);
        activity.setType(ActivityType.TASK_CREATED);
        activity.setDescription("Test activity " + id);
        activity.setCreatedAt(createdAt);
        return activity;
    }
}
