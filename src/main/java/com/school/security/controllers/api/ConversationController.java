package com.school.security.controllers.api;

import com.school.security.dtos.requests.AddMembersRequest;
import com.school.security.dtos.requests.CreateConversationRequest;
import com.school.security.dtos.requests.CreateGroupRequest;
import com.school.security.dtos.responses.*;
import com.school.security.services.contracts.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService
    ) {
        this.conversationService =
                conversationService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<ChatUserResponse>>
    getUsers() {

        return ResponseEntity.ok(
                conversationService.getChatUsers()
        );
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>>
    getConversations(
            @RequestParam(
                    defaultValue = "false"
            )
            boolean archived
    ) {

        return ResponseEntity.ok(
                conversationService
                        .getConversations(archived)
        );
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse>
    getConversation(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService
                        .getConversation(
                                conversationId
                        )
        );
    }

    @PostMapping("/private")
    public ResponseEntity<ConversationResponse>
    createPrivate(
            @RequestBody
            CreateConversationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        conversationService
                                .createPrivateConversation(
                                        request
                                )
                );
    }

    @PostMapping("/group")
    public ResponseEntity<ConversationResponse>
    createGroup(
            @RequestBody
            CreateGroupRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        conversationService
                                .createGroup(
                                        request
                                )
                );
    }

    @GetMapping("/{conversationId}/members")
    public ResponseEntity<
            List<ConversationMemberResponse>
            >
    getMembers(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.getMembers(
                        conversationId
                )
        );
    }

    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ConversationResponse>
    addMembers(
            @PathVariable Long conversationId,
            @RequestBody AddMembersRequest request
    ) {

        return ResponseEntity.ok(
                conversationService.addMembers(
                        conversationId,
                        request
                )
        );
    }

    @DeleteMapping(
            "/{conversationId}/members/{userId}"
    )
    public ResponseEntity<Void> removeMember(
            @PathVariable Long conversationId,
            @PathVariable Long userId
    ) {

        conversationService.removeMember(
                conversationId,
                userId
        );

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long conversationId
    ) {

        conversationService.leaveGroup(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId
    ) {

        conversationService.markAsRead(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/{conversationId}/pin")
    public ResponseEntity<ConversationResponse>
    togglePin(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.togglePin(
                        conversationId
                )
        );
    }

    @PatchMapping("/{conversationId}/archive")
    public ResponseEntity<ConversationResponse>
    toggleArchive(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.toggleArchive(
                        conversationId
                )
        );
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void>
    deleteConversation(
            @PathVariable Long conversationId
    ) {

        conversationService.deleteConversation(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }
}