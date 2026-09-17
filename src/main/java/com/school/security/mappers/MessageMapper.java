package com.school.security.mappers;

import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.Message;
import com.school.security.entities.User;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper message vers DTO de réponse.
 *
 * <p>Fonctionnement constaté (documenté, non modifié) :
 * <ul>
 *   <li>{@code readBy} : liste d'identifiants d'utilisateurs ({code User::getUsersId}) ;
 *       le flux est vide si {@code readBy} est {@code null}.</li>
 *   <li>{@code attachments} : si {@code null}, remplacé par une liste vide ; chaque
 *       élément est mappé par {@code attachmentMapper::toResponse}.</li>
 *   <li>{@code replyTo} : ID du message parent, {@code null} s'il n'y en a pas.</li>
 * </ul>
 */
@Component
public class MessageMapper {

    private final MessageAttachmentMapper
            attachmentMapper;

    public MessageMapper(
            MessageAttachmentMapper attachmentMapper
    ) {
        this.attachmentMapper = attachmentMapper;
    }

    public MessageResponse toResponse(
            Message message
    ) {

        return new MessageResponse(
                message.getMessageId(),

                message.getConversation()
                        .getConversationId(),

                message.getSender()
                        .getUsersId(),

                message.getContent(),

                message.getCreatedAt(),

                (message.getAttachments() == null ? java.util.Collections.<com.school.security.entities.MessageAttachment>emptyList() : message.getAttachments())
                        .stream()
                        .map(
                                attachmentMapper::toResponse
                        )
                        .toList(),

                message.getReplyTo() != null
                        ? message.getReplyTo()
                        .getMessageId()
                        : null,

                (message.getReadBy() == null ? java.util.Collections.<User>emptyList() : message.getReadBy())
                        .stream()
                        .map(
                                User::getUsersId
                        )
                        .toList(),

                message.getDeleted()
        );
    }
}