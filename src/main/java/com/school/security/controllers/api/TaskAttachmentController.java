package com.school.security.controllers.api;

import com.school.security.dtos.responses.TaskAttachmentResDto;
import com.school.security.entities.Task;
import com.school.security.entities.TaskAttachment;
import com.school.security.entities.User;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.TaskAttachmentRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class TaskAttachmentController {

    @Autowired
    private TaskAttachmentRepository attachmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<List<TaskAttachmentResDto>> getAttachments(@PathVariable Long taskId) {
        List<TaskAttachment> attachments = attachmentRepository.findByTaskTaskIdOrderByUploadedAtDesc(taskId);
        List<TaskAttachmentResDto> dtos = attachments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskAttachmentResDto> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String filePath = fileStorageService.saveTaskAttachment(file);

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setName(filePath);
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setPath(filePath);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setUploadedBy(user);

        TaskAttachment saved = attachmentRepository.save(attachment);
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/tasks/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) throws IOException {
        TaskAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

        Resource resource = fileStorageService.loadTaskAttachment(attachment.getPath());

        String contentType = attachment.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition",
                        "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/tasks/attachments/{id}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long id) {
        TaskAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

        fileStorageService.deleteTaskAttachment(attachment.getPath());
        attachmentRepository.delete(attachment);

        return ResponseEntity.ok(Map.of("message", "Pièce jointe supprimée"));
    }

    private TaskAttachmentResDto toDto(TaskAttachment entity) {
        String uploadedByName = "";
        if (entity.getUploadedBy() != null) {
            uploadedByName = (entity.getUploadedBy().getFirstname() != null ? entity.getUploadedBy().getFirstname() : "")
                    + " " + (entity.getUploadedBy().getLastname() != null ? entity.getUploadedBy().getLastname() : "");
            uploadedByName = uploadedByName.trim();
        }
        return new TaskAttachmentResDto(
                entity.getAttachmentId(),
                entity.getName(),
                entity.getOriginalName(),
                entity.getContentType(),
                entity.getSize(),
                entity.getUploadedAt(),
                uploadedByName
        );
    }
}
