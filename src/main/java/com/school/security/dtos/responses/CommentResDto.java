package com.school.security.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResDto(
    Long commentId,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    AuthorDto author,
    Long parentId,
    List<CommentResDto> replies,
    List<ReactionDto> reactions
) {
    public record AuthorDto(
        Long userId,
        String firstname,
        String lastname,
        String imagePath
    ) {}

    public record ReactionDto(
        String emoji,
        long count,
        boolean reactedByCurrentUser,
        List<String> usernames
    ) {}
}
