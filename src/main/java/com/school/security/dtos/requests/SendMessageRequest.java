package com.school.security.dtos.requests;

import java.util.List;

public record SendMessageRequest(
        String content,
        List<AttachmentRequest> attachments,
        Long replyToId
) {

    public record AttachmentRequest(
            String name,
            String type,
            Long size,
            String url
    ) {
    }
}