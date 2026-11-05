package com.school.security.mappers;

import com.school.security.dtos.responses.MessageAttachmentResponse;
import com.school.security.entities.Attachment;
import org.springframework.stereotype.Component;

/**
 * Mapper d'une pièce jointe vers le DTO exposé dans un message.
 *
 * <p>Les noms de champs du DTO sont ceux historiques de la messagerie :
 * {@code name} = nom d'origine, {@code type} = type MIME, {@code url} = chemin
 * de stockage. Ils sont conservés tels quels pour ne pas modifier le contrat
 * d'API consommé par le frontend.
 */
@Component
public class AttachmentMapper {

    public MessageAttachmentResponse toResponse(
            Attachment attachment
    ) {
        return new MessageAttachmentResponse(
                attachment.getAttachmentId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSize(),
                attachment.getPath()
        );
    }
}
