package com.school.security.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.security.dtos.responses.MessagePageResponse;
import com.school.security.dtos.responses.MessageResponse;
import com.school.security.services.contracts.MessageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tests de la couche HTTP de la pagination et de la recherche : ce qui
 * appartient au contrôleur (liaison des paramètres, valeur par défaut,
 * bornage de la limite) est vérifié ici, la logique métier restant couverte
 * par {@code MessageServiceImplTest}.
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private static final Long CONVERSATION_ID = 100L;

    private static final int DEFAULT_LIMIT = 15;

    private MockMvc mockMvc;

    @Mock
    private MessageService messageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(messageController)
                        .build();
    }

    @Test
    void getMessagesShouldExposeItemsAndHasMore() throws Exception {
        when(messageService.getMessages(
                CONVERSATION_ID,
                DEFAULT_LIMIT,
                null
        )).thenReturn(
                new MessagePageResponse(
                        List.of(
                                response(1L),
                                response(2L)
                        ),
                        true
                )
        );

        mockMvc.perform(
                        get("/conversations/{id}/messages", CONVERSATION_ID)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void getMessagesShouldApplyDefaultLimitAndForwardCursor() throws Exception {
        when(messageService.getMessages(
                CONVERSATION_ID,
                DEFAULT_LIMIT,
                42L
        )).thenReturn(
                new MessagePageResponse(
                        List.of(),
                        false
                )
        );

        mockMvc.perform(
                        get("/conversations/{id}/messages", CONVERSATION_ID)
                                .param("before", "42")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));

        /*
         * Sans paramètre limit, le contrôleur doit appliquer 15 et
         * transmettre le curseur tel quel au service.
         */
        verify(messageService).getMessages(
                CONVERSATION_ID,
                DEFAULT_LIMIT,
                42L
        );
    }

    @Test
    void getMessagesShouldClampLimitAboveMaximum() throws Exception {
        when(messageService.getMessages(
                any(),
                anyInt(),
                any()
        )).thenReturn(
                new MessagePageResponse(
                        List.of(),
                        false
                )
        );

        mockMvc.perform(
                        get("/conversations/{id}/messages", CONVERSATION_ID)
                                .param("limit", "1000")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        /*
         * Une limite abusive est ramenée à 100 côté contrôleur : la
         * protection reste effective même si le service est appelé
         * directement ailleurs.
         */
        verify(messageService).getMessages(
                CONVERSATION_ID,
                100,
                null
        );
    }

    @Test
    void getMessagesShouldClampLimitBelowMinimum() throws Exception {
        when(messageService.getMessages(
                any(),
                anyInt(),
                any()
        )).thenReturn(
                new MessagePageResponse(
                        List.of(),
                        false
                )
        );

        mockMvc.perform(
                        get("/conversations/{id}/messages", CONVERSATION_ID)
                                .param("limit", "0")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(messageService).getMessages(
                CONVERSATION_ID,
                1,
                null
        );
    }

    @Test
    void searchMessagesShouldForwardQueryAndCursor() throws Exception {
        when(messageService.searchMessages(
                CONVERSATION_ID,
                "rapport",
                DEFAULT_LIMIT,
                7L
        )).thenReturn(
                new MessagePageResponse(
                        List.of(
                                response(9L)
                        ),
                        false
                )
        );

        mockMvc.perform(
                        get("/conversations/{id}/messages/search", CONVERSATION_ID)
                                .param("query", "rapport")
                                .param("before", "7")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(messageService).searchMessages(
                eq(CONVERSATION_ID),
                eq("rapport"),
                eq(DEFAULT_LIMIT),
                eq(7L)
        );
    }

    @Test
    void searchMessagesShouldRequireQueryParameter() throws Exception {
        mockMvc.perform(
                        get("/conversations/{id}/messages/search", CONVERSATION_ID)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verify(messageService, never()).searchMessages(
                any(),
                any(),
                anyInt(),
                any()
        );
    }

    @Test
    void searchMessagesShouldNotCollideWithMessageListing() throws Exception {
        when(messageService.searchMessages(
                any(),
                any(),
                anyInt(),
                any()
        )).thenReturn(
                new MessagePageResponse(
                        List.of(),
                        false
                )
        );

        /*
         * /messages/search doit être résolu par l'endpoint de recherche et
         * non interprété comme un message dont l'identifiant serait
         * "search" : les deux routes cohabitent.
         */
        mockMvc.perform(
                        get("/conversations/{id}/messages/search", CONVERSATION_ID)
                                .param("query", "terme")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(messageService, never()).getMessages(
                any(),
                anyInt(),
                any()
        );
    }

    private MessageResponse response(Long id) {
        return new MessageResponse(
                id,
                CONVERSATION_ID,
                5L,
                "contenu",
                LocalDateTime.now(),
                List.of(),
                null,
                List.of(),
                false
        );
    }
}