package com.school.security.services.contracts;
import com.school.security.dtos.requests.SendMessageRequest;
import com.school.security.dtos.responses.MessageResponse;

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
            SendMessageRequest request
    );

    void deleteMessage(
            Long messageId
    );
}