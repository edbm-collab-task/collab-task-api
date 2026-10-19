package com.school.security.dtos.responses;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> items,
        boolean hasMore
) {
}
