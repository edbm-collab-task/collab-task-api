package com.school.security.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        LocalDateTime createdAt,
        List<MessageAttachmentResponse> attachments,
        Long replyToId,
        List<Long> readBy,
        Boolean deleted
) {
}