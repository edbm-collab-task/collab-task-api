package com.school.security.controllers.api;

import com.school.security.dtos.responses.MessageResponse;
import com.school.security.services.contracts.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/conversations")
/**
 * Contrôleur HTTP des messages de conversation.
 *
 * <p>Il expose les opérations de lecture, d'envoi et de suppression des
 * messages associés à une conversation donnée. La majorité des contrôles
 * métier, notamment l'appartenance à la conversation et la cohérence des
 * réponses, est déléguée au service.
 *
 * <p>Ce contrôleur joue aussi un rôle de relais temps réel : après l'envoi
 * d'un message, il diffuse la réponse via WebSocket à la destination
 * {@code /topic/conversations/{conversationId}}.
 */
public class MessageController {

    private final MessageService messageService;

    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Retourne les messages d'une conversation.
     *
     * <p>La vérification de l'appartenance de l'utilisateur courant à la
     * conversation est effectuée dans le service avant l'accès aux messages.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                messageService.getMessages(
                        conversationId
                )
        );
    }

    /**
     * Envoie un message dans une conversation.
     *
     * <p>La requête accepte un contenu texte, une référence de réponse
     * éventuelle et une liste de pièces jointes multipart. Le service
     * construit le message, applique les contrôles d'appartenance et gère
     * la persistance des fichiers associés.
     *
     * <p>Après la création, la réponse du service est diffusée en temps réel
     * à tous les abonnés de la conversation via WebSocket.
     */
    @PostMapping(
            value = "/{conversationId}/messages",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MessageResponse> sendMessage(

            @PathVariable Long conversationId,

            @RequestParam(
                    required = false
            )
            String content,

            @RequestParam(
                    required = false
            )
            Long replyToId,

            @RequestPart(
                    value = "attachments",
                    required = false
            )
            List<MultipartFile> attachments
    ) {

        MessageResponse response =
                messageService.sendMessage(
                        conversationId,
                        content,
                        replyToId,
                        attachments
                );

        /*
         * Diffusion temps réel du message créé à tous les clients abonnés
         * à cette conversation.
         *
         * Destination :
         *
         * /topic/conversations/{conversationId}
         */
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                response
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retourne un message précis.
     *
     * <p>Le service vérifie l'accès à la conversation d'origine du message
     * avant de renvoyer le DTO correspondant.
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable Long messageId
    ) {

        return ResponseEntity.ok(
                messageService.getMessage(
                        messageId
                )
        );
    }

    /**
     * Supprime un message.
     *
     * <p>La suppression effective est gérée dans le service, qui applique la
     * règle métier de suppression par l'auteur et traite également les
     * pièces jointes associées.
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId
    ) {

        messageService.deleteMessage(
                messageId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}