package com.school.security.dtos.responses;

import com.school.security.enums.RoleType;

public record LoginResDto(String accessToken, String refreshToken, RoleType roles) {}
