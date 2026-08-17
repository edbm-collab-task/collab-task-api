package com.school.security.entities;

import com.school.security.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    @Column(nullable = false)
    private String name;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ConversationMember> members = new ArrayList<>();

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
}