package com.school.security.services.contracts;

import com.school.security.dtos.responses.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {

    List<MessageResponse> getMessages(
            Long conversationId
    );

    MessageResponse getMessage(
            Long messageId
    );

    MessageResponse sendMessage(
            Long conversationId,
            String content,
            Long replyToId,
            List<MultipartFile> attachments
    );

    void deleteMessage(
            Long messageId
    );
}