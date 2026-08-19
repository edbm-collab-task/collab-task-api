package com.school.security.securities.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path uploadPath;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Impossible de créer le dossier upload",
                    e
            );
        }
    }

    public String saveUserImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "L'image ne doit pas dépasser 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                (!contentType.equals("image/jpeg")
                        && !contentType.equals("image/png")
                        && !contentType.equals("image/webp"))) {

            throw new IllegalArgumentException(
                    "Format d'image non supporté"
            );
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };

        String fileName = UUID.randomUUID() + extension;

        Path targetPath = uploadPath
                .resolve("users")
                .resolve(fileName)
                .normalize();

        try {
            Files.createDirectories(targetPath.getParent());

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "uploads/users/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la sauvegarde de l'image",
                    e
            );
        }
    }

    public void deleteUserImage(String imagePath) {

        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        try {
            String fileName = Paths.get(imagePath)
                    .getFileName()
                    .toString();

            Path image = uploadPath
                    .resolve("users")
                    .resolve(fileName)
                    .normalize();

            Files.deleteIfExists(image);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la suppression de l'image",
                    e
            );
        }
    }

    public Resource loadUserImage(String imagePath) {

        try {

            String fileName = Paths
                    .get(imagePath)
                    .getFileName()
                    .toString();

            Path path = uploadPath
                    .resolve("users")
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Image not found");
            }

            return resource;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Impossible de charger l'image",
                    e
            );
        }
    }

    public String saveTaskAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Le fichier ne doit pas dépasser 10 MB"
            );
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;

        Path targetPath = uploadPath
                .resolve("tasks")
                .resolve(fileName)
                .normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return "uploads/tasks/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la sauvegarde du fichier",
                    e
            );
        }
    }

    public void deleteTaskAttachment(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            String fileName = Paths.get(filePath)
                    .getFileName()
                    .toString();
            Path file = uploadPath
                    .resolve("tasks")
                    .resolve(fileName)
                    .normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la suppression du fichier",
                    e
            );
        }
    }

    public Resource loadTaskAttachment(String filePath) {
        try {
            String fileName = Paths.get(filePath)
                    .getFileName()
                    .toString();
            Path path = uploadPath
                    .resolve("tasks")
                    .resolve(fileName)
                    .normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found");
            }
            return resource;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Impossible de charger le fichier",
                    e
            );
        }
    }
}