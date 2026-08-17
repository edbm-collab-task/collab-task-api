package com.school.security.dtos.responses;

public record MessageUserResponse(
        Long id,
        String firstname,
        String lastname,
        String email,
        String avatar,
        Boolean online
) {
}