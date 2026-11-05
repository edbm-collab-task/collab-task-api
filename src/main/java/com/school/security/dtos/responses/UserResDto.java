package com.school.security.dtos.responses;

import com.school.security.enums.Gender;

import java.time.LocalDate;
import java.util.List;

public record UserResDto(
        Long id,
        String firstname,
        String lastname,
        String email,
        String number,
        String direction,
        String job,
        Gender gender,
        Boolean status,
        Boolean isActive,
        LocalDate createdAt,
        String role,
        String codeRole,
        String imagePath) {}
