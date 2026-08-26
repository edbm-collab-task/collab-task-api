package com.school.security.services.implementations;

import com.school.security.dtos.requests.CommentReqDto;
import com.school.security.dtos.responses.CommentResDto;
import com.school.security.entities.CommentReaction;
import com.school.security.entities.Task;
import com.school.security.entities.TaskComment;
import com.school.security.entities.User;
import com.school.security.exceptions.EntityException;
import com.school.security.repositories.CommentReactionRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.TaskCommentRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.CommentService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final TaskCommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Override
    public List<CommentResDto> getCommentsByTask(Long taskId) {
        List<TaskComment> rootComments = commentRepository
                .findByTaskTaskIdAndParentCommentIsNullOrderByCreatedAtDesc(taskId);
        Long currentUserId = getCurrentUserId();
        return rootComments.stream()
                .map(c -> toDto(c, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public long countCommentsByTask(Long taskId) {
        return commentRepository.countByTaskTaskId(taskId);
    }

    @Override
    public CommentResDto createComment(Long taskId, CommentReqDto dto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityException("Task not found"));
        User author = getCurrentUser();

        TaskComment comment = new TaskComment();
        comment.setContent(dto.content());
        comment.setTask(task);
        comment.setAuthor(author);

        if (dto.parentCommentId() != null) {
            TaskComment parent = commentRepository.findById(dto.parentCommentId())
                    .orElseThrow(() -> new EntityException("Parent comment not found"));
            comment.setParentComment(parent);
        }

        TaskComment saved = commentRepository.save(comment);

        autoUnarchiveProject(task);

        return toDto(saved, author.getUsersId());
    }

    @Override
    public CommentResDto updateComment(Long commentId, CommentReqDto dto) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityException("Comment not found"));
        User currentUser = getCurrentUser();

        if (!comment.getAuthor().getUsersId().equals(currentUser.getUsersId())) {
            throw new EntityException("Vous ne pouvez modifier que vos propres commentaires");
        }

        comment.setContent(dto.content());
        TaskComment saved = commentRepository.save(comment);
        return toDto(saved, currentUser.getUsersId());
    }

    @Override
    public void deleteComment(Long commentId) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityException("Comment not found"));
        User currentUser = getCurrentUser();

        if (!comment.getAuthor().getUsersId().equals(currentUser.getUsersId())) {
            throw new EntityException("Vous ne pouvez supprimer que vos propres commentaires");
        }

        commentRepository.delete(comment);
    }

    @Override
    public CommentResDto toggleReaction(Long commentId, String emoji) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityException("Comment not found"));
        User currentUser = getCurrentUser();

        var existing = reactionRepository
                .findByCommentCommentIdAndEmojiAndUserUsersId(commentId, emoji, currentUser.getUsersId());

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
        } else {
            CommentReaction reaction = new CommentReaction();
            reaction.setEmoji(emoji);
            reaction.setComment(comment);
            reaction.setUser(currentUser);
            reactionRepository.save(reaction);
        }

        return toDto(comment, currentUser.getUsersId());
    }

    private CommentResDto toDto(TaskComment comment, Long currentUserId) {
        List<CommentResDto> replies = comment.getReplies().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(r -> toDto(r, currentUserId))
                .collect(Collectors.toList());

        List<CommentResDto.ReactionDto> reactions = groupReactions(comment.getReactions(), currentUserId);

        return new CommentResDto(
                comment.getCommentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                new CommentResDto.AuthorDto(
                        comment.getAuthor().getUsersId(),
                        comment.getAuthor().getFirstname(),
                        comment.getAuthor().getLastname(),
                        comment.getAuthor().getImagePath()
                ),
                comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null,
                replies,
                reactions
        );
    }

    private List<CommentResDto.ReactionDto> groupReactions(List<CommentReaction> reactions, Long currentUserId) {
        Map<String, List<CommentReaction>> grouped = new LinkedHashMap<>();
        for (CommentReaction r : reactions) {
            grouped.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>()).add(r);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<CommentReaction> list = entry.getValue();
                    List<String> usernames = list.stream()
                            .map(r -> r.getUser().getFirstname() + " " + r.getUser().getLastname())
                            .collect(Collectors.toList());
                    boolean reactedByCurrentUser = list.stream()
                            .anyMatch(r -> r.getUser().getUsersId().equals(currentUserId));
                    return new CommentResDto.ReactionDto(
                            entry.getKey(),
                            list.size(),
                            reactedByCurrentUser,
                            usernames
                    );
                })
                .collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        try {
            String email = SecurityUtils.getCurrentUsername();
            return userRepository.findByEmail(email).map(User::getUsersId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityException("User not found"));
    }

    private void autoUnarchiveProject(Task task) {
        var project = task.getProject();
        if (Boolean.FALSE.equals(project.getIsActive())) {
            project.setIsActive(true);
            projectRepository.save(project);
        }
    }
}
