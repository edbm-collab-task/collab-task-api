package com.school.security.dtos.responses;

import com.school.security.enums.PermissionType;
import java.util.List;

public record PermissionResDto(Long id, String name, String description) {}
