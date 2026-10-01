package com.school.security.services.contracts;

import com.school.security.dtos.responses.MessagePageResponse;
import com.school.security.dtos.responses.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {

    /**
     * Page de messages d'une conversation, du plus ancien au plus récent.
     *
     * @param conversationId conversation interrogée
     * @param limit           nombre maximal de messages renvoyés
     * @param before          identifiant du curseur : seuls les messages
     *                        d'identifiant strictement inférieur sont
     *                        renvoyés. {@code null} pour la page la plus
     *                        récente.
     */
    MessagePageResponse getMessages(
            Long conversationId,
            int limit,
            Long before
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
