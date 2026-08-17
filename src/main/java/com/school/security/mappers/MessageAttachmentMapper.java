package com.school.security.mappers;

import com.school.security.dtos.responses.MessageAttachmentResponse;
import com.school.security.entities.MessageAttachment;
import org.springframework.stereotype.Component;

@Component
public class MessageAttachmentMapper {

    public MessageAttachmentResponse toResponse(
            MessageAttachment attachment
    ) {
        return new MessageAttachmentResponse(
                attachment.getAttachmentId(),
                attachment.getName(),
                attachment.getType(),
                attachment.getSize(),
                attachment.getUrl()
        );
    }
}
