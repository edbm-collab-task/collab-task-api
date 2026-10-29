package com.school.security.controllers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.security.entities.Attachment;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.repositories.AttachmentRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.FileStorageService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests de {@link TaskAttachmentController} après la fusion des tables
 * {@code task_attachments} et {@code message_attachments} en {@code attachments}.
 *
 * <p>Ils verrouillent deux choses :
 * <ul>
 *   <li>le contrat JSON exposé au client reste identique (le champ {@code name}
 *       contient toujours le chemin de stockage, pas le nom du fichier) ;</li>
 *   <li>une pièce jointe créée depuis une tâche porte bien {@code task} et non
 *       {@code message}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TaskAttachmentControllerTest {

    private MockMvc mockMvc;

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private TaskAttachmentController controller;

    @Captor private ArgumentCaptor<Attachment> attachmentCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("admin@edbm.test", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── GET /tasks/{taskId}/attachments ────────────────────────────

    @Test
    void listShouldKeepLegacyJsonContract() throws Exception {
        when(attachmentRepository.findByTaskTaskIdOrderByUploadedAtDesc(885L))
                .thenReturn(List.of(attachment()));

        mockMvc.perform(get("/tasks/885/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                // « name » = chemin de stockage, comme avant la fusion
                .andExpect(jsonPath("$[0].name").value("uploads/tasks/abc.pdf"))
                .andExpect(jsonPath("$[0].originalName").value("rapport.pdf"))
                .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$[0].size").value(1024))
                .andExpect(jsonPath("$[0].uploadedByName").value("Admin EDBM"));
    }

    // ─── POST /tasks/{taskId}/attachments ───────────────────────────

    @Test
    void uploadShouldAttachToTaskAndDeriveStoredName() throws Exception {
        Task task = new Task();
        task.setTaskId(885L);
        User user = user();

        when(taskRepository.findById(885L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(fileStorageService.saveTaskAttachment(any()))
                .thenReturn("uploads/tasks/9f9c16b1.pdf");
        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "rapport.pdf", "application/pdf", "contenu".getBytes());

        mockMvc.perform(multipart("/tasks/885/attachments").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("rapport.pdf"))
                .andExpect(jsonPath("$.name").value("uploads/tasks/9f9c16b1.pdf"));

        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment saved = attachmentCaptor.getValue();
        assertEquals(task, saved.getTask());
        assertNull(saved.getMessage(), "une pièce jointe de tâche ne doit pas avoir de message");
        assertEquals("rapport.pdf", saved.getOriginalName());
        assertEquals("9f9c16b1.pdf", saved.getStoredName());
        assertEquals("application/pdf", saved.getContentType());
        assertEquals(7L, saved.getSize());
        assertEquals("uploads/tasks/9f9c16b1.pdf", saved.getPath());
        assertNotNull(saved.getUploadedAt());
        assertEquals(user, saved.getUploadedBy());
    }

    // ─── DELETE /tasks/attachments/{id} ─────────────────────────────

    @Test
    void deleteShouldRemoveFileAndRow() throws Exception {
        Attachment attachment = attachment();
        when(attachmentRepository.findById(12L)).thenReturn(Optional.of(attachment));

        mockMvc.perform(delete("/tasks/attachments/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pièce jointe supprimée"));

        verify(fileStorageService)
                .deleteTaskAttachment(eq("uploads/tasks/abc.pdf"));
        verify(attachmentRepository).delete(attachment);
    }

    // ─── helpers ────────────────────────────────────────────────────

    private User user() {
        User user = new User();
        user.setFirstname("Admin");
        user.setLastname("EDBM");
        return user;
    }

    private Attachment attachment() {
        return Attachment.builder()
                .attachmentId(12L)
                .originalName("rapport.pdf")
                .storedName("abc.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .path("uploads/tasks/abc.pdf")
                .uploadedAt(LocalDateTime.of(2026, 10, 2, 15, 30))
                .uploadedBy(user())
                .build();
    }
}
