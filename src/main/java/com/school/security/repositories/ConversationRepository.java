package com.school.security.repositories;

import com.school.security.entities.Conversation;
import com.school.security.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Accès aux conversations.
 *
 * <p>Les requêtes JPQL ci-dessous portent sur les appartenances
 * ({@code Conversation.members}) ; seul le sens des requêtes est documenté.
 */
public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    /**
     * Conversations dont l'utilisateur est membre ({@code DISTINCT} pour éviter
     * les doublons de jointure). Ne filtre ni le type ni l'état archivé.
     */
    @Query("""
        SELECT DISTINCT c
        FROM Conversation c
        JOIN c.members m
        WHERE m.user.usersId = :userId
        """)
    List<Conversation> findUserConversations(
            @Param("userId") Long userId
    );

    /**
     * Conversations du type donné réunissant exactement deux membres, qui sont
     * précisément {@code user1} et {@code user2} (un {@code EXISTS} par
     * utilisateur plus un total de membres égal à 2). Renvoie une liste : vide,
     * un seul élément, ou plusieurs si des doublons existent en base.
     */
    @Query("""
        SELECT c
        FROM Conversation c
        WHERE c.type = :type
        AND EXISTS (
            SELECT m1
            FROM ConversationMember m1
            WHERE m1.conversation = c
            AND m1.user.usersId = :user1
        )
        AND EXISTS (
            SELECT m2
            FROM ConversationMember m2
            WHERE m2.conversation = c
            AND m2.user.usersId = :user2
        )
        AND (
            SELECT COUNT(m3)
            FROM ConversationMember m3
            WHERE m3.conversation = c
        ) = 2
        """)
    List<Conversation> findPrivateConversation(
            @Param("type") ConversationType type,
            @Param("user1") Long user1,
            @Param("user2") Long user2
    );
}