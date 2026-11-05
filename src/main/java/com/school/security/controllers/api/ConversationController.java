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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Conversations", description = "Gestion des conversations (privées et groupes), membres, archivage, épinglage et lecture.")
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
    @Operation(summary = "Lister les utilisateurs disponibles", description = "Retourne la liste des utilisateurs exploitables pour créer une conversation. L'utilisateur courant et les comptes inactifs sont exclus par le service.")
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
    @Operation(summary = "Lister mes conversations", description = "Retourne les conversations de l'utilisateur courant. Le filtre 'archived' permet d'inclure ou non les conversations archivées.")
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
     * Nombre total de messages non lus de l'utilisateur courant, toutes
     * conversations confondues ({@code GET /conversations/unread-count}).
     *
     * <p>Retourne un entier brut (0 s'il n'y a aucun non-lu). Le calcul est
     * scopé à l'utilisateur authentifié par le service, qui somme
     * {@code unreadCount} sur ses appartenances.
     */
    @Operation(summary = "Nombre total de messages non lus", description = "Retourne le nombre total de messages non lus pour l'utilisateur courant, toutes conversations confondues.")
    @GetMapping("/unread-count")
    public ResponseEntity<Integer>
    getUnreadCount() {

        return ResponseEntity.ok(
                conversationService
                        .getUnreadCount()
        );
    }

        /**
         * Retourne une conversation précise.
         *
         * <p>Le contrôle d'appartenance à la conversation est effectué dans le
         * service avant l'exposition des données.
         */
    @Operation(summary = "Récupérer une conversation", description = "Retourne une conversation précise. L'accès est réservé aux membres de la conversation.")
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
    @Operation(summary = "Créer une conversation privée", description = "Crée ou réactive une conversation privée avec un autre utilisateur. Le service détecte un échange déjà existant et applique les règles associées.")
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
    @Operation(summary = "Créer une conversation de groupe", description = "Crée une conversation de groupe. L'utilisateur courant est ajouté comme membre initial et les membres demandés sont validés par le service.")
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
    @Operation(summary = "Lister les membres d'une conversation", description = "Retourne les membres d'une conversation. L'accès est réservé aux membres de la conversation.")
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
    @Operation(summary = "Ajouter des membres à un groupe", description = "Ajoute des membres à une conversation. Les règles sur le type de conversation, l'appartenance et les doublons sont appliquées par le service.")
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
    @Operation(summary = "Retirer un membre d'un groupe", description = "Retire un membre d'une conversation de groupe. Les règles d'appartenance, de type de conversation et d'existence du membre sont appliquées par le service.")
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
    @Operation(summary = "Quitter un groupe", description = "Permet à l'utilisateur courant de quitter un groupe. Cette opération ne s'applique pas aux conversations privées.")
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
    @Operation(summary = "Marquer une conversation comme lue", description = "Marque la conversation comme lue pour l'utilisateur courant. Le compteur de non-lus et l'horodatage de lecture sont mis à jour par le service.")
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
    @Operation(summary = "Épingler/désépingler une conversation", description = "Active ou désactive l'état épinglé de la conversation pour l'utilisateur courant. La conversation mise à jour est retournée.")
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
    @Operation(summary = "Archiver/désarchiver une conversation", description = "Bascule l'état d'archivage de la conversation pour l'utilisateur courant. La conversation mise à jour est retournée.")
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
    @Operation(summary = "Supprimer une conversation", description = "Supprime la conversation. L'appartenance est vérifiée par le service avant suppression.")
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