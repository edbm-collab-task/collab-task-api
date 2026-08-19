package com.school.security.repositories;

import com.school.security.entities.Notification;
import com.school.security.enums.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserUsersIdOrderByCreatedAtDesc(Long userId);
    long countByUserUsersIdAndIsReadFalse(Long userId);
    void deleteByUserUsersId(Long userId);
    boolean existsByUserUsersIdAndTypeAndProjectIdAndTaskId(Long userId, NotificationType type, Long projectId, Long taskId);
}
