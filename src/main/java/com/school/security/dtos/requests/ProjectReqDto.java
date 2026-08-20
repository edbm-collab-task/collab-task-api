package com.school.security.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ProjectReqDto(
        @NotBlank(message = "Title is required") String title,
        String description,
        LocalDate startDate,
        LocalDate endDate) {}
