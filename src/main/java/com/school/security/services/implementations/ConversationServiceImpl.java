package com.school.security.services.implementations;

import com.school.security.dtos.requests.AddMembersRequest;
import com.school.security.dtos.requests.CreateConversationRequest;
import com.school.security.dtos.requests.CreateGroupRequest;
import com.school.security.dtos.responses.ChatUserResponse;
import com.school.security.dtos.responses.ConversationMemberResponse;
import com.school.security.dtos.responses.ConversationResponse;
import com.school.security.entities.Conversation;
import com.school.security.entities.ConversationMember;
import com.school.security.entities.User;
import com.school.security.enums.ConversationType;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.mappers.ChatUserMapper;
import com.school.security.mappers.ConversationMapper;
import com.school.security.mappers.ConversationMemberMapper;
import com.school.security.repositories.ConversationMemberRepository;
import com.school.security.repositories.ConversationRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.ConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implémentation du service de messagerie (conversations privées et groupes).
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>l'utilisateur courant est résolu depuis le {@code SecurityContext}
 *       par email ; absence d'authentification → {@code BadRequestException},
 *       email inconnu → {@code ResourceNotFoundException} ;</li>
 *   <li>l'appartenance à une conversation est systématiquement vérifiée via
 *       {@link #verifyMember} pour les opérations de lecture/écriture liées
 *       à une conversation ;</li>
 *   <li>il n'existe PAS de hiérarchie propriétaire/admin de groupe : la
 *       notion de "owner" se limite à la {@code ConversationMember} créée en
 *       premier lors de {@link #createGroup}. Tout membre peut donc ajouter
 *       des membres, en retirer (y compris d'autres membres) ou supprimer la
 *       conversation ;</li>
 *   <li>les opérations d'ajout/retrait de membres, {@code leaveGroup},
 *       {@code markAsRead}, {@code togglePin} et {@code toggleArchive}
 *       n'émettent AUCUNE notification et ne diffusent rien par WebSocket
 *       (aucune dépendance à un broker dans ce service) ;</li>
 *   <li>{@link #deleteConversation} est une suppression physique : la
 *       relation {@code Conversation.members}/{@code Conversation.messages}
 *       est en {@code CascadeType.ALL + orphanRemoval}, donc messages et
 *       appartenances sont supprimés avec la conversation.</li>
 * </ul>
 *
 * <p>L'annotation {@code @Transactional} de classe est en lecture-écriture ;
 * les méthodes de lecture la surchargent par
 * {@code @Transactional(readOnly = true)}.
 */
@Service
@Transactional
public class ConversationServiceImpl
        implements ConversationService {

    private final ConversationRepository
            conversationRepository;

    private final ConversationMemberRepository
            memberRepository;

    private final UserRepository userRepository;

    private final ConversationMapper
            conversationMapper;

    private final ConversationMemberMapper
            memberMapper;

    private final ChatUserMapper chatUserMapper;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository,
            ConversationMapper conversationMapper,
            ConversationMemberMapper memberMapper,
            ChatUserMapper chatUserMapper
    ) {
        this.conversationRepository =
                conversationRepository;
        this.memberRepository =
                memberRepository;
        this.userRepository =
                userRepository;
        this.conversationMapper =
                conversationMapper;
        this.memberMapper =
                memberMapper;
        this.chatUserMapper =
                chatUserMapper;
    }

    /**
     * Résout l'identifiant de l'utilisateur authentifié courant.
     *
     * <p>Contrairement à {@code SecurityUtils.getCurrentUsername()}, cette
     * méthode ne teste pas {@code isAuthenticated()} : elle se contente de
     * vérifier que l'authentification et son nom ne sont pas nuls. Elle lève
     * {@code BadRequestException} si aucun utilisateur n'est authentifié, et
     * {@code ResourceNotFoundException} si l'email du contexte n'existe pas
     * en base.
     */
    private Long currentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null) {
            throw new BadRequestException(
                    "Utilisateur non authentifié."
            );
        }

        User user =
                userRepository
                        .findByEmail(
                                authentication.getName()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Utilisateur connecté introuvable."
                                )
                        );

        return user.getUsersId();
    }

    /**
     * Liste les interlocuteurs disponibles pour le chat :
     * tous les autres utilisateurs ACTIFS uniquement (l'utilisateur courant
     * est exclu, les comptes désactivés sont filtrés).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatUserResponse> getChatUsers() {

        Long currentUserId =
                currentUserId();

        return userRepository
                .findAll()
                .stream()
                .filter(user ->
                        !user.getUsersId()
                                .equals(currentUserId)
                )
                .filter(user ->
                        Boolean.TRUE.equals(
                                user.getIsActive()
                        )
                )
                .map(chatUserMapper::toResponse)
                .toList();
    }

    /**
     * Liste les conversations de l'utilisateur courant.
     *
     * <p>Le drapeau d'archivage étant porté par {@code ConversationMember}
     * (donc propre à chaque participant), il est appliqué via le mapping
     * individuel de la conversation : {@code includeArchived=false} masque
     * celles que l'utilisateur a archivées. Tri : épinglées d'abord, puis
     * {@code updatedAt} décroissant.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(
            boolean includeArchived
    ) {

        Long currentUserId =
                currentUserId();

        return conversationRepository
                .findUserConversations(
                        currentUserId
                )
                .stream()
                .map(conversation ->
                        conversationMapper.toResponse(
                                conversation,
                                currentUserId
                        )
                )
                .filter(conversation ->
                        includeArchived
                                || !conversation.archived()
                )
                .sorted(
                        Comparator
                                .comparing(
                                        ConversationResponse::pinned
                                )
                                .reversed()
                                .thenComparing(
                                        ConversationResponse::updatedAt,
                                        Comparator.reverseOrder()
                                )
                )
                .toList();
    }

    /**
     * Récupère une conversation par identifiant.
     *
     * <p>Refuse l'accès (via {@link #verifyMember}) si l'utilisateur courant
     * n'est pas membre de la conversation.
     */
    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(conversationId);

        verifyMember(
                conversation,
                currentUserId
        );

        return conversationMapper.toResponse(
                conversation,
                currentUserId
        );
    }

    /**
     * Crée (ou récupère) une conversation privée entre l'utilisateur courant
     * et un autre utilisateur.
     *
     * <p>Validations : {@code userId} obligatoire, interdiction de créer une
     * conversation avec soi-même ; les deux utilisateurs doivent exister.
     *
     * <p>Recherche préalable d'une conversation privée existante entre les
     * deux participants (requête symétrique, exactement 2 membres) : si elle
     * existe, elle est RÉUTILISÉE au lieu d'en créer une nouvelle. Dans ce
     * cas, seule la {@code ConversationMember} de l'utilisateur courant est
     * désarchivée et {@code updatedAt} est rafraîchi ; le nom et l'avatar ne
     * sont pas recalculés.
     *
     * <p>À la création : conversation de type {@code PRIVATE} nommée d'après
     * l'autre utilisateur et reprenant son image, avec deux membres créés
     * avec les valeurs par défaut ({@link #createMember}). Aucune
     * notification n'est émise.
     */
    @Override
    public ConversationResponse createPrivateConversation(
            CreateConversationRequest request
    ) {

        Long currentUserId =
                currentUserId();

        if (request.userId() == null) {
            throw new BadRequestException(
                    "L'utilisateur est obligatoire."
            );
        }

        if (request.userId()
                .equals(currentUserId)) {
            throw new BadRequestException(
                    "Vous ne pouvez pas créer une conversation avec vous-même."
            );
        }

        User currentUser =
                findUser(currentUserId);

        User otherUser =
                findUser(request.userId());

        List<Conversation> existing =
                conversationRepository
                        .findPrivateConversation(
                                ConversationType.PRIVATE,
                                currentUserId,
                                request.userId()
                        );

        // Conversation privée déjà existante : on la réutilise plutôt que
        // d'en créer un doublon.
        if (!existing.isEmpty()) {

            Conversation conversation =
                    existing.getFirst();

            ConversationMember member =
                    memberRepository
                            .findByConversationConversationIdAndUserUsersId(
                                    conversation.getConversationId(),
                                    currentUserId
                            )
                            .orElseThrow();

            // Réouverture côté utilisateur courant : la conversation
            // réapparaît pour lui même s'il l'avait archivée.
            member.setArchived(false);

            conversation.setUpdatedAt(
                    LocalDateTime.now()
            );

            return conversationMapper.toResponse(
                    conversationRepository.save(
                            conversation
                    ),
                    currentUserId
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        Conversation conversation =
                Conversation.builder()
                        .type(
                                ConversationType.PRIVATE
                        )
                        .name(
                                otherUser.getFirstname()
                                        + " "
                                        + otherUser.getLastname()
                        )
                        .avatar(
                                otherUser.getImagePath()
                        )
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        ConversationMember first =
                createMember(
                        conversation,
                        currentUser
                );

        ConversationMember second =
                createMember(
                        conversation,
                        otherUser
                );

        conversation.getMembers()
                .add(first);

        conversation.getMembers()
                .add(second);

        conversation =
                conversationRepository.save(
                        conversation
                );

        return conversationMapper.toResponse(
                conversation,
                currentUserId
        );
    }

    /**
     * Crée une conversation de groupe.
     *
     * <p>Validations : nom obligatoire (non vide après {@code trim()}) et au
     * moins un identifiant de membre fourni. Les identifiants sont dédupliqués
     * ({@code LinkedHashSet}) et l'utilisateur courant en est retiré : s'il ne
     * reste alors personne, une erreur est levée ("Ajoutez au moins un autre
     * membre"). Chaque identifiant restant doit correspondre à un utilisateur
     * existant.
     *
     * <p>L'utilisateur courant est ajouté comme premier membre. Aucun rôle
     * propriétaire/admin n'est persisté : les membres du groupe sont
     * strictement équivalents. L'avatar est laissé à {@code null} à la
     * création. Aucune notification n'est émise.
     */
    @Override
    public ConversationResponse createGroup(
            CreateGroupRequest request
    ) {

        Long currentUserId =
                currentUserId();

        if (request.name() == null ||
                request.name().trim().isEmpty()) {
            throw new BadRequestException(
                    "Le nom du groupe est obligatoire."
            );
        }

        if (request.memberIds() == null ||
                request.memberIds().isEmpty()) {
            throw new BadRequestException(
                    "Le groupe doit contenir au moins un membre."
            );
        }

        Set<Long> ids =
                new LinkedHashSet<>(
                        request.memberIds()
                );

        // L'utilisateur courant est ajouté séparément ci-dessous : on l'écarte
        // de la liste fournie pour éviter un doublon de membre.
        ids.remove(currentUserId);

        if (ids.isEmpty()) {
            throw new BadRequestException(
                    "Ajoutez au moins un autre membre."
            );
        }

        User currentUser =
                findUser(currentUserId);

        LocalDateTime now =
                LocalDateTime.now();

        Conversation conversation =
                Conversation.builder()
                        .type(
                                ConversationType.GROUP
                        )
                        .name(
                                request.name().trim()
                        )
                        .avatar(null)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        // Premier membre = créateur du groupe. Ce statut n'est pas persisté
        // comme un rôle : il ne donne aucun privilège supplémentaire.
        ConversationMember owner =
                createMember(
                        conversation,
                        currentUser
                );

        conversation.getMembers()
                .add(owner);

        for (Long userId : ids) {

            User user =
                    findUser(userId);

            ConversationMember member =
                    createMember(
                            conversation,
                            user
                    );

            conversation.getMembers()
                    .add(member);
        }

        conversation =
                conversationRepository.save(
                        conversation
                );

        return conversationMapper.toResponse(
                conversation,
                currentUserId
        );
    }

    /**
     * Liste les membres d'une conversation, après vérification que
     * l'utilisateur courant en fait partie.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ConversationMemberResponse> getMembers(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        return memberRepository
                .findByConversationConversationId(
                        conversationId
                )
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    /**
     * Ajoute des membres à une conversation de groupe.
     *
     * <p>Accessible à TOUT membre de la conversation (pas de contrôle de
     * propriétaire/admin) ; refuse les conversations privées et une liste
     * vide. Les identifiants déjà membres sont ignorés silencieusement, les
     * autres sont résolus puis ajoutés. {@code updatedAt} est rafraîchi.
     */
    @Override
    public ConversationResponse addMembers(
            Long conversationId,
            AddMembersRequest request
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        if (conversation.getType()
                != ConversationType.GROUP) {
            throw new BadRequestException(
                    "Impossible d'ajouter des membres à une conversation privée."
            );
        }

        if (request.memberIds() == null ||
                request.memberIds().isEmpty()) {
            throw new BadRequestException(
                    "Aucun membre à ajouter."
            );
        }

        // Ensemble des membres déjà présents, utilisé pour ignorer les
        // identifiants en doublon.
        Set<Long> existing =
                conversation.getMembers()
                        .stream()
                        .map(member ->
                                member.getUser()
                                        .getUsersId()
                        )
                        .collect(
                                java.util.stream.Collectors
                                        .toSet()
                        );

        for (Long userId :
                new LinkedHashSet<>(
                        request.memberIds()
                )) {

            if (existing.contains(userId)) {
                continue;
            }

            User user =
                    findUser(userId);

            ConversationMember member =
                    createMember(
                            conversation,
                            user
                    );

            conversation.getMembers()
                    .add(member);
        }

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        conversation =
                conversationRepository.save(
                        conversation
                );

        return conversationMapper.toResponse(
                conversation,
                currentUserId
        );
    }

    /**
     * Retire un membre d'une conversation de groupe.
     *
     * <p>Accessible à tout membre : rien n'empêche de retirer un AUTRE membre
     * (le statut de créateur n'est pas contrôlé). La cible doit exister dans
     * le groupe. La suppression est faite directement via le repository ;
     * {@code updatedAt} est modifié sur l'entité gérée (persisté par
     * dirty-checking de la transaction, sans {@code save} explicite).
     */
    @Override
    public void removeMember(
            Long conversationId,
            Long userId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        if (conversation.getType()
                != ConversationType.GROUP) {
            throw new BadRequestException(
                    "Cette opération est réservée aux groupes."
            );
        }

        if (!memberRepository
                .existsByConversationConversationIdAndUserUsersId(
                        conversationId,
                        userId
                )) {
            throw new ResourceNotFoundException(
                    "Membre introuvable dans ce groupe."
            );
        }

        memberRepository
                .deleteByConversationConversationIdAndUserUsersId(
                        conversationId,
                        userId
                );

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );
    }

    /**
     * Quitte un groupe : supprime l'appartenance de l'utilisateur courant.
     *
     * <p>Réservé aux groupes (une conversation privée ne peut pas être
     * quittée). À noter : contrairement à {@link #removeMember}, cette méthode
     * ne rafraîchit PAS {@code updatedAt} de la conversation.
     */
    @Override
    public void leaveGroup(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        if (conversation.getType()
                != ConversationType.GROUP) {
            throw new BadRequestException(
                    "Une conversation privée ne peut pas être quittée."
            );
        }

        memberRepository
                .deleteByConversationConversationIdAndUserUsersId(
                        conversationId,
                        currentUserId
                );
    }

    /**
     * Marque la conversation comme lue pour l'utilisateur courant.
     *
     * <p>Le compteur de non-lus et la date de lecture sont portés par la
     * {@code ConversationMember}. L'absence d'appartenance est traitée comme
     * "conversation introuvable" ({@code ResourceNotFoundException}) ; aucune
     * vérification préalable d'appartenance n'est faite.
     */
    @Override
    public void markAsRead(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        ConversationMember member =
                memberRepository
                        .findByConversationConversationIdAndUserUsersId(
                                conversationId,
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Conversation introuvable."
                                )
                        );

        member.setUnreadCount(0);
        member.setReadAt(
                LocalDateTime.now()
        );

        memberRepository.save(member);
    }

    /**
     * Nombre total de messages non lus de l'utilisateur courant, toutes
     * conversations confondues.
     *
     * <p>Le non-lu est porté par {@code ConversationMember.unreadCount} :
     * incrémenté à chaque message reçu, remis à zéro par {@link #markAsRead}.
     * La somme commute en base en une seule requête agrégée ({@code SUM}),
     * sans chargement des appartenances en mémoire.
     */
    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount() {

        Long currentUserId =
                currentUserId();

        return Math.toIntExact(
                memberRepository
                        .sumUnreadCountByUserUsersId(
                                currentUserId
                        )
        );
    }

    /**
     * Bascule l'épinglage de la conversation pour l'utilisateur courant
     * (état propre à chaque membre). Refuse si l'utilisateur n'est pas
     * membre (via {@link #getMember}).
     */
    @Override
    public ConversationResponse togglePin(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        ConversationMember member =
                getMember(
                        conversationId,
                        currentUserId
                );

        member.setPinned(
                !Boolean.TRUE.equals(
                        member.getPinned()
                )
        );

        memberRepository.save(member);

        return getConversation(
                conversationId
        );
    }

    /**
     * Bascule l'archivage de la conversation pour l'utilisateur courant
     * (état propre à chaque membre). Refuse si l'utilisateur n'est pas
     * membre (via {@link #getMember}).
     */
    @Override
    public ConversationResponse toggleArchive(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        ConversationMember member =
                getMember(
                        conversationId,
                        currentUserId
                );

        member.setArchived(
                !Boolean.TRUE.equals(
                        member.getArchived()
                )
        );

        memberRepository.save(member);

        return getConversation(
                conversationId
        );
    }

    /**
     * Supprime définitivement une conversation.
     *
     * <p>Accessible à tout membre (pas de contrôle de créateur). Il s'agit
     * d'une suppression physique de l'entité : en raison du
     * {@code CascadeType.ALL + orphanRemoval} mappé sur
     * {@code Conversation.members} et {@code Conversation.messages}, les
     * appartenances et TOUS les messages de la conversation sont supprimés
     * pour l'ensemble des participants.
     */
    @Override
    public void deleteConversation(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

        Conversation conversation =
                findConversation(
                        conversationId
                );

        verifyMember(
                conversation,
                currentUserId
        );

        conversationRepository.delete(
                conversation
        );
    }

    /**
     * Construit une appartenance de conversation avec les valeurs par défaut :
     * non mutée, non archivée, non épinglée, aucun non-lu, {@code joinedAt}
     * à maintenant. La conversation et l'utilisateur fournis doivent être des
     * entités gérées/persistées (la relation est propagée à la sauvegarde de
     * la conversation).
     */
    private ConversationMember createMember(
            Conversation conversation,
            User user
    ) {

        return ConversationMember.builder()
                .conversation(conversation)
                .user(user)
                .joinedAt(
                        LocalDateTime.now()
                )
                .muted(false)
                .archived(false)
                .pinned(false)
                .unreadCount(0)
                .build();
    }

    /**
     * Charge une conversation par identifiant ou lève
     * {@code ResourceNotFoundException}.
     */
    private Conversation findConversation(
            Long conversationId
    ) {

        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Conversation introuvable."
                        )
                );
    }

    /**
     * Charge un utilisateur par identifiant ou lève
     * {@code ResourceNotFoundException}.
     */
    private User findUser(
            Long userId
    ) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Utilisateur introuvable."
                        )
                );
    }

    /**
     * Charge l'appartenance (conversation, utilisateur) via le repository ou
     * lève {@code ResourceNotFoundException}. Utilisé par les bascules
     * d'épinglage/archivage, qui ont besoin de l'entité persistée.
     */
    private ConversationMember getMember(
            Long conversationId,
            Long userId
    ) {

        return memberRepository
                .findByConversationConversationIdAndUserUsersId(
                        conversationId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Membre introuvable."
                        )
                );
    }

    /**
     * Vérifie que l'utilisateur fait partie de la conversation en parcourant
     * la collection {@code conversation.getMembers()} déjà chargée (et non le
     * repository). Lève {@code BadRequestException} sinon.
     */
    private void verifyMember(
            Conversation conversation,
            Long userId
    ) {

        boolean member =
                conversation.getMembers()
                        .stream()
                        .anyMatch(item ->
                                item.getUser()
                                        .getUsersId()
                                        .equals(userId)
                        );

        if (!member) {
            throw new BadRequestException(
                    "Vous ne faites pas partie de cette conversation."
            );
        }
    }
}
