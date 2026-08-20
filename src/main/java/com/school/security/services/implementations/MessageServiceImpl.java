package com.school.security.services.implementations;

import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.Conversation;
import com.school.security.entities.ConversationMember;
import com.school.security.entities.Message;
import com.school.security.entities.MessageAttachment;
import com.school.security.entities.User;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.mappers.MessageMapper;
import com.school.security.repositories.ConversationMemberRepository;
import com.school.security.repositories.ConversationRepository;
import com.school.security.repositories.MessageRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.MessageService;
import com.school.security.securities.services.FileStorageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MessageServiceImpl
        implements MessageService {

    private final MessageRepository messageRepository;

    private final ConversationRepository
            conversationRepository;

    private final ConversationMemberRepository
            memberRepository;

    private final UserRepository userRepository;

    private final MessageMapper messageMapper;

    private final FileStorageService fileStorageService;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            MessageMapper messageMapper,
            FileStorageService fileStorageService
    ) {
        this.messageRepository =
                messageRepository;

        this.conversationRepository =
                conversationRepository;

        this.memberRepository =
                memberRepository;

        this.userRepository =
                userRepository;

        this.messageMapper =
                messageMapper;

        this.fileStorageService =
                fileStorageService;
    }

    private Long currentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null) {

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
                        .findById(
                                conversationId
                        )
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
            String content,
            Long replyToId,
            List<MultipartFile> attachments
    ) {

        Long currentUserId =
                currentUserId();

        User sender =
                userRepository
                        .findById(
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Utilisateur introuvable."
                                )
                        );

        Conversation conversation =
                conversationRepository
                        .findById(
                                conversationId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Conversation introuvable."
                                )
                        );

        verifyMember(
                conversation,
                currentUserId
        );

        String messageContent =
                content == null
                        ? ""
                        : content.trim();

        boolean hasAttachments =
                attachments != null
                        && attachments.stream()
                        .anyMatch(file ->
                                file != null
                                        && !file.isEmpty()
                        );

        if (messageContent.isEmpty()
                && !hasAttachments) {

            throw new BadRequestException(
                    "Le message ne peut pas être vide."
            );
        }

        Message replyTo = null;

        if (replyToId != null) {

            replyTo =
                    messageRepository
                            .findById(
                                    replyToId
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Message de réponse introuvable."
                                    )
                            );

            if (!replyTo
                    .getConversation()
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
                        .content(messageContent)
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

        /*
         * Sauvegarde des pièces jointes
         */
        if (attachments != null) {

            for (MultipartFile file :
                    attachments) {

                if (file == null ||
                        file.isEmpty()) {
                    continue;
                }

                String url =
                        fileStorageService
                                .saveMessageAttachment(
                                        file
                                );

                MessageAttachment attachment =
                        MessageAttachment.builder()
                                .message(message)
                                .name(
                                        file.getOriginalFilename()
                                                != null
                                                ? file.getOriginalFilename()
                                                : "fichier"
                                )
                                .type(
                                        file.getContentType()
                                                != null
                                                ? file.getContentType()
                                                : "application/octet-stream"
                                )
                                .size(
                                        file.getSize()
                                )
                                .url(url)
                                .build();

                message.getAttachments()
                        .add(attachment);
            }
        }

        message =
                messageRepository.save(
                        message
                );

        /*
         * Incrémentation des messages
         * non lus pour les autres membres
         */
        for (ConversationMember member :
                conversation.getMembers()) {

            if (!member
                    .getUser()
                    .getUsersId()
                    .equals(currentUserId)) {

                Integer unreadCount =
                        member.getUnreadCount();

                member.setUnreadCount(
                        unreadCount == null
                                ? 1
                                : unreadCount + 1
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
                        .findById(
                                messageId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Message introuvable."
                                )
                        );

        if (!message
                .getSender()
                .getUsersId()
                .equals(currentUserId)) {

            throw new BadRequestException(
                    "Vous ne pouvez supprimer que vos propres messages."
            );
        }

        /*
         * Suppression physique des fichiers
         * avant de supprimer les références.
         */
        if (message.getAttachments() != null) {

            for (MessageAttachment attachment :
                    message.getAttachments()) {

                fileStorageService
                        .deleteMessageAttachment(
                                attachment.getUrl()
                        );
            }
        }

        message.setDeleted(true);
        message.setContent("");
        message.getAttachments().clear();

        messageRepository.save(
                message
        );
    }

    private void verifyMember(
            Conversation conversation,
            Long userId
    ) {

        boolean member =
                conversation
                        .getMembers()
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