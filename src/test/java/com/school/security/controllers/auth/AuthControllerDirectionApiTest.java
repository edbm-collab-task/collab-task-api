package com.school.security.controllers.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.enums.Gender;
import com.school.security.exceptions.EntityException;
import com.school.security.exceptions.GlobalExceptionHandler;
import com.school.security.mappers.UserMapper;
import com.school.security.repositories.DirectionRepository;
import com.school.security.securities.services.JwtService;
import com.school.security.services.contracts.UserService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerDirectionApiTest {

    @Mock private UserService userService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;
    @Mock private DirectionRepository directionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller =
                new AuthController(
                        userService,
                        authenticationManager,
                        jwtService,
                        userMapper,
                        emailService,
                        userMapper,
                        directionRepository);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void registerShouldDeserializeExplicitNullAsEmptyDirection() throws Exception {
        when(userService.createOrUpdate(any(UserReqDto.class))).thenReturn(response());

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"alice@test.com\",\"directionId\":null}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UserReqDto> captor = ArgumentCaptor.forClass(UserReqDto.class);
        verify(userService).createOrUpdate(captor.capture());
        assertEquals(Optional.empty(), captor.getValue().directionId());
    }

    @Test
    void registerShouldKeepMissingDirectionAsNull() throws Exception {
        when(userService.createOrUpdate(any(UserReqDto.class))).thenReturn(response());

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"alice@test.com\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UserReqDto> captor = ArgumentCaptor.forClass(UserReqDto.class);
        verify(userService).createOrUpdate(captor.capture());
        assertNull(captor.getValue().directionId());
    }

    @Test
    void createShouldReturn404WhenDirectionDoesNotExist() throws Exception {
        when(userService.create(any(UserReqDto.class)))
                .thenThrow(new EntityException("Direction not found with ID 99"));

        mockMvc.perform(
                        post("/auth/create")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"alice@test.com\",\"directionId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Direction not found with ID 99"));

        ArgumentCaptor<UserReqDto> captor = ArgumentCaptor.forClass(UserReqDto.class);
        verify(userService).create(captor.capture());
        assertEquals(Optional.of(99L), captor.getValue().directionId());
    }

    @Test
    void registerShouldReturn404WhenDirectionDoesNotExist() throws Exception {
        when(userService.createOrUpdate(any(UserReqDto.class)))
                .thenThrow(new EntityException("Direction not found with ID 99"));

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"alice@test.com\",\"directionId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Direction not found with ID 99"));
    }

    private UserResDto response() {
        return new UserResDto(
                1L,
                "Alice",
                "Martin",
                "alice@test.com",
                null,
                null,
                null,
                Gender.F,
                true,
                true,
                LocalDate.now(),
                "USER",
                "USER",
                null);
    }
}
