package com.school.security.controllers.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.security.services.contracts.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(conversationController)
                        .build();
    }

    @Test
    void getUnreadCountShouldReturn200WithRawCount() throws Exception {
        when(conversationService.getUnreadCount()).thenReturn(5);

        mockMvc.perform(
                        get("/conversations/unread-count")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getUnreadCountShouldReturnZeroWhenNothingUnread() throws Exception {
        when(conversationService.getUnreadCount()).thenReturn(0);

        mockMvc.perform(
                        get("/conversations/unread-count")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    void getUnreadCountShouldReflectAnotherUserCount() throws Exception {
        when(conversationService.getUnreadCount()).thenReturn(9);

        mockMvc.perform(
                        get("/conversations/unread-count")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(9));
    }
}