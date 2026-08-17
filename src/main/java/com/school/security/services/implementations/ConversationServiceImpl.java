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

    @Override
    public void markAsRead(
            Long conversationId
    ) {

        Long currentUserId =
                currentUserId();

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
