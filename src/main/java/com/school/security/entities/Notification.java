package com.school.security.entities;

import com.school.security.enums.NotificationType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    protected Long notificationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private Boolean isRead = false;

    private LocalDateTime createdAt;

    private Long projectId;

    private Long taskId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
