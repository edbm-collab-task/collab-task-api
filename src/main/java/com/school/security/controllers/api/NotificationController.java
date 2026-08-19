package com.school.security.controllers.api;

import com.school.security.dtos.responses.NotificationResDto;
import com.school.security.entities.User;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    @GetMapping
    public List<NotificationResDto> getNotifications() {
        Long currentUserId = getCurrentUserId();
        return notificationService.findByUserId(currentUserId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        Long currentUserId = getCurrentUserId();
        return Map.of("count", notificationService.countUnread(currentUserId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResDto> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long currentUserId = getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }
}
