package com.school.security.mappers;

import com.school.security.dtos.responses.ConversationResponse;
import com.school.security.entities.Conversation;
import com.school.security.entities.ConversationMember;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

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
                        && member.getArchived(),
                member != null
                        && member.getPinned(),
                member != null
                        ? member.getUnreadCount()
                        : 0
        );
    }
}
