package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "comment_reactions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "emoji", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommentReaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long reactionId;

    @Column(nullable = false, length = 10)
    private String emoji;

    @ManyToOne
    @JoinColumn(name = "comment_id", nullable = false)
    private TaskComment comment;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
