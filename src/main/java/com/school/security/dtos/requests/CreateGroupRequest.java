package com.school.security.dtos.requests;

import java.util.List;

public record CreateGroupRequest(
        String name,
        List<Long> memberIds
) {
}