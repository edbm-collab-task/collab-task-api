package com.school.security.dtos.responses;

import com.school.security.enums.PermissionType;
import java.util.List;

public record LoginResDto(Long userId, String role, String codeRole, String firstname, String lastname, String email, List<PermissionSummary> permissions, String accessToken, String refreshToken, Boolean firstLogin) {
    public LoginResDto(Long userId, String role, String codeRole, String firstname, String lastname, String email, List<PermissionSummary> permissions, String accessToken, String refreshToken) {
        this(userId, role, codeRole, firstname, lastname, email, permissions, accessToken, refreshToken, false);
    }
    public LoginResDto(Long userId, String role, String codeRole, String firstname, String lastname, String email, List<PermissionSummary> permissions) {
        this(userId, role, codeRole, firstname, lastname, email, permissions, null, null, false);
    }
    public LoginResDto(Long userId, String role, String codeRole, String firstname, String lastname, String email) {
        this(userId, role, codeRole, firstname, lastname, email, List.of(), null, null, false);
    }
}
