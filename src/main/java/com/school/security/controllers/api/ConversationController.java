package com.school.security.controllers.api;

import com.school.security.dtos.requests.AddMembersRequest;
import com.school.security.dtos.requests.CreateConversationRequest;
import com.school.security.dtos.requests.CreateGroupRequest;
import com.school.security.dtos.responses.*;
import com.school.security.services.contracts.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
/**
 * Contrôleur HTTP des conversations de messagerie.
 *
 * <p>Il expose les opérations de lecture et de gestion des conversations
 * privées et de groupe, ainsi que les actions liées aux membres, à l'état
 * d'archivage, au pin et au marquage en lu.
 *
 * <p>Le contrôleur ne porte pas lui-même les règles d'appartenance : il se
 * contente de recevoir la requête HTTP et de déléguer au service, qui vérifie
 * l'utilisateur courant et applique les contrôles métier sur la conversation.
 */
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService
    ) {
        this.conversationService =
                conversationService;
    }

    /**
     * Retourne la liste des utilisateurs exploitables pour créer une
     * conversation.
     *
     * <p>La sélection des utilisateurs est gérée par le service, qui exclut
     * l'utilisateur courant et filtre les comptes inactifs.
     */
    @GetMapping("/users")
    public ResponseEntity<List<ChatUserResponse>>
    getUsers() {

        return ResponseEntity.ok(
                conversationService.getChatUsers()
        );
    }

        /**
         * Retourne les conversations de l'utilisateur courant.
         *
         * <p>Le paramètre {@code archived} permet d'inclure ou non les conversations
         * archivées dans la réponse. Le tri et le filtrage fin sont effectués par
         * le service.
         */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>>
    getConversations(
            @RequestParam(
                    defaultValue = "false"
            )
            boolean archived
    ) {

        return ResponseEntity.ok(
                conversationService
                        .getConversations(archived)
        );
    }

        /**
         * Retourne une conversation précise.
         *
         * <p>Le contrôle d'appartenance à la conversation est effectué dans le
         * service avant l'exposition des données.
         */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse>
    getConversation(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService
                        .getConversation(
                                conversationId
                        )
        );
    }

        /**
         * Crée ou réactive une conversation privée avec un autre utilisateur.
         *
         * <p>Le service gère la création de la conversation, la détection d'un
         * échange privé déjà existant et l'archivage éventuel côté membre courant.
         */
    @PostMapping("/private")
    public ResponseEntity<ConversationResponse>
    createPrivate(
            @RequestBody
            CreateConversationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        conversationService
                                .createPrivateConversation(
                                        request
                                )
                );
    }

        /**
         * Crée une conversation de groupe.
         *
         * <p>Le service valide les membres demandés, ajoute l'utilisateur courant
         * comme membre initial et construit la conversation de type groupe.
         */
    @PostMapping("/group")
    public ResponseEntity<ConversationResponse>
    createGroup(
            @RequestBody
            CreateGroupRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        conversationService
                                .createGroup(
                                        request
                                )
                );
    }

        /**
         * Retourne les membres d'une conversation.
         *
         * <p>Le service vérifie que l'utilisateur courant appartient à la
         * conversation avant de renvoyer la liste des membres.
         */
    @GetMapping("/{conversationId}/members")
    public ResponseEntity<
            List<ConversationMemberResponse>
            >
    getMembers(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.getMembers(
                        conversationId
                )
        );
    }

    /**
     * Ajoute des membres à une conversation.
     *
     * <p>La gestion du type de conversation, de l'appartenance et des doublons
     * est déléguée au service.
     */
    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ConversationResponse>
    addMembers(
            @PathVariable Long conversationId,
            @RequestBody AddMembersRequest request
    ) {

        return ResponseEntity.ok(
                conversationService.addMembers(
                        conversationId,
                        request
                )
        );
    }

    /**
     * Retire un membre d'une conversation de groupe.
     *
     * <p>Le service applique les règles d'appartenance, de type de conversation
     * et d'existence du membre cible.
     */
    @DeleteMapping(
            "/{conversationId}/members/{userId}"
    )
    public ResponseEntity<Void> removeMember(
            @PathVariable Long conversationId,
            @PathVariable Long userId
    ) {

        conversationService.removeMember(
                conversationId,
                userId
        );

        return ResponseEntity.noContent()
                .build();
    }

    /**
     * Permet à l'utilisateur courant de quitter un groupe.
     *
     * <p>Cette opération ne concerne pas les conversations privées ; la
     * vérification est effectuée dans le service.
     */
    @DeleteMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long conversationId
    ) {

        conversationService.leaveGroup(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }

    /**
     * Marque la conversation comme lue pour l'utilisateur courant.
     *
     * <p>Le service met à jour le compteur de non-lus et l'horodatage de
     * lecture pour le membre courant.
     */
    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId
    ) {

        conversationService.markAsRead(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }

    /**
     * Active ou désactive l'état épinglé de la conversation pour l'utilisateur
     * courant.
     *
     * <p>L'état est porté par l'entrée membre correspondante et le service
     * renvoie ensuite la conversation mise à jour.
     */
    @PatchMapping("/{conversationId}/pin")
    public ResponseEntity<ConversationResponse>
    togglePin(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.togglePin(
                        conversationId
                )
        );
    }

    /**
     * Archive ou désarchive la conversation pour l'utilisateur courant.
     *
     * <p>Le service bascule l'état d'archivage du membre puis renvoie la
     * conversation recalculée pour ce membre.
     */
    @PatchMapping("/{conversationId}/archive")
    public ResponseEntity<ConversationResponse>
    toggleArchive(
            @PathVariable Long conversationId
    ) {

        return ResponseEntity.ok(
                conversationService.toggleArchive(
                        conversationId
                )
        );
    }

    /**
     * Supprime la conversation.
     *
     * <p>Le contrôle d'appartenance est effectué dans le service avant la
     * suppression de l'enregistrement.
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void>
    deleteConversation(
            @PathVariable Long conversationId
    ) {

        conversationService.deleteConversation(
                conversationId
        );

        return ResponseEntity.noContent()
                .build();
    }
}