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

/**
 * Implémentation du service de messages d'une conversation.
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>l'utilisateur courant est résolu depuis le {@code SecurityContext}
 *       par email ; absence d'authentification → {@code BadRequestException},
 *       email inconnu → {@code ResourceNotFoundException} ;</li>
 *   <li>l'appartenance à la conversation est vérifiée via
 *       {@link #verifyMember} pour la lecture et l'envoi ;</li>
 *   <li>il n'existe ici AUCUNE méthode de modification/édition de message
 *       (le contrat {@code MessageService} n'expose que lecture, envoi et
 *       suppression) ;</li>
 *   <li>ce service n'émet AUCUNE notification et ne réalise AUCUNE diffusion
 *       WebSocket (aucune dépendance à un broker ou à un repository de
 *       notifications) ;</li>
 *   <li>la suppression d'un message combine une suppression PHYSIQUE des
 *       fichiers de pièces jointes et une suppression LOGIQUE du message
 *       (drapeau {@code deleted}, contenu vidé).</li>
 * </ul>
 */
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

    /**
     * Résout l'identifiant de l'utilisateur authentifié courant par email.
     *
     * <p>Ne teste pas {@code isAuthenticated()} : seul le nom de
     * l'authentification est vérifié. Lève {@code BadRequestException} si
     * aucune authentification/nom, {@code ResourceNotFoundException} si
     * l'email n'existe pas en base.
     */
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

    /**
     * Liste les messages d'une conversation, dans l'ordre chronologique
     * croissant ({@code createdAt ASC}), après vérification que l'utilisateur
     * courant est membre de la conversation.
     */
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

    /**
     * Récupère un message par identifiant.
     *
     * <p>L'appartenance est vérifiée à partir de la conversation du message
     * ({@code message.getConversation()}) ; un message inexistant lève
     * {@code ResourceNotFoundException}, un non-membre
     * {@code BadRequestException}.
     */
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

    /**
     * Envoie un message dans une conversation.
     *
     * <p>Validations et comportements :
     * <ul>
     *   <li>expéditeur et conversation doivent exister, et l'expéditeur doit
     *       être membre de la conversation ;</li>
     *   <li>le contenu est {@code trim()}é ; un message sans texte est refusé
     *       seulement s'il n'a AUCUNE pièce jointe non vide (un message
     *       composé uniquement de fichiers est donc accepté) ;</li>
     *   <li>si {@code replyToId} est fourni, le message cité doit exister et
     *       appartenir à la MÊME conversation, sinon
     *       {@code BadRequestException} ;</li>
     *   <li>le message est créé avec {@code deleted=false} et
     *       {@code readBy} initialisé à l'expéditeur uniquement ;</li>
     *   <li>chaque pièce jointe est d'abord écrite physiquement via
     *       {@link FileStorageService#saveMessageAttachment}, puis référencée
     *       dans {@code message.attachments} (cascade {@code ALL} +
     *       {@code orphanRemoval}) ; nom/type ont des valeurs de repli si
     *       absents ;</li>
     *   <li>le compteur de non-lus de chaque AUTRE membre est incrémenté
     *       (gestion du {@code null} → 1) ;</li>
     *   <li>{@code updatedAt} de la conversation est rafraîchi ;</li>
     *   <li>aucune notification ni diffusion WebSocket n'est déclenchée
     *       ici.</li>
     * </ul>
     */
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

        // Un message vide n'est autorisé que s'il porte au moins une pièce
        // jointe réellement non vide.
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

            // Le message cité doit appartenir à la même conversation.
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
                        // Expéditeur marqué comme l'ayant déjà lu.
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

                // Écriture physique du fichier ; l'URL renvoyée est du type
                // "uploads/messages/<uuid>".
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

    /**
     * Supprime un message.
     *
     * <p>Seul l'EXPÉDITEUR du message peut le supprimer ; l'appartenance à la
     * conversation n'est pas revérifiée ici. La suppression combine deux
     * mécanismes :
     * <ul>
     *   <li>suppression PHYSIQUE de chaque fichier de pièce jointe via
     *       {@link FileStorageService#deleteMessageAttachment} ;</li>
     *   <li>suppression LOGIQUE du message : {@code deleted=true}, contenu
     *       vidé, et {@code attachments.clear()} qui, via
     *       {@code orphanRemoval}, supprime les lignes de pièces jointes en
     *       base. Le message lui-même reste présent (tombstone).</li>
     * </ul>
     */
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

        // Marqueur de suppression logique : le message est conservé mais vidé.
        message.setDeleted(true);
        message.setContent("");
        message.getAttachments().clear();

        messageRepository.save(
                message
        );
    }

    /**
     * Vérifie que l'utilisateur fait partie de la conversation en parcourant
     * la collection {@code conversation.getMembers()} déjà chargée. Lève
     * {@code BadRequestException} sinon.
     */
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