package com.school.security.controllers.api;

import com.school.security.dtos.requests.CommentReqDto;
import com.school.security.dtos.responses.CommentResDto;
import com.school.security.services.contracts.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur de gestion des commentaires d'une tâche, sous le préfixe
 * {@code /tasks/{taskId}/comments}.
 *
 * <p>L'intégralité du traitement est déléguée à {@link CommentService}
 * (séparation couche HTTP / logique métier). La plupart des règles métier
 * visibles ici — contrôle de l'auteur sur la modification/suppression,
 * validation du DTO via bean validation, réactivation automatique du projet
 * à l'ajout d'un commentaire — sont réellement appliquées dans le service.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : les endpoints reposent sur
 *       la seule règle globale {@code anyRequest().authenticated()} de
 *       {@code SecurityConfig} ; aucun contrôle d'appartenance au projet ou
 *       de rôle n'est appliqué ;</li>
 *   <li>pour les opérations ciblées sur un commentaire (modification,
 *       suppression, réaction), la variable de chemin {@code taskId} n'est
 *       PAS utilisée par le service : seul {@code commentId} est exploité,
 *       sans vérifier que le commentaire appartient bien à la tâche du
 *       chemin ;</li>
 *   <li>aucune notification n'est déclenchée à la création d'un commentaire.</li>
 * </ul>
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Liste les commentaires racines d'une tâche ({@code GET /tasks/{taskId}/comments}),
     * du plus récent au plus ancien.
     *
     * <p>Délègue à {@code CommentService.getCommentsByTask(taskId)} :
     * seuls les commentaires sans parent sont retournés à la racine, les
     * réponses étant imbriquées ({@code replies}) et triées chronologiquement.
     * L'état des réactions est calculé par rapport à l'utilisateur courant.
     */
    @GetMapping
    public List<CommentResDto> getComments(@PathVariable Long taskId) {
        return commentService.getCommentsByTask(taskId);
    }

    /**
     * Nombre total de commentaires d'une tâche ({@code GET /tasks/{taskId}/comments/count}).
     *
     * <p>Retourne une map {@code {"count": <nombre>}}. Le comptage inclut
     * TOUS les commentaires de la tâche (commentaires racines ET réponses),
     * via {@code CommentService.countCommentsByTask(taskId)}.
     */
    @GetMapping("/count")
    public Map<String, Long> countComments(@PathVariable Long taskId) {
        return Map.of("count", commentService.countCommentsByTask(taskId));
    }

    /**
     * Création d'un commentaire ({@code POST /tasks/{taskId}/comments}).
     *
     * <p>Le DTO (corps) est validé par bean validation ({@code @Valid}) ;
     * le champ {@code parentCommentId}, s'il est renseigné, fait du nouveau
     * commentaire une réponse à un commentaire existant (validé dans le
     * service). L'auteur est l'utilisateur courant (résolu via le
     * {@code SecurityContext} dans le service).
     *
     * <p>Effet de bord appliqué par le service : la création d'un commentaire
     * réactive automatiquement un projet archivé si sa tâche appartient à ce
     * projet.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentResDto createComment(
            @PathVariable Long taskId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentCommentId", required = false) Long parentCommentId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        CommentReqDto dto = new CommentReqDto(content, parentCommentId);
        return commentService.createComment(taskId, dto, file);
    }

    /**
     * Modification du contenu d'un commentaire ({@code PUT /tasks/{taskId}/comments/{commentId}}).
     *
     * <p>Le service vérifie que l'utilisateur courant est bien l'AUTEUR du
     * commentaire (sinon exception {@code EntityException}). Seul le
     * contenu est mis à jour.
     *
     * <p>NOTE : {@code taskId} n'est pas utilisé par le service, seule
     * {@code commentId} est exploité (voir le point d'attention de la classe).
     */
    @PutMapping("/{commentId}")
    public CommentResDto updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReqDto dto) {
        return commentService.updateComment(commentId, dto);
    }

    /**
     * Suppression d'un commentaire ({@code DELETE /tasks/{taskId}/comments/{commentId}}).
     *
     * <p>Comme pour la modification, le service vérifie que l'utilisateur
     * courant est l'auteur (sinon exception {@code EntityException}). La
     * réponse est 204 No Content en cas de succès (aucun corps).
     *
     * <p>NOTE : {@code taskId} n'est pas utilisé par le service, seule
     * {@code commentId} est exploité.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Ajout/retrait d'une réaction (emoji) sur un commentaire
     * ({@code POST /tasks/{taskId}/comments/{commentId}/reactions}).
     *
     * <p>L'emoji est passé en query parameter (pas dans le corps). L'opération
     * est un TOGGLE géré par le service : si l'utilisateur courant a déjà
     * réagi avec cet emoji, la réaction est retirée ; sinon elle est ajoutée.
     * Retourne le commentaire avec les réactions regroupées par emoji.
     *
     * <p>NOTE : {@code taskId} n'est pas utilisé par le service, seule
     * {@code commentId} est exploité. Aucune restriction d'auteur n'est
     * appliquée pour les réactions.
     */
    @PostMapping("/{commentId}/reactions")
    public CommentResDto toggleReaction(
            @PathVariable Long commentId,
            @RequestParam String emoji) {
        return commentService.toggleReaction(commentId, emoji);
    }
}
