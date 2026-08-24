package com.school.security.repositories;

import com.school.security.entities.CommentReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentCommentIdAndEmojiAndUserUsersId(Long commentId, String emoji, Long userId);
    void deleteByCommentCommentIdAndEmojiAndUserUsersId(Long commentId, String emoji, Long userId);
}
