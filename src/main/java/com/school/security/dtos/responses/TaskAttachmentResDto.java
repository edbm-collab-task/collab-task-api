package com.school.security.dtos.responses;

import java.time.LocalDateTime;

public record TaskAttachmentResDto(
        Long id,
        String name,
        String originalName,
        String contentType,
        Long size,
        LocalDateTime uploadedAt,
        String uploadedByName
) {}
