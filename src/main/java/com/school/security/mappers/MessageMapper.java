package com.school.security.mappers;

import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.Message;
import com.school.security.entities.User;
import org.springframework.stereotype.Component;

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

                message.getAttachments()
                        .stream()
                        .map(
                                attachmentMapper::toResponse
                        )
                        .toList(),

                message.getReplyTo() != null
                        ? message.getReplyTo()
                        .getMessageId()
                        : null,

                message.getReadBy()
                        .stream()
                        .map(
                                User::getUsersId
                        )
                        .toList(),

                message.getDeleted()
        );
    }
}