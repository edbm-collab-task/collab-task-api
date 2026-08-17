package com.school.security.mappers;

import com.school.security.dtos.responses.ConversationMemberResponse;
import com.school.security.entities.ConversationMember;
import org.springframework.stereotype.Component;

@Component
public class ConversationMemberMapper {

    private final ChatUserMapper chatUserMapper;

    public ConversationMemberMapper(
            ChatUserMapper chatUserMapper
    ) {
        this.chatUserMapper = chatUserMapper;
    }

    public ConversationMemberResponse toResponse(
            ConversationMember member
    ) {

        return new ConversationMemberResponse(
                member.getConversationMemberId(),
                member.getUser().getUsersId(),
                chatUserMapper.toResponse(
                        member.getUser()
                ),
                member.getJoinedAt(),
                member.getReadAt(),
                member.getMuted(),
                member.getArchived(),
                member.getPinned(),
                member.getUnreadCount()
        );
    }
}
