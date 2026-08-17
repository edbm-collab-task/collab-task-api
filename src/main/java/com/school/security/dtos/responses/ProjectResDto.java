package com.school.security.dtos.responses;

import java.time.LocalDate;

public record ProjectResDto(
        Long projectId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isActive,
        Long ownerId,
        String ownerName) {}
