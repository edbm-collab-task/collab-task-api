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
@RequestMapping("/uploads/comments")
public class CommentAttachmentController {

    private final Path uploadPath =
            Paths.get(
                    System.getProperty("user.dir"),
                    "uploads",
                    "comments"
            );

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getAttachment(
            @PathVariable String filename,
            @RequestParam(value = "download", required = false) Boolean download
    ) {

        try {

            Path filePath =
                    uploadPath.resolve(filename)
                            .normalize();

            if (!filePath.startsWith(
                    uploadPath.normalize()
            )) {
                return ResponseEntity
                        .badRequest()
                        .build();
            }

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

            // Si le paramètre download=true est passé, forcer le téléchargement
            if (download != null && download) {
                return ResponseEntity.ok()
                        .contentType(
                                MediaType.parseMediaType(
                                        contentType
                                )
                        )
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" +
                                        filename +
                                        "\""
                        )
                        .body(resource);
            } else {
                // Aperçu inline (par défaut)
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
            }

        } catch (MalformedURLException e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}