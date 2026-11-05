package com.school.security.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Pièce jointe, rattachée soit à une tâche, soit à un message.
 *
 * <p>Cette entité remplace les deux tables historiques
 * {@code task_attachments} et {@code message_attachments}, qui décrivaient le
 * même concept avec des noms de colonnes différents.
 *
 * <p>Règles :
 * <ul>
 *   <li><strong>exactement un parent</strong> : {@code task} ou {@code message}
 *       est renseigné, jamais les deux ni aucun des deux. La règle est portée
 *       par la contrainte SQL {@code attachments_one_parent} (voir
 *       {@code data.sql}, section 19) : Hibernate ne l'ajoute pas sur une table
 *       existante ;</li>
 *   <li>{@code originalName} : nom du fichier fourni par le client ;</li>
 *   <li>{@code storedName} : nom du fichier sur le disque (UUID + extension) ;</li>
 *   <li>{@code path} : chemin relatif complet, préfixé par le dossier de
 *       stockage ({@code uploads/tasks/...} ou {@code uploads/messages/...}) ;</li>
 *   <li>{@code uploadedBy} est nullable : les pièces jointes de message
 *       n'enregistraient pas l'auteur à l'origine.</li>
 * </ul>
 *
 * <p>{@code @EqualsAndHashCode} exclut les associations : sans cela, la
 * relation bidirectionnelle {@code Message#attachments} provoquerait une
 * récursion infinie.
 */
@Entity
@Table(name = "attachments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(exclude = {"task", "message", "uploadedBy"})
public class Attachment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_name", nullable = false)
    private String storedName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
}
