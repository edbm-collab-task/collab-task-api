package com.school.security.controllers.api;

import com.school.security.dtos.responses.TaskAttachmentResDto;
import com.school.security.entities.Attachment;
import com.school.security.entities.Task;
import com.school.security.entities.User;
import com.school.security.repositories.AttachmentRepository;
import com.school.security.repositories.TaskRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Contrôleur de gestion des pièces jointes associées aux tâches.
 *
 * <p>Pas de préfixe {@code @RequestMapping} au niveau de la classe : chaque
 * endpoint définit son propre chemin sous {@code /tasks/...}. Ce contrôleur
 * n'appelle aucun service métier dédié : il opère directement sur les
 * repositories ({@code AttachmentRepository}, {@code TaskRepository},
 * {@code UserRepository}) et délègue le stockage physique des fichiers à
 * {@code FileStorageService}.
 *
 * <p>Depuis la fusion des tables {@code task_attachments} et
 * {@code message_attachments}, les pièces jointes vivent dans l'entité unique
 * {@link Attachment} ; côté tâche, la colonne {@code message_id} reste nulle.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>la liste ({@code GET}) et le téléchargement ({@code GET .../download})
 *       ne portent aucune {@code @PreAuthorize} : tout utilisateur authentifié
 *       peut les appeler ; l'upload et la suppression exigent respectivement
 *       {@code MANAGE_ATTACHMENTS/INSERT} et {@code MANAGE_ATTACHMENTS/DELETE} ;</li>
 *   <li>la suppression ({@code DELETE /tasks/attachments/{id}}) est une
 *       suppression PHYSIQUE : le fichier est effacé du disque ET
 *       l'enregistrement est supprimé de la base ;</li>
 *   <li>le chemin de téléchargement ({@code /tasks/attachments/{id}/download})
 *       n'inclut pas l'identifiant de la tâche : l'accès repose uniquement
 *       sur l'identifiant de la pièce jointe ;</li>
 *   <li>aucune validation de type ou de taille de fichier n'est réalisée au
 *       niveau du contrôleur ;</li>
 *   <li>injection de dépendances par champs ({@code @Autowired}).</li>
 * </ul>
 */
@Tag(name = "Pièces jointes de tâches", description = "Gestion des pièces jointes des tâches : liste, upload, téléchargement et suppression physique.")
@RestController
public class TaskAttachmentController {

    @Autowired
    private AttachmentRepository attachmentRepository;

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
    @Operation(summary = "Lister les pièces jointes d'une tâche", description = "Retourne la liste des pièces jointes d'une tâche, triées par date d'upload décroissante. Accessible à tout utilisateur authentifié.")
    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<List<TaskAttachmentResDto>> getAttachments(@PathVariable Long taskId) {
        List<Attachment> attachments = attachmentRepository.findByTaskTaskIdOrderByUploadedAtDesc(taskId);
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
     * {@link Attachment} est persisté en base, rattaché à la tâche.
     *
     * <p>Point d'attention : aucune restriction de type de fichier ni de
     * taille n'est appliquée au niveau du contrôleur ; seuls les contrôles
     * éventuels dans {@code FileStorageService} s'appliquent.
     */
    @Operation(summary = "Ajouter une pièce jointe", description = "Upload une pièce jointe sur une tâche (multipart/form-data). Requiert la permission MANAGE_ATTACHMENTS avec action INSERT. Le fichier est stocké physiquement et enregistré en base.")
    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_ATTACHMENTS', 'INSERT')")
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

        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setStoredName(Paths.get(filePath).getFileName().toString());
        attachment.setContentType(file.getContentType());
        attachment.setSize(file.getSize());
        attachment.setPath(filePath);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setUploadedBy(user);

        Attachment saved = attachmentRepository.save(attachment);
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
    @Operation(summary = "Télécharger une pièce jointe", description = "Télécharge le fichier associé à une pièce jointe par son identifiant. Accessible à tout utilisateur authentifié.")
    @GetMapping("/tasks/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
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
    @Operation(summary = "Supprimer une pièce jointe", description = "Suppression physique : efface le fichier du disque et supprime l'enregistrement en base. Requiert la permission MANAGE_ATTACHMENTS avec action DELETE.")
    @DeleteMapping("/tasks/attachments/{id}")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_ATTACHMENTS', 'DELETE')")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

        fileStorageService.deleteTaskAttachment(attachment.getPath());
        attachmentRepository.delete(attachment);

        return ResponseEntity.ok(Map.of("message", "Pièce jointe supprimée"));
    }

    /**
     * Conversion d'une entité {@link Attachment} en DTO de réponse.
     *
     * <p>Le nom de l'uploadeur est reconstruit à partir du prénom et du nom
     * (null-safe) ; si l'entité {@code uploadedBy} est elle-même nulle, une
     * chaîne vide est utilisée.
     *
     * <p>Le champ {@code name} du DTO conserve le chemin de stockage, comme
     * avant la fusion des tables (le frontend ne l'utilise pas : il affiche
     * {@code originalName}).
     */
    private TaskAttachmentResDto toDto(Attachment entity) {
        String uploadedByName = "";
        if (entity.getUploadedBy() != null) {
            uploadedByName = (entity.getUploadedBy().getFirstname() != null ? entity.getUploadedBy().getFirstname() : "")
                    + " " + (entity.getUploadedBy().getLastname() != null ? entity.getUploadedBy().getLastname() : "");
            uploadedByName = uploadedByName.trim();
        }
        return new TaskAttachmentResDto(
                entity.getAttachmentId(),
                entity.getPath(),
                entity.getOriginalName(),
                entity.getContentType(),
                entity.getSize(),
                entity.getUploadedAt(),
                uploadedByName
        );
    }
}
