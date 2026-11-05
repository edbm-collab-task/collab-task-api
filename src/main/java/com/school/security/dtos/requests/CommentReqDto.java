package com.school.security.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentReqDto(
    @NotBlank String content,
    Long parentCommentId
) {}
