package com.school.security.services.implementations;

import com.school.security.dtos.requests.SendMessageRequest;
import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.*;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.mappers.MessageMapper;
import com.school.security.repositories.*;
import com.school.security.services.contracts.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MessageServiceImpl
        implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository
            attachmentRepository;
    private final ConversationRepository
            conversationRepository;
    private final ConversationMemberRepository
            memberRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            MessageAttachmentRepository attachmentRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            MessageMapper messageMapper
    ) {
        this.messageRepository =
                messageRepository;
        this.attachmentRepository =
                attachmentRepository;
        this.conversationRepository =
                conversationRepository;
        this.memberRepository =
                memberRepository;
        this.userRepository =
                userRepository;
        this.messageMapper =
                messageMapper;
    }

    private Long currentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            throw new BadRequestException(
                    "Utilisateur non authentifié."
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable."
                        )
                )
                .getUsersId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Conversation introuvable."
                                )
                        );

        verifyMember(
                conversation,
                currentUserId
        );

        return messageRepository
                .findByConversationConversationIdOrderByCreatedAtAsc(
                        conversationId
                )
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getMessage(
            Long messageId
    ) {

        Message message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message introuvable."
                                )
                        );

        verifyMember(
                message.getConversation(),
                currentUserId()
        );

        return messageMapper.toResponse(
                message
        );
    }

    @Override
    public MessageResponse sendMessage(
            Long conversationId,
            SendMessageRequest request
    ) {

        Long currentUserId =
                currentUserId();

        User sender =
                userRepository
                        .findById(currentUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        Conversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Conversation introuvable."
                                )
                        );

        verifyMember(
                conversation,
                currentUserId
        );

        String content =
                request.content() == null
                        ? ""
                        : request.content().trim();

        boolean hasAttachments =
                request.attachments() != null
                        && !request.attachments()
                        .isEmpty();

        if (content.isEmpty()
                && !hasAttachments) {
            throw new BadRequestException(
                    "Le message ne peut pas être vide."
            );
        }

        Message replyTo = null;

        if (request.replyToId() != null) {

            replyTo =
                    messageRepository
                            .findById(
                                    request.replyToId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Message de réponse introuvable."
                                    )
                            );

            if (!replyTo.getConversation()
                    .getConversationId()
                    .equals(conversationId)) {
                throw new BadRequestException(
                        "Le message cité n'appartient pas à cette conversation."
                );
            }
        }

        Message message =
                Message.builder()
                        .conversation(conversation)
                        .sender(sender)
                        .content(content)
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .replyTo(replyTo)
                        .readBy(
                                new ArrayList<>(
                                        List.of(sender)
                                )
                        )
                        .deleted(false)
                        .build();

        if (request.attachments() != null) {

            for (
                    SendMessageRequest.AttachmentRequest
                            requestAttachment
                    : request.attachments()
            ) {

                MessageAttachment attachment =
                        MessageAttachment.builder()
                                .message(message)
                                .name(
                                        requestAttachment.name()
                                )
                                .type(
                                        requestAttachment.type()
                                )
                                .size(
                                        requestAttachment.size()
                                )
                                .url(
                                        requestAttachment.url()
                                )
                                .build();

                message.getAttachments()
                        .add(attachment);
            }
        }

        message =
                messageRepository.save(
                        message
                );

        for (
                ConversationMember member
                : conversation.getMembers()
        ) {

            if (!member.getUser()
                    .getUsersId()
                    .equals(currentUserId)) {

                member.setUnreadCount(
                        member.getUnreadCount() + 1
                );
            }
        }

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        conversationRepository.save(
                conversation
        );

        return messageMapper.toResponse(
                message
        );
    }

    @Override
    public void deleteMessage(
            Long messageId
    ) {

        Long currentUserId =
                currentUserId();

        Message message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message introuvable."
                                )
                        );

        if (!message.getSender()
                .getUsersId()
                .equals(currentUserId)) {
            throw new BadRequestException(
                    "Vous ne pouvez supprimer que vos propres messages."
            );
        }

        message.setDeleted(true);
        message.setContent("");
        message.getAttachments().clear();

        messageRepository.save(message);
    }

    private void verifyMember(
            Conversation conversation,
            Long userId
    ) {

        boolean member =
                conversation.getMembers()
                        .stream()
                        .anyMatch(item ->
                                item.getUser()
                                        .getUsersId()
                                        .equals(userId)
                        );

        if (!member) {
            throw new BadRequestException(
                    "Vous ne faites pas partie de cette conversation."
            );
        }
    }
}