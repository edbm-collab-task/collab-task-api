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

/**
 * Service centralisé de stockage physique des fichiers (images utilisateur,
 * pièces jointes de messages, pièces jointes de tâches).
 *
 * <p>Organisation sur disque : le répertoire racine est fourni par la
 * propriété {@code app.upload.dir} (défaut {@code uploads}), rendu absolu et
 * normalisé à la construction. Chaque usage dispose de son sous-dossier :
 * {@code users/}, {@code messages/}, {@code tasks/}.
 *
 * <p>Règles transverses constatées (documentées, non modifiées) :
 * <ul>
 *   <li>taille maximale commune {@link #MAX_FILE_SIZE} = 10 Mio, vérifiée à
 *       l'écriture (mais pas au chargement) ; dépassement →
 *       {@code IllegalArgumentException} ;</li>
 *   <li>les noms de fichiers sont générés à partir d'un UUID
 *       ({@code UUID.randomUUID()}), ce qui évite la reprise d'un nom
 *       fourni par le client ;</li>
 *   <li>les chemins sont {@code normalize()}és et, en suppression/chargement,
 *       seul le DERNIER segment du chemin fourni est conservé
 *       ({@code Paths.get(...).getFileName()}) ; il n'existe PAS de contrôle
 *       explicite de confinement (aucun {@code startsWith(uploadPath)}) ;</li>
 *   <li>la suppression des fichiers est PHYSIQUE ({@code Files.deleteIfExists}),
 *       sans corbeille ni suppression logique ;</li>
 *   <li>les URL retournées sont des chemins relatifs préfixés en dur par
 *       {@code "uploads/..."}, indépendamment de la valeur réelle de
 *       {@code app.upload.dir}.</li>
 * </ul>
 */
@Service
public class FileStorageService {

    /**
     * Taille maximale acceptée pour un fichier : 10 Mio (10 × 1024 × 1024).
     * Appliquée aux images, pièces jointes de messages et de tâches.
     */
    private static final long MAX_FILE_SIZE =
            10 * 1024 * 1024;

    /**
     * Répertoire racine absolu et normalisé sous lequel tous les fichiers
     * sont écrits/lus. Chaque méthode lui ajoute son sous-dossier et un nom
     * de fichier généré.
     */
    private final Path uploadPath;

    /**
     * Construit le répertoire racine depuis {@code app.upload.dir}
     * (défaut {@code uploads}) et le crée s'il n'existe pas. Un échec de
     * création lève une {@code RuntimeException}.
     */
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

    /**
     * Sauvegarde l'image de profil d'un utilisateur dans {@code users/}.
     *
     * <p>Fichier nul ou vide → retourne {@code null} (pas d'écriture).
     * Validation par {@link #validateImage} : taille ≤ 10 Mio ET type MIME
     * limité à {@code image/jpeg}, {@code image/png} ou {@code image/webp}
     * (sinon {@code IllegalArgumentException}).
     *
     * <p>L'extension est ici déduite UNIQUEMENT du content-type (pas du nom
     * d'origine) ; un content-type inattendu donnerait un nom sans extension
     * (chaîne vide). Retourne un chemin relatif {@code "uploads/users/<uuid>"}.
     */
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

    /**
     * Sauvegarde une pièce jointe de message dans {@code messages/}.
     *
     * <p>Fichier nul ou vide → {@code null}. Taille > 10 Mio →
     * {@code IllegalArgumentException}. Contrairement aux images, AUCUN
     * contrôle du type MIME n'est effectué : tout type est accepté, avec un
     * repli {@code application/octet-stream} si le content-type est absent.
     * L'extension provient de {@link #getExtension} (nom d'origine si
     * plausible, sinon content-type). Retourne
     * {@code "uploads/messages/<uuid>"}.
     */
    public String saveMessageAttachment(
            MultipartFile file
    ) {

        if (file == null ||
                file.isEmpty()) {

            return null;
        }

        // Limite de taille appliquée uniquement à l'écriture.
        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "La pièce jointe ne doit pas dépasser 10 MB."
            );
        }

        String contentType =
                file.getContentType();

        // Repli si le client n'a pas fourni de content-type.
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

    /**
     * Supprime physiquement l'image d'un utilisateur. Délègue à
     * {@link #deleteFile} avec le sous-dossier {@code users}. Chemin nul ou
     * vide → aucun effet ; l'absence du fichier n'est pas une erreur
     * ({@code deleteIfExists}).
     */
    public void deleteUserImage(
            String imagePath
    ) {

        deleteFile(
                imagePath,
                "users"
        );
    }

    /**
     * Supprime physiquement une pièce jointe de message. Délègue à
     * {@link #deleteFile} avec le sous-dossier {@code messages}.
     */
    public void deleteMessageAttachment(
            String attachmentPath
    ) {

        deleteFile(
                attachmentPath,
                "messages"
        );
    }

    /**
     * Charge l'image d'un utilisateur. Délègue à {@link #loadFile} avec le
     * sous-dossier {@code users} ; lève une {@code RuntimeException} si le
     * fichier est absent ou illisible.
     */
    public Resource loadUserImage(
            String imagePath
    ) {

        return loadFile(
                imagePath,
                "users"
        );
    }

    /**
     * Charge une pièce jointe de message. Délègue à {@link #loadFile} avec le
     * sous-dossier {@code messages}.
     */
    public Resource loadMessageAttachment(
            String attachmentPath
    ) {

        return loadFile(
                attachmentPath,
                "messages"
        );
    }

    /**
     * Valide une image destinée au profil utilisateur.
     *
     * <p>Deux contrôles : taille ≤ {@link #MAX_FILE_SIZE} et content-type
     * strictement égal à {@code image/jpeg}, {@code image/png} ou
     * {@code image/webp} (un content-type nul est refusé). Toute violation
     * lève {@code IllegalArgumentException}. Aucun contrôle du contenu réel
     * du fichier (magic bytes) n'est effectué.
     */
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

    /**
     * Détermine l'extension à appliquer au fichier stocké.
     *
     * <p>Priorité au nom d'origine : si celui-ci contient un point et que le
     * suffixe obtenu fait au plus 10 caractères, il est repris tel quel
     * (ex. {@code .docx}). Sinon, repli sur le content-type
     * ({@code image/jpeg} → {@code .jpg}, {@code image/png} → {@code .png},
     * {@code image/webp} → {@code .webp}, {@code application/pdf} →
     * {@code .pdf}) et par défaut {@code .bin}.
     */
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

            // Garde-fou : on n'accepte le suffixe du nom d'origine que s'il
            // reste court (heuristique contre les noms fantaisistes).
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

    /**
     * Supprime physiquement un fichier dans le sous-dossier indiqué.
     *
     * <p>Chemin nul ou vide → aucun effet. Seul le dernier segment du chemin
     * fourni est utilisé ({@code getFileName()}), puis résolu sous
     * {@code uploadPath/<folder>} et normalisé : les éventuels segments de
     * répertoire du chemin d'entrée sont donc ignorés. L'absence du fichier
     * n'est pas une erreur. Toute {@code IOException} est encapsulée dans une
     * {@code RuntimeException}.
     */
    private void deleteFile(
            String filePath,
            String folder
    ) {

        if (filePath == null ||
                filePath.isBlank()) {

            return;
        }

        try {

            // Réduction au nom de fichier : neutralise les préfixes de chemin.
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

    /**
     * Charge un fichier du sous-dossier indiqué sous forme de
     * {@link Resource} URL.
     *
     * <p>Comme {@link #deleteFile}, seul le dernier segment du chemin fourni
     * est conservé, puis résolu sous {@code uploadPath/<folder>} et normalisé.
     * Si la ressource n'existe pas ou n'est pas lisible, une
     * {@code RuntimeException("Fichier introuvable.")} est levée — celle-ci
     * étant attrapée par le {@code catch} général, elle est encapsulée dans
     * une {@code RuntimeException("Impossible de charger le fichier.")} (le
     * message d'origine reste en cause).
     */
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

    /**
     * Sauvegarde une pièce jointe de tâche dans {@code tasks/}.
     *
     * <p>Fichier nul ou vide → {@code null} ; taille > 10 Mio →
     * {@code IllegalArgumentException}. AUCUN contrôle du type MIME. La
     * logique est ici dupliquée (et non déléguée à {@code getExtension} ou
     * {@code saveMessageAttachment}) : l'extension est reprise du nom
     * d'origine dès qu'il contient un point, SANS la limite de longueur
     * appliquée par {@link #getExtension}, et sans repli sur le content-type.
     * Retourne {@code "uploads/tasks/<uuid>"}.
     */
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

    /**
     * Supprime physiquement une pièce jointe de tâche. Logique inline
     * équivalente à {@link #deleteFile} pour le dossier {@code tasks} :
     * dernier segment uniquement, résolution puis normalisation sous
     * {@code tasks/}, absence tolérée.
     */
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

    /**
     * Charge une pièce jointe de tâche. Logique inline équivalente à
     * {@link #loadFile} pour le dossier {@code tasks}. Le message interne
     * d'absence est ici en anglais ({@code "File not found"}), puis
     * encapsulé dans une {@code RuntimeException} "Impossible de charger le
     * fichier".
     */
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