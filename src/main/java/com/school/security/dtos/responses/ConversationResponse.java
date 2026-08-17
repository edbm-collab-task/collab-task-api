package com.school.security.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationResponse(
        Long id,
        String type,
        String name,
        String avatar,
        List<Long> memberIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean archived,
        Boolean pinned,
        Integer unreadCount
) {
}