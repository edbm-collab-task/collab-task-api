package com.school.security.dtos.requests;

import com.school.security.enums.RoleType;

public record AttachRoleReqDto(String email, RoleType role) {}
