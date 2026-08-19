package com.school.security.securities.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE =
            10 * 1024 * 1024;

    private final Path uploadPath;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}")
            String uploadDir
    ) {

        this.uploadPath =
                Paths.get(uploadDir)
                        .toAbsolutePath()
                        .normalize();

        try {

            Files.createDirectories(
                    uploadPath
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Impossible de créer le dossier upload",
                    e
            );
        }
    }

    public String saveUserImage(
            MultipartFile file
    ) {

        if (file == null ||
                file.isEmpty()) {

            return null;
        }

        validateImage(file);

        String contentType =
                file.getContentType();

        String extension =
                switch (contentType) {

                    case "image/jpeg" -> ".jpg";

                    case "image/png" -> ".png";

                    case "image/webp" -> ".webp";

                    default -> "";
                };

        String fileName =
                UUID.randomUUID()
                        + extension;

        Path targetPath =
                uploadPath
                        .resolve("users")
                        .resolve(fileName)
                        .normalize();

        try {

            Files.createDirectories(
                    targetPath.getParent()
            );

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "uploads/users/"
                    + fileName;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la sauvegarde de l'image",
                    e
            );
        }
    }

    public String saveMessageAttachment(
            MultipartFile file
    ) {

        if (file == null ||
                file.isEmpty()) {

            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "La pièce jointe ne doit pas dépasser 10 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                contentType.isBlank()) {

            contentType =
                    "application/octet-stream";
        }

        String extension =
                getExtension(
                        file.getOriginalFilename(),
                        contentType
                );

        String fileName =
                UUID.randomUUID()
                        + extension;

        Path targetPath =
                uploadPath
                        .resolve("messages")
                        .resolve(fileName)
                        .normalize();

        try {

            Files.createDirectories(
                    targetPath.getParent()
            );

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "uploads/messages/"
                    + fileName;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la sauvegarde de la pièce jointe.",
                    e
            );
        }
    }

    public void deleteUserImage(
            String imagePath
    ) {

        deleteFile(
                imagePath,
                "users"
        );
    }

    public void deleteMessageAttachment(
            String attachmentPath
    ) {

        deleteFile(
                attachmentPath,
                "messages"
        );
    }

    public Resource loadUserImage(
            String imagePath
    ) {

        return loadFile(
                imagePath,
                "users"
        );
    }

    public Resource loadMessageAttachment(
            String attachmentPath
    ) {

        return loadFile(
                attachmentPath,
                "messages"
        );
    }

    private void validateImage(
            MultipartFile file
    ) {

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "L'image ne doit pas dépasser 10 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                (!contentType.equals("image/jpeg")
                        && !contentType.equals("image/png")
                        && !contentType.equals("image/webp"))) {

            throw new IllegalArgumentException(
                    "Format d'image non supporté."
            );
        }
    }

    private String getExtension(
            String originalFilename,
            String contentType
    ) {

        if (originalFilename != null &&
                originalFilename.contains(".")) {

            String extension =
                    originalFilename.substring(
                            originalFilename
                                    .lastIndexOf(".")
                    );

            if (extension.length() <= 10) {
                return extension;
            }
        }

        return switch (contentType) {

            case "image/jpeg" -> ".jpg";

            case "image/png" -> ".png";

            case "image/webp" -> ".webp";

            case "application/pdf" -> ".pdf";

            default -> ".bin";
        };
    }

    private void deleteFile(
            String filePath,
            String folder
    ) {

        if (filePath == null ||
                filePath.isBlank()) {

            return;
        }

        try {

            String fileName =
                    Paths.get(filePath)
                            .getFileName()
                            .toString();

            Path file =
                    uploadPath
                            .resolve(folder)
                            .resolve(fileName)
                            .normalize();

            Files.deleteIfExists(
                    file
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la suppression du fichier.",
                    e
            );
        }
    }

    private Resource loadFile(
            String filePath,
            String folder
    ) {

        try {

            String fileName =
                    Paths.get(filePath)
                            .getFileName()
                            .toString();

            Path path =
                    uploadPath
                            .resolve(folder)
                            .resolve(fileName)
                            .normalize();

            Resource resource =
                    new UrlResource(
                            path.toUri()
                    );

            if (!resource.exists() ||
                    !resource.isReadable()) {

                throw new RuntimeException(
                        "Fichier introuvable."
                );
            }

            return resource;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Impossible de charger le fichier.",
                    e
            );
        }
    }
}