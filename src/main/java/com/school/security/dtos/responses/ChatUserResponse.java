package com.school.security.dtos.responses;

public record ChatUserResponse(
        Long id,
        String firstname,
        String lastname,
        String email,
        String avatar,
        Boolean online
) {
}