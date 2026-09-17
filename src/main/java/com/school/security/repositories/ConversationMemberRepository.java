package com.school.security.repositories;

import com.school.security.entities.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

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
}