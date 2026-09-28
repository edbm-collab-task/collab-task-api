package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.entities.User;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.mappers.ChatUserMapper;
import com.school.security.mappers.ConversationMapper;
import com.school.security.mappers.ConversationMemberMapper;
import com.school.security.repositories.ConversationMemberRepository;
import com.school.security.repositories.ConversationRepository;
import com.school.security.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private ConversationMemberMapper memberMapper;

    @Mock
    private ChatUserMapper chatUserMapper;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = buildUser(5L, "alice@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUnreadCountShouldReturnZeroWhenNoUnreadMessages() {
        authenticate("alice@test.com");
        stubCurrentUser();

        when(memberRepository.sumUnreadCountByUserUsersId(5L)).thenReturn(0L);

        int result = conversationService.getUnreadCount();

        assertEquals(0, result);
        verify(memberRepository, times(1)).sumUnreadCountByUserUsersId(5L);
    }

    @Test
    void getUnreadCountShouldSumUnreadCountsAcrossConversations() {
        authenticate("alice@test.com");
        stubCurrentUser();

        when(memberRepository.sumUnreadCountByUserUsersId(5L)).thenReturn(5L);

        int result = conversationService.getUnreadCount();

        assertEquals(5, result);
        verify(memberRepository, times(1)).sumUnreadCountByUserUsersId(5L);
    }

    @Test
    void getUnreadCountShouldBeScopedToAuthenticatedUser() {
        authenticate("bob@test.com");
        when(userRepository.findByEmail("bob@test.com"))
                .thenReturn(Optional.of(buildUser(9L, "bob@test.com")));

        when(memberRepository.sumUnreadCountByUserUsersId(9L)).thenReturn(3L);

        int result = conversationService.getUnreadCount();

        assertEquals(3, result);
        verify(memberRepository, times(1)).sumUnreadCountByUserUsersId(9L);
        verify(memberRepository, times(0)).sumUnreadCountByUserUsersId(5L);
    }

    @Test
    void getUnreadCountShouldThrowWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(
                BadRequestException.class,
                () -> conversationService.getUnreadCount()
        );
    }

    @Test
    void getUnreadCountShouldThrowWhenAuthenticatedEmailUnknown() {
        authenticate("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> conversationService.getUnreadCount()
        );
    }

    private void stubCurrentUser() {
        when(userRepository.findByEmail("alice@test.com"))
                .thenReturn(Optional.of(currentUser));
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

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setUsersId(id);
        user.setEmail(email);
        return user;
    }
}