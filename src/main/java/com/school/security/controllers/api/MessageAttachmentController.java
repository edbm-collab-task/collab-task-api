package com.school.security.controllers.api;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads/messages")
/**
 * Contrôleur HTTP des fichiers joints aux messages.
 *
 * <p>Il n'expose pas les métadonnées en base mais le fichier physique lui-
 * même, résolu depuis le répertoire local utilisé par le stockage des pièces
 * jointes de messages.
 *
 * <p>Ce contrôleur complète le flux du {@link MessageController} : les
 * messages sont manipulés côté conversation, tandis que les fichiers sont
 * servis séparément depuis le stockage local.
 */
public class MessageAttachmentController {

    private final Path uploadPath =
            Paths.get(
                    System.getProperty("user.dir"),
                    "uploads",
                    "messages"
            );

    /**
     * Expose un fichier joint de message à partir de son nom de fichier.
     *
     * <p>Le chemin demandé est résolu relativement au dossier local des
     * pièces jointes de messages, puis normalisé avant toute lecture. Le
     * contrôleur vérifie ensuite que le chemin reste bien à l'intérieur du
     * répertoire attendu et que le fichier existe réellement avant de le
     * renvoyer.
     *
     * <p>Le contenu est servi comme {@link Resource} avec un type MIME déduit
     * du fichier lorsqu'il est disponible, sinon avec un type générique
     * {@code application/octet-stream}. L'en-tête {@code Content-Disposition}
     * est positionné en {@code inline} pour permettre l'affichage ou le
     * téléchargement côté client selon le type du fichier.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getAttachment(
            @PathVariable String filename
    ) {

        try {

            Path filePath =
                    uploadPath.resolve(filename)
                            .normalize();

            // Empêche toute sortie du répertoire dédié aux messages si le
            // nom demandé contient des segments de chemin inattendus.
            if (!filePath.startsWith(
                    uploadPath.normalize()
            )) {
                return ResponseEntity
                        .badRequest()
                        .build();
            }

            // Le contrôleur ne lit que des fichiers réguliers présents sur
            // disque ; un chemin absent ou un répertoire est traité comme un
            // non-trouvé.
            if (!Files.exists(filePath)
                    || !Files.isRegularFile(filePath)) {
                return ResponseEntity
                        .notFound()
                        .build();
            }

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            String contentType =
                    Files.probeContentType(filePath);

            if (contentType == null) {
                contentType =
                        "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" +
                                    filename +
                                    "\""
                    )
                    .body(resource);

        } catch (MalformedURLException e) {

                        // Un problème d'URL du resource est renvoyé tel quel en erreur
                        // serveur sans détail supplémentaire côté client.
            return ResponseEntity
                    .internalServerError()
                    .build();
        } catch (Exception e) {

                        // Toute autre erreur de lecture ou de résolution du fichier
                        // est traitée comme une erreur serveur générique.
            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}