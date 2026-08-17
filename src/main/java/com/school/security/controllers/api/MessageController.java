package com.school.security.controllers.api;

import com.school.security.dtos.requests.SendMessageRequest;
import com.school.security.dtos.responses.MessageResponse;
import com.school.security.services.contracts.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(
            MessageService messageService
    ) {
        this.messageService = messageService;
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId
    ) {
        return ResponseEntity.ok(
                messageService.getMessages(conversationId)
        );
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        messageService.sendMessage(
                                conversationId,
                                request
                        )
                );
    }

    @GetMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable Long messageId
    ) {
        return ResponseEntity.ok(
                messageService.getMessage(messageId)
        );
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId
    ) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}