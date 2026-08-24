package com.school.security.controllers.api;

import com.school.security.dtos.requests.CommentReqDto;
import com.school.security.dtos.responses.CommentResDto;
import com.school.security.services.contracts.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResDto> getComments(@PathVariable Long taskId) {
        return commentService.getCommentsByTask(taskId);
    }

    @GetMapping("/count")
    public Map<String, Long> countComments(@PathVariable Long taskId) {
        return Map.of("count", commentService.countCommentsByTask(taskId));
    }

    @PostMapping
    public CommentResDto createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentReqDto dto) {
        return commentService.createComment(taskId, dto);
    }

    @PutMapping("/{commentId}")
    public CommentResDto updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReqDto dto) {
        return commentService.updateComment(commentId, dto);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/reactions")
    public CommentResDto toggleReaction(
            @PathVariable Long commentId,
            @RequestParam String emoji) {
        return commentService.toggleReaction(commentId, emoji);
    }
}
