package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Direction;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.Gender;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.mappers.UserMapper;
import com.school.security.repositories.DirectionRepository;
import com.school.security.repositories.RoleRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.FileStorageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tests des flux de création et de modification d'utilisateur lorsque la
 * direction est facultative.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private DirectionRepository directionRepository;
    @Mock private EmailService emailService;
    @Mock private FileStorageService fileStorageService;
    @Mock private TaskRepository taskRepository;

    private UserServiceImpl userService;
    private Direction direction;
    private Role userRole;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        UserMapper userMapper =
                new UserMapper(new RoleMapper(), directionRepository, passwordEncoder);
        userService =
                new UserServiceImpl(
                        userMapper,
                        passwordEncoder,
                        roleRepository,
                        userRepository,
                        emailService,
                        fileStorageService,
                        taskRepository);

        direction = new Direction();
        direction.setDirectionId(1L);
        direction.setName("DSI");

        userRole = new Role();
        userRole.setRolesId(1L);
        userRole.setName("USER");
        userRole.setCodeRole("USER");
    }

    @Test
    void createShouldAttachExistingDirection() {
        stubSuccessfulNewUserCreation();
        when(directionRepository.findById(1L)).thenReturn(Optional.of(direction));

        UserResDto result =
                userService.create(
                        request("alice@test.com", Optional.of(1L), "USER"));

        assertEquals("DSI", result.direction());
        verify(directionRepository).findById(1L);
    }

    @Test
    void createShouldAcceptMissingDirection() {
        stubSuccessfulNewUserCreation();

        UserResDto result =
                userService.create(
                        request("alice@test.com", Optional.empty(), "USER"));

        assertNull(result.direction());
        verifyNoInteractions(directionRepository);
    }

    @Test
    void createShouldAcceptAbsentDirectionField() {
        stubSuccessfulNewUserCreation();

        UserResDto result =
                userService.create(request("alice@test.com", null, "USER"));

        assertNull(result.direction());
        verifyNoInteractions(directionRepository);
    }

    @Test
    void createShouldRejectUnknownDirection() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(directionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityException.class,
                () ->
                        userService.create(
                                request("alice@test.com", Optional.of(99L), "USER")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateShouldAttachExistingDirection() {
        User existing = existingUserWithoutDirection();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(directionRepository.findById(1L)).thenReturn(Optional.of(direction));

        UserResDto result =
                userService.createOrUpdate(
                        request("alice@test.com", Optional.of(1L), null));

        assertEquals("DSI", result.direction());
        assertEquals(direction, existing.getDirection());
        verify(directionRepository).findById(1L);
    }

    @Test
    void updateShouldRemoveDirectionWhenExplicitlyEmpty() {
        User existing = existingUserWithDirection();
        existing.setFirstname("Ancien");
        existing.setLastname("Nom");
        existing.setJob("Ancien poste");
        existing.setNumber("0300000000");
        existing.setGender(Gender.M);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserReqDto clearDirection =
                new UserReqDto(
                        null,
                        null,
                        null,
                        Optional.empty(),
                        "alice@test.com",
                        null,
                        null,
                        null,
                        null,
                        null);

        UserResDto result = userService.createOrUpdate(clearDirection);

        assertNull(result.direction());
        assertNull(existing.getDirection());
        assertEquals("Ancien", existing.getFirstname());
        assertEquals("Nom", existing.getLastname());
        assertEquals("Ancien poste", existing.getJob());
        assertEquals("0300000000", existing.getNumber());
        assertEquals(Gender.M, existing.getGender());
        verifyNoInteractions(directionRepository);
    }

    @Test
    void updateShouldRejectUnknownDirection() {
        User existing = existingUserWithDirection();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(existing));
        when(directionRepository.findById(99L)).thenReturn(Optional.empty());

        EntityException exception =
                assertThrows(
                        EntityException.class,
                        () ->
                                userService.createOrUpdate(
                                        request("alice@test.com", Optional.of(99L), null)));

        assertEquals("Direction not found with ID 99", exception.getMessage());
        assertEquals(direction, existing.getDirection());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateShouldPreserveDirectionWhenFieldIsAbsent() {
        User existing = existingUserWithDirection();
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResDto result =
                userService.createOrUpdate(request("alice@test.com", null, null));

        assertEquals("DSI", result.direction());
        assertEquals(direction, existing.getDirection());
        verifyNoInteractions(directionRepository);
    }

    @Test
    void findByIdShouldReturnUserWithoutDirection() {
        User existing = existingUserWithoutDirection();
        existing.setUsersId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));

        UserResDto result = userService.findById(5L);

        assertNull(result.direction());
        assertEquals("alice@test.com", result.email());
    }

    private void stubSuccessfulNewUserCreation() {
        AtomicReference<User> savedUser = new AtomicReference<>();
        when(userRepository.findByEmail("alice@test.com"))
                .thenAnswer(
                        invocation -> {
                            User user = savedUser.get();
                            return user == null ? Optional.empty() : Optional.of(user);
                        });
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            if (user.getUsersId() == null) {
                                user.setUsersId(10L);
                            }
                            savedUser.set(user);
                            return user;
                        });
        when(roleRepository.findByCodeRole("USER")).thenReturn(Optional.of(userRole));
    }

    private UserReqDto request(String email, Optional<Long> directionId, String role) {
        return new UserReqDto(
                "Alice",
                "Martin",
                "Développeuse",
                directionId,
                email,
                "0340000000",
                false,
                "password123",
                Gender.F,
                role);
    }

    private User existingUserWithDirection() {
        User user = existingUserWithoutDirection();
        user.setDirection(direction);
        return user;
    }

    private User existingUserWithoutDirection() {
        User user = new User();
        user.setUsersId(5L);
        user.setFirstname("Alice");
        user.setLastname("Martin");
        user.setEmail("alice@test.com");
        user.setRoles(new ArrayList<>(List.of(userRole)));
        return user;
    }
}
