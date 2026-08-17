package com.school.security.repositories;

import com.school.security.entities.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, Long> {

    Optional<ConversationMember>
    findByConversationConversationIdAndUserUsersId(
            Long conversationId,
            Long userId
    );

    List<ConversationMember>
    findByConversationConversationId(
            Long conversationId
    );

    boolean existsByConversationConversationIdAndUserUsersId(
            Long conversationId,
            Long userId
    );

    void deleteByConversationConversationIdAndUserUsersId(
            Long conversationId,
            Long userId
    );
}