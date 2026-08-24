package com.school.security.services.contracts;

import com.school.security.dtos.requests.CommentReqDto;
import com.school.security.dtos.responses.CommentResDto;
import java.util.List;

public interface CommentService {
    List<CommentResDto> getCommentsByTask(Long taskId);
    long countCommentsByTask(Long taskId);
    CommentResDto createComment(Long taskId, CommentReqDto dto);
    CommentResDto updateComment(Long commentId, CommentReqDto dto);
    void deleteComment(Long commentId);
    CommentResDto toggleReaction(Long commentId, String emoji);
}
