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
         * Envoi temps réel à tous les utilisateurs
         * abonnés à cette conversation.
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