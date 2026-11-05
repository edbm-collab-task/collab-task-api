package com.school.security.repositories;

import com.school.security.entities.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux appartenances de conversation.
 *
 * <p>Les méthodes dérivées par clé composée {@code conversationId + userId}
 * sont explicites (recherche d'un membre, existence, liste des membres).
 * Seule la suppression dérivée mérite une précision (voir ci-dessous).
 */
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

    /**
     * Supprime l'appartenance correspondante. Requête dérivée de suppression :
     * Spring Data sélectionne puis supprime les entités une à une, et un
     * contexte transactionnel est requis (méthode non annotée {@code @Modifying}).
     */
    void deleteByConversationConversationIdAndUserUsersId(
            Long conversationId,
            Long userId
    );

    /**
     * Somme des messages non lus d'un utilisateur, toutes conversations
     * confondues.
     *
     * <p>Le non-lu est porté par {@code ConversationMember.unreadCount}
     * (incrémenté à l'envoi d'un message pour chaque destinataire, remis à 0
     * par le marquage en lu). {@code COALESCE} garantit un résultat de 0
     * lorsque l'utilisateur n'a aucune appartenance.
     */
    @Query(
            "SELECT COALESCE(SUM(m.unreadCount), 0) " +
            "FROM ConversationMember m " +
            "WHERE m.user.usersId = :userId"
    )
    long sumUnreadCountByUserUsersId(
            @Param("userId") Long userId
    );
}
