package com.school.security.dtos.responses;

import com.school.security.enums.PermissionType;
import com.school.security.enums.RoleType;
import java.util.List;

public record LoginResDto(Long userId, RoleType role, String firstname, String lastname, String email, List<String> permissions) {
    public LoginResDto(Long userId, RoleType role, String firstname, String lastname, String email) {
        this(userId, role, firstname, lastname, email, List.of());
    }
}
