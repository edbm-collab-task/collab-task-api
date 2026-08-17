package com.school.security.dtos.requests;

import java.util.List;

public record AddMembersRequest(
        List<Long> memberIds
) {
}