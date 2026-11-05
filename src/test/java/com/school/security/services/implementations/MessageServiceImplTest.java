package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.dtos.responses.MessagePageResponse;
import com.school.security.dtos.responses.MessageResponse;
import com.school.security.entities.Conversation;
import com.school.security.entities.ConversationMember;
import com.school.security.entities.Message;
import com.school.security.entities.User;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.mappers.MessageMapper;
import com.school.security.repositories.ConversationMemberRepository;
import com.school.security.repositories.ConversationRepository;
import com.school.security.repositories.MessageRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.FileStorageService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tests de la pagination par curseur et de la recherche de messages.
 *
 * <p>Les requêtes du dépôt étant exécutées du plus récent au plus ancien,
 * les listes simulées sont construites dans cet ordre DESC afin de
 * reproduire le comportement réel de la base.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    private static final Long CONVERSATION_ID = 100L;

    private static final Long USER_ID = 5L;

    private static final String USER_EMAIL = "alice@test.com";

    private static final int LIMIT = 15;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        authenticate(USER_EMAIL);
        stubMember();

        /*
         * Le mapper est simulé : la réponse reprend l'identifiant du
         * message source, ce qui permet d'observer l'ordre réellement
         * produit par le service. Le stubbing est lenient car les tests de
         * rejet (non-membre, conversation absente) ne l'atteignent jamais.
         */
        lenient()
                .when(messageMapper.toResponse(any(Message.class)))
                .thenAnswer(invocation -> {
                    Message source =
                            invocation.getArgument(0);

                    return new MessageResponse(
                            source.getMessageId(),
                            CONVERSATION_ID,
                            USER_ID,
                            "contenu",
                            LocalDateTime.now(),
                            List.of(),
                            null,
                            List.of(),
                            false
                    );
                });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMessagesShouldReturnMostRecentPageWhenBeforeIsNull() {
        /*
         * Le dépôt renvoie du plus récent au plus ancien : le service doit
         * ré-inverser pour exposer du plus ancien au plus récent.
         */
        when(messageRepository
                .findByConversationConversationIdOrderByMessageIdDesc(
                        eq(CONVERSATION_ID),
                        any(Pageable.class)
                ))
                .thenReturn(List.of(
                        message(20L),
                        message(19L),
                        message(18L)
                ));

        MessagePageResponse page =
                messageService.getMessages(CONVERSATION_ID, LIMIT, null);

        assertEquals(
                List.of(18L, 19L, 20L),
                page.items()
                        .stream()
                        .map(MessageResponse::id)
                        .toList()
        );

        assertFalse(page.hasMore());
    }

    @Test
    void getMessagesShouldRequestOneExtraMessageToDetectNextPage() {
        when(messageRepository
                .findByConversationConversationIdOrderByMessageIdDesc(
                        eq(CONVERSATION_ID),
                        any(Pageable.class)
                ))
                .thenReturn(List.of());

        messageService.getMessages(CONVERSATION_ID, LIMIT, null);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(messageRepository)
                .findByConversationConversationIdOrderByMessageIdDesc(
                        eq(CONVERSATION_ID),
                        captor.capture()
                );

        /*
         * Un message de plus que la limite est demandé : c'est ce message
         * surnuméraire, et non un second appel, qui signale la présence
         * d'une page suivante.
         */
        assertEquals(
                LIMIT + 1,
                captor.getValue().getPageSize()
        );
    }

    @Test
    void getMessagesShouldFlagHasMoreAndTrimToLimit() {
        /*
         * 16 messages pour une limite de 15 : le 16e est le signal de page
         * suivante et ne doit pas être renvoyé au client.
         */
        List<Message> descending = new ArrayList<>();

        for (long id = 30; id >= 15; id--) {
            descending.add(message(id));
        }

        when(messageRepository
                .findByConversationConversationIdOrderByMessageIdDesc(
                        eq(CONVERSATION_ID),
                        any(Pageable.class)
                ))
                .thenReturn(descending);

        MessagePageResponse page =
                messageService.getMessages(CONVERSATION_ID, LIMIT, null);

        assertTrue(page.hasMore());
        assertEquals(LIMIT, page.items().size());

        assertEquals(
                16L,
                page.items()
                        .get(0)
                        .id()
        );

        assertEquals(
                30L,
                page.items()
                        .get(LIMIT - 1)
                        .id()
        );
    }

    @Test
    void getMessagesShouldUseCursorQueryWhenBeforeIsProvided() {
        when(messageRepository
                .findByConversationConversationIdAndMessageIdLessThanOrderByMessageIdDesc(
                        eq(CONVERSATION_ID),
                        eq(20L),
                        any(Pageable.class)
                ))
                .thenReturn(List.of(
                        message(19L),
                        message(18L)
                ));

        MessagePageResponse page =
                messageService.getMessages(CONVERSATION_ID, LIMIT, 20L);

        assertEquals(
                List.of(18L, 19L),
                page.items()
                        .stream()
                        .map(MessageResponse::id)
                        .toList()
        );

        /*
         * Avec un curseur, la requête la plus récente ne doit pas être
         * employée : elle ignorerait la borne.
         */
        verify(messageRepository, never())
                .findByConversationConversationIdOrderByMessageIdDesc(
                        anyLong(),
                        any(Pageable.class)
                );
    }

    @Test
    void getMessagesShouldRejectNonMember() {
        Conversation conversation = new Conversation();

        conversation.setConversationId(CONVERSATION_ID);
        conversation.setMembers(List.of());

        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.of(conversation));

        assertThrows(
                BadRequestException.class,
                () -> messageService.getMessages(
                        CONVERSATION_ID,
                        LIMIT,
                        null
                )
        );

        verify(messageRepository, never())
                .findByConversationConversationIdOrderByMessageIdDesc(
                        anyLong(),
                        any(Pageable.class)
                );
    }

    @Test
    void getMessagesShouldThrowWhenConversationMissing() {
        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> messageService.getMessages(
                        CONVERSATION_ID,
                        LIMIT,
                        null
                )
        );
    }

    @Test
    void searchMessagesShouldReturnAscendingPageFromDescendingResults() {
        when(messageRepository.search(
                eq(CONVERSATION_ID),
                eq("bonjour"),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of(
                message(50L),
                message(49L)
        ));

        MessagePageResponse page =
                messageService.searchMessages(
                        CONVERSATION_ID,
                        "bonjour",
                        LIMIT,
                        null
                );

        assertEquals(
                List.of(49L, 50L),
                page.items()
                        .stream()
                        .map(MessageResponse::id)
                        .toList()
        );

        assertFalse(page.hasMore());
    }

    @Test
    void searchMessagesShouldTrimQueryBeforeHittingRepository() {
        when(messageRepository.search(
                eq(CONVERSATION_ID),
                anyString(),
                any(),
                any(Pageable.class)
        )).thenReturn(List.of());

        messageService.searchMessages(
                CONVERSATION_ID,
                "  rapport  ",
                LIMIT,
                null
        );

        /*
         * Les espaces de saisie ne doivent pas être conservés dans le LIKE,
         * sinon une recherche Accidentellement préfixée ne trouverait rien.
         */
        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(messageRepository).search(
                eq(CONVERSATION_ID),
                captor.capture(),
                any(),
                any(Pageable.class)
        );

        assertEquals("rapport", captor.getValue());
    }

    @Test
    void searchMessagesShouldFlagHasMoreAndTrimToLimit() {
        List<Message> descending = new ArrayList<>();

        for (long id = 70; id >= 55; id--) {
            descending.add(message(id));
        }

        when(messageRepository.search(
                eq(CONVERSATION_ID),
                anyString(),
                eq(null),
                any(Pageable.class)
        )).thenReturn(descending);

        MessagePageResponse page =
                messageService.searchMessages(
                        CONVERSATION_ID,
                        "terme",
                        LIMIT,
                        null
                );

        assertTrue(page.hasMore());
        assertEquals(LIMIT, page.items().size());

        assertEquals(
                56L,
                page.items()
                        .get(0)
                        .id()
        );
    }

    @Test
    void searchMessagesShouldRejectNonMember() {
        Conversation conversation = new Conversation();

        conversation.setConversationId(CONVERSATION_ID);
        conversation.setMembers(List.of());

        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.of(conversation));

        assertThrows(
                BadRequestException.class,
                () -> messageService.searchMessages(
                        CONVERSATION_ID,
                        "bonjour",
                        LIMIT,
                        null
                )
        );

        verify(messageRepository, never()).search(
                anyLong(),
                anyString(),
                any(),
                any(Pageable.class)
        );
    }

    @Test
    void searchMessagesShouldPropagateCursorToRepository() {
        ArgumentCaptor<Long> cursorCaptor =
                ArgumentCaptor.forClass(Long.class);

        when(messageRepository.search(
                eq(CONVERSATION_ID),
                anyString(),
                cursorCaptor.capture(),
                any(Pageable.class)
        )).thenReturn(List.of());

        messageService.searchMessages(
                CONVERSATION_ID,
                "bonjour",
                LIMIT,
                42L
        );

        /*
         * La pagination des résultats de recherche doit suivre le même
         * curseur que la liste des messages.
         */
        assertEquals(42L, cursorCaptor.getValue());
    }

    private void stubMember() {
        User user = new User();

        user.setUsersId(USER_ID);
        user.setEmail(USER_EMAIL);

        ConversationMember member = new ConversationMember();

        member.setUser(user);

        Conversation conversation = new Conversation();

        conversation.setConversationId(CONVERSATION_ID);
        conversation.setMembers(List.of(member));

        when(userRepository.findByEmail(USER_EMAIL))
                .thenReturn(Optional.of(user));

        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.of(conversation));
    }

    private Message message(Long id) {
        Message message = new Message();

        message.setMessageId(id);

        return message;
    }

    private void authenticate(String email) {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ADMIN")
                        )
                );

        context.setAuthentication(auth);

        SecurityContextHolder.setContext(context);
    }
}