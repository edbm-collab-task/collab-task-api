package com.school.security.dtos.requests;

public record AttachmentRequest(
        String name,
        String type,
        Long size,
        String url
) {
}