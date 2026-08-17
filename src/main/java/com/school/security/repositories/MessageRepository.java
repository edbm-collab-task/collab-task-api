package com.school.security.repositories;

import com.school.security.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message> findByConversationConversationIdOrderByCreatedAtAsc(
            Long conversationId
    );
}