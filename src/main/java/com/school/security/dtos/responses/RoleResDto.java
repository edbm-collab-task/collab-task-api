package com.school.security.dtos.responses;

import com.school.security.enums.PermissionType;
import com.school.security.enums.RoleType;
import java.util.List;

public record RoleResDto(Long id, RoleType name, List<PermissionType> permissions) {}
