package com.school.security.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "conversation_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_user",
                        columnNames = {
                                "conversation_id",
                                "user_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_member_id")
    private Long conversationMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "conversation_id",
            nullable = false
    )
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean muted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean archived = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;
}