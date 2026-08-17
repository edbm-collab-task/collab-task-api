package com.school.security.dtos.requests;

public record StatusReqDto(
        String name,
        Integer sortOrder,
        Long projectId
) {}