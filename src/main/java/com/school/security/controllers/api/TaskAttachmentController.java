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

/**
 * Contrôleur de gestion des pièces jointes associées aux tâches.
 *
 * <p>Pas de préfixe {@code @RequestMapping} au niveau de la classe : chaque
 * endpoint définit son propre chemin sous {@code /tasks/...}. Ce contrôleur
 * n'appelle aucun service métier dédié : il opère directement sur les
 * repositories ({@code TaskAttachmentRepository}, {@code TaskRepository},
 * {@code UserRepository}) et délègue le stockage physique des fichiers à
 * {@code FileStorageService}.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : tous les endpoints
 *       reposent sur la seule règle globale {@code anyRequest().authenticated()}
 *       de {@code SecurityConfig} ; aucun contrôle d'accès par projet, par
 *       rôle ou par appartenance n'est appliqué ;</li>
 *   <li>la suppression ({@code DELETE /tasks/attachments/{id}}) est une
 *       suppression PHYSIQUE : le fichier est effacé du disque ET
 *       l'enregistrement est supprimé de la base ;</li>
 *   <li>le chemin de téléchargement ({@code /tasks/attachments/{id}/download})
 *       n'inclut pas l'identifiant de la tâche : l'accès repose uniquement
 *       sur l'identifiant de la pièce jointe ;</li>
 *   <li>les champs {@code name} et {@code path} de l'entité
 *       {@code TaskAttachment} sont renseignés avec la même valeur
 *       ({@code filePath}) ;</li>
 *   <li>aucune validation de type ou de taille de fichier n'est réalisée au
 *       niveau du contrôleur ;</li>
 *   <li>injection de dépendances par champs ({@code @Autowired}).</li>
 * </ul>
 */
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

    /**
     * Liste les pièces jointes d'une tâche ({@code GET /tasks/{taskId}/attachments}).
     *
     * <p>Les pièces jointes sont retournées de la plus récente à la plus
     * ancienne (tri {@code uploadedAt} décroissant). Aucun contrôle de
     * projet ou de membership n'est appliqué : tout utilisateur authentifié
     * peut consulter les pièces jointes de n'importe quelle tâche.
     */
    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<List<TaskAttachmentResDto>> getAttachments(@PathVariable Long taskId) {
        List<TaskAttachment> attachments = attachmentRepository.findByTaskTaskIdOrderByUploadedAtDesc(taskId);
        List<TaskAttachmentResDto> dtos = attachments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Upload d'une pièce jointe sur une tâche ({@code POST /tasks/{taskId}/attachments}).
     *
     * <p>Le corps de la requête est au format {@code multipart/form-data} avec
     * un champ {@code file}. L'identifiant de l'utilisateur courant est résolu
     * via le {@code SecurityContext}. Le fichier est stocké physiquement par
     * {@code FileStorageService.saveTaskAttachment} puis un enregistrement
     * {@code TaskAttachment} est persisté en base.
     *
     * <p>Point d'attention : aucune restriction de type de fichier ni de
     * taille n'est appliquée au niveau du contrôleur ; seuls les contrôles
     * éventuels dans {@code FileStorageService} s'appliquent.
     */
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

    /**
     * Téléchargement d'une pièce jointe ({@code GET /tasks/attachments/{id}/download}).
     *
     * <p>Le chemin d'accès ne porte que l'identifiant de la pièce jointe (pas
     * l'identifiant de la tâche). L'enregistrement est chargé en base puis le
     * fichier est lu via {@code FileStorageService.loadTaskAttachment}. Le
     * Content-Type est repris de l'enregistrement ; en cas d'absence, le
     * fallback est {@code application/octet-stream}.
     *
     * <p>Aucun contrôle d'accès n'est appliqué ici : tout utilisateur
     * authentifié peut télécharger n'importe quelle pièce jointe à partir de
     * son identifiant.
     */
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

    /**
     * Suppression PHYSIQUE d'une pièce jointe ({@code DELETE /tasks/attachments/{id}}).
     *
     * <p>La suppression est double : le fichier est effacé du disque via
     * {@code FileStorageService.deleteTaskAttachment} puis l'enregistrement
     * est supprimé de la base ({@code attachmentRepository.delete}). Il ne
     * s'agit pas d'un archivage/logique delete : la donnée est définitivement
     * perdue.
     *
     * <p>Aucun contrôle de propriété n'est appliqué : tout utilisateur
     * authentifié peut supprimer n'importe quelle pièce jointe à partir de
     * son identifiant.
     */
    @DeleteMapping("/tasks/attachments/{id}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long id) {
        TaskAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

        fileStorageService.deleteTaskAttachment(attachment.getPath());
        attachmentRepository.delete(attachment);

        return ResponseEntity.ok(Map.of("message", "Pièce jointe supprimée"));
    }

    /**
     * Conversion d'une entité {@code TaskAttachment} en DTO de réponse.
     *
     * <p>Le nom de l'uploadeur est reconstruit à partir du prénom et du nom
     * (null-safe) ; si l'entité {@code uploadedBy} est elle-même nulle, une
     * chaîne vide est utilisée.
     */
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
