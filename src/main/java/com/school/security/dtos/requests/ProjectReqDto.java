package com.school.security.dtos.requests;

import java.time.LocalDate;

public record ProjectReqDto(
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Long ownerId) {}
