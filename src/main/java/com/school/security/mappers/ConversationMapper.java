package com.school.security.mappers;

import com.school.security.dtos.responses.ConversationResponse;
import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.Conversation;
import com.school.security.entities.ConversationMember;
import com.school.security.entities.Message;
import com.school.security.repositories.MessageRepository;
import org.springframework.stereotype.Component;

/**
 * Mapper deConversation vers réponse DTO.
 *
 * <p>Fonctionnement constaté (documenté, non modifié) :
 * <ul>
 *   <li>{@code member} : l'appartenance de l'utilisateur courant ({@code
 *       currentUserId}) dans la conversation, utilisée pour l'état {@code archived},
 *       {@code pinned}, {@code unreadCount}.</li>
 *   <li>{@code lastMessage} : le message le plus récent ( {@code findTopByConversationConversationIdOrderByCreatedAtDesc} ).</li>
 *   <li>la liste des {@code members} utilisateurs est restituée tels quels (ids uniquement).</li>
 * </ul>
 */
@Component
public class ConversationMapper {

    private final MessageRepository messageRepository;

    private final MessageMapper messageMapper;

    public ConversationMapper(
            MessageRepository messageRepository,
            MessageMapper messageMapper
    ) {
        this.messageRepository =
                messageRepository;

        this.messageMapper =
                messageMapper;
    }

    public ConversationResponse toResponse(
            Conversation conversation,
            Long currentUserId
    ) {

        ConversationMember member =
                conversation.getMembers()
                        .stream()
                        .filter(item ->
                                item.getUser()
                                        .getUsersId()
                                        .equals(currentUserId)
                        )
                        .findFirst()
                        .orElse(null);

        /*
         * Récupération du dernier message
         */
        MessageResponse lastMessage =
                messageRepository
                        .findTopByConversationConversationIdOrderByCreatedAtDesc(
                                conversation.getConversationId()
                        )
                        .map(messageMapper::toResponse)
                        .orElse(null);

        return new ConversationResponse(

                conversation.getConversationId(),

                conversation.getType()
                        .name()
                        .toLowerCase(),

                conversation.getName(),

                conversation.getAvatar(),

                conversation.getMembers()
                        .stream()
                        .map(item ->
                                item.getUser()
                                        .getUsersId()
                        )
                        .toList(),

                conversation.getCreatedAt(),

                conversation.getUpdatedAt(),

                member != null
                        && Boolean.TRUE.equals(
                        member.getArchived()
                ),

                member != null
                        && Boolean.TRUE.equals(
                        member.getPinned()
                ),

                member != null
                        ? member.getUnreadCount()
                        : 0,

                /*
                 * Dernier message
                 */
                lastMessage
        );
    }
}