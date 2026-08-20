package com.school.security.dtos.responses;

import java.time.LocalDateTime;

public record ProjectContributorResDto(
        Long id,
        Long projectId,
        Long userId,
        String userName,
        String userEmail,
        LocalDateTime addedAt
) {}
