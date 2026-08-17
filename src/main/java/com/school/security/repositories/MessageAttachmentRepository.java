package com.school.security.repositories;

import com.school.security.entities.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository
        extends JpaRepository<MessageAttachment, Long> {
}