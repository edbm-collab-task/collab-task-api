package com.school.security.dtos.requests;

import com.school.security.enums.PermissionType;
import java.util.List;

public record RoleReqDto(String name, List<PermissionType> permissions) {}
