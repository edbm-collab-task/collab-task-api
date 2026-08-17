package com.school.security.dtos.responses;

public record MessageAttachmentResponse(
        Long id,
        String name,
        String type,
        Long size,
        String url
) {
}