package com.school.security.dtos.responses;

import java.time.LocalDateTime;

public record ConversationMemberResponse(
        Long id,
        Long userId,
        ChatUserResponse user,
        LocalDateTime joinedAt,
        LocalDateTime readAt,
        Boolean muted,
        Boolean archived,
        Boolean pinned,
        Integer unreadCount
) {
}