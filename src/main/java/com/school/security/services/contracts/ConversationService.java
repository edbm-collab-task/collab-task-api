package com.school.security.services.contracts;

import com.school.security.dtos.requests.AddMembersRequest;
import com.school.security.dtos.requests.CreateConversationRequest;
import com.school.security.dtos.requests.CreateGroupRequest;
import com.school.security.dtos.responses.ChatUserResponse;
import com.school.security.dtos.responses.ConversationMemberResponse;
import com.school.security.dtos.responses.ConversationResponse;

import java.util.List;

public interface ConversationService {

    List<ChatUserResponse> getChatUsers();

    List<ConversationResponse> getConversations(
            boolean includeArchived
    );

    ConversationResponse getConversation(
            Long conversationId
    );

    ConversationResponse createPrivateConversation(
            CreateConversationRequest request
    );

    ConversationResponse createGroup(
            CreateGroupRequest request
    );

    List<ConversationMemberResponse> getMembers(
            Long conversationId
    );

    ConversationResponse addMembers(
            Long conversationId,
            AddMembersRequest request
    );

    void removeMember(
            Long conversationId,
            Long userId
    );

    void leaveGroup(
            Long conversationId
    );

    void markAsRead(
            Long conversationId
    );

    ConversationResponse togglePin(
            Long conversationId
    );

    ConversationResponse toggleArchive(
            Long conversationId
    );

    void deleteConversation(
            Long conversationId
    );
}