package com.school.security.repositories;

import com.school.security.entities.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
