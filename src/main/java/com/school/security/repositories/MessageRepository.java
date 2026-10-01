package com.school.security.repositories;

import com.school.security.entities.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    /**
     * Page des messages les plus récents d'une conversation, du plus récent au
     * plus ancien. Le tri se fait sur l'identifiant et non sur
     * {@code createdAt} afin de disposer d'un curseur stable et sans doublon
     * lorsque plusieurs messages partagent le même horodatage.
     *
     * <p>Le {@code Pageable} borne la requête ; l'appelant choisit la fenêtre
     * et se charge de l'inversion finale.
     */
    List<Message>
    findByConversationConversationIdOrderByMessageIdDesc(
            Long conversationId,
            Pageable pageable
    );

    /**
     * Idem {@link #findByConversationConversationIdOrderByMessageIdDesc}, mais
     * limitée aux messages d'identifiant strictement inférieur à
     * {@code beforeId} : c'est la page des messages plus anciens. Un
     * {@code beforeId} nul se traduit par l'appel de la méthode sans curseur.
     *
     * <p>L'identifiant servant de curseur rend la pagination insensible aux
     * insertions concurrentes.
     */
    List<Message>
    findByConversationConversationIdAndMessageIdLessThanOrderByMessageIdDesc(
            Long conversationId,
            Long beforeId,
            Pageable pageable
    );

    Optional<Message>
    findTopByConversationConversationIdOrderByCreatedAtDesc(
            Long conversationId
    );

    /**
     * Messages d'une conversation correspondant à {@code term}, du plus récent
     * au plus ancien.
     *
     * <p>Un message correspond si son contenu contient le terme, ou si l'une de
     * ses pièces jointes porte ce nom. La recherche est donc insensible à la
     * casse et la sous-requête {@code EXISTS} évite de dupliquer un message
     * possédant plusieurs pièces jointes correspondantes.
     *
     * <p>Les messages supprimés (suppression logique) sont exclus, et
     * {@code beforeId} permet de paginer les résultats comme la liste des
     * messages.
     */
    @Query("""
        SELECT m
        FROM Message m
        WHERE m.conversation.conversationId = :conversationId
        AND m.deleted = false
        AND (
            LOWER(m.content) LIKE LOWER(CONCAT('%', :term, '%'))
            OR EXISTS (
                SELECT 1
                FROM MessageAttachment a
                WHERE a.message = m
                AND LOWER(a.name) LIKE LOWER(CONCAT('%', :term, '%'))
            )
        )
        AND (:beforeId IS NULL OR m.messageId < :beforeId)
        """)
    List<Message> search(
            @Param("conversationId") Long conversationId,
            @Param("term") String term,
            @Param("beforeId") Long beforeId,
            Pageable pageable
    );
}
