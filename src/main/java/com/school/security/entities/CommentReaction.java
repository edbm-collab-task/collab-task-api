package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "comment_reactions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "emoji", "user_id"}))
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

    public Long getReactionId() {
        return reactionId;
    }

    public void setReactionId(Long reactionId) {
        this.reactionId = reactionId;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public TaskComment getComment() {
        return comment;
    }

    public void setComment(TaskComment comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}