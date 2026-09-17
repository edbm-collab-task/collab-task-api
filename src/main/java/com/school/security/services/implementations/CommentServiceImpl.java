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

/**
 * Implémentation du service de commentaires de tâche (avec réponses imbriquées
 * et réactions emoji).
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>l'utilisateur courant est résolu depuis le {@code SecurityContext} par
 *       email ; {@link #getCurrentUser} lève une {@code EntityException}
 *       ("User not found") si l'email n'existe pas, alors que
 *       {@link #getCurrentUserId} renvoie silencieusement {@code null} en cas
 *       d'échec (sert uniquement à marquer "réaction de l'utilisateur
 *       courant") ;</li>
 *   <li>la modification et la suppression d'un commentaire sont réservées à son
 *       auteur : aucun rôle (admin/super admin) ne peut les contourner ;</li>
 *   <li>la suppression est physique ({@code commentRepository.delete}) ;</li>
 *   <li>la création d'un commentaire réactive automatiquement le projet parent
 *       s'il était archivé (voir {@link #autoUnarchiveProject}) ;</li>
 *   <li>aucune notification ni diffusion WebSocket n'est émise par ce service.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final TaskCommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    /**
     * Retourne uniquement les commentaires racines (sans parent), triés du plus
     * récent au plus ancien ; les réponses sont imbriquées récursivement dans le
     * DTO. {@code currentUserId} (nullable) sert à indiquer si l'utilisateur
     * courant a réagi.
     */
    @Override
    public List<CommentResDto> getCommentsByTask(Long taskId) {
        List<TaskComment> rootComments = commentRepository
                .findByTaskTaskIdAndParentCommentIsNullOrderByCreatedAtDesc(taskId);
        Long currentUserId = getCurrentUserId();
        return rootComments.stream()
                .map(c -> toDto(c, currentUserId))
                .collect(Collectors.toList());
    }

    /** Compte tous les commentaires de la tâche (racines et réponses). */
    @Override
    public long countCommentsByTask(Long taskId) {
        return commentRepository.countByTaskTaskId(taskId);
    }

    /**
     * Crée un commentaire (ou une réponse si {@code parentCommentId} est fourni)
     * attribué à l'utilisateur courant, puis réactive le projet parent s'il
     * était archivé.
     */
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

    /** Met à jour le contenu ; seul l'auteur du commentaire est autorisé. */
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

    /** Supprime physiquement le commentaire ; seul l'auteur est autorisé. */
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

    /**
     * Bascule la réaction de l'utilisateur courant pour un emoji : l'ajoute si
     * absente, la retire sinon. Retourne le commentaire mis à jour.
     */
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

    /**
     * Convertit récursivement un commentaire et ses réponses ; les réponses sont
     * triées par date de création croissante.
     */
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

    /**
     * Regroupe les réactions par emoji en conservant l'ordre d'insertion et
     * indique pour chaque groupe si l'utilisateur courant a réagi.
     */
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

    /**
     * Retourne l'id de l'utilisateur courant, ou {@code null} si
     * l'authentification est absente ou l'email inconnu (aucune exception).
     */
    private Long getCurrentUserId() {
        try {
            String email = SecurityUtils.getCurrentUsername();
            return userRepository.findByEmail(email).map(User::getUsersId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retourne l'utilisateur courant ; lève une {@code EntityException} si
     * l'email résolu n'est pas trouvé.
     */
    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityException("User not found"));
    }

    /**
     * Réactive le projet parent si son flag actif vaut explicitement
     * {@code Boolean.FALSE} (un {@code null} n'est pas traité).
     */
    private void autoUnarchiveProject(Task task) {
        var project = task.getProject();
        if (Boolean.FALSE.equals(project.getIsActive())) {
            project.setIsActive(true);
            projectRepository.save(project);
        }
    }
}
