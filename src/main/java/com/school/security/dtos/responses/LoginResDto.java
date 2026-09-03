package com.school.security.dtos.responses;

import com.school.security.enums.PermissionType;
import com.school.security.enums.RoleType;
import java.util.List;

public record LoginResDto(Long userId, RoleType role, String firstname, String lastname, String email, List<String> permissions, String accessToken, String refreshToken) {
    public LoginResDto(Long userId, RoleType role, String firstname, String lastname, String email, List<String> permissions) {
        this(userId, role, firstname, lastname, email, permissions, null, null);
    }
    public LoginResDto(Long userId, RoleType role, String firstname, String lastname, String email) {
        this(userId, role, firstname, lastname, email, List.of(), null, null);
    }
}
