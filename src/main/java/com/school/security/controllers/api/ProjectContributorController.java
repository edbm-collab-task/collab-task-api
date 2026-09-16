package com.school.security.controllers.api;

import com.school.security.core.email.EmailService;
import com.school.security.core.email.TemplateEmailService;
import com.school.security.dtos.responses.ProjectContributorResDto;
import com.school.security.entities.Project;
import com.school.security.entities.ProjectContributor;
import com.school.security.entities.User;
import com.school.security.enums.ActivityType;
import com.school.security.enums.NotificationType;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.ProjectContributorRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.ActivityService;
import com.school.security.services.contracts.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur de gestion des contributeurs d'un projet, sous le préfixe
 * {@code /projects/{projectId}/contributors}.
 *
 * <p>Contrairement aux autres contrôleurs métier, ce contrôleur n'appelle
 * PAS de service dédié : il opère directement sur les repositories
 * ({@code ProjectContributorRepository}, {@code ProjectRepository},
 * {@code UserRepository}) et orchestre lui-même la logique (vérification
 * du propriétaire, unicité, création). Les services utilisés ici sont
 * uniquement les notifications, les activités et les emails.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : le contrôle d'accès est
 *       réalisé manuellement "seul le propriétaire du projet peut ajouter ou
 *       supprimer" (réponse 403 sinon) ;</li>
 *   <li>la consultation (GET) ne vérifie NI le propriétaire NI l'appartenance
 *       au projet : tout utilisateur authentifié peut lister les contributeurs
 *       d'un projet quelconque ;</li>
 *   <li>les variables sont injectées par champs via {@code @Autowired}
 *       (style différent du reste du backend qui utilise l'injection par
 *       constructeur) ;</li>
 *   <li>l'import {@code org.springframework.stereotype.Controller} est présent
 *       mais inutilisé (l'annotation utilisée est {@code @RestController}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects")
public class ProjectContributorController {

    @Autowired
    private ProjectContributorRepository contributorRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateEmailService templateEmailService;

    /**
     * Résout l'identifiant de l'utilisateur actuellement authentifié à partir
     * de l'email du {@code SecurityContext} (requête à
     * {@code UserRepository}). Lève une {@code RuntimeException} si
     * l'utilisateur est introuvable en base.
     */
    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    /**
     * Liste des contributeurs d'un projet ({@code GET /projects/{projectId}/contributors}).
     *
     * <p>Retourne les contributeurs triés du plus récent au plus ancien
     * (tri {@code addedAt} décroissant). Aucun contrôle d'accès n'est
     * appliqué ici : le propriétaire, un contributeur ou n'importe quel
     * utilisateur authentifié obtient la même liste.
     *
     * @param projectId identifiant du projet concerné
     * @return liste des contributeurs sous forme de DTO
     */
    @GetMapping("/{projectId}/contributors")
    public ResponseEntity<List<ProjectContributorResDto>> getContributors(@PathVariable Long projectId) {
        List<ProjectContributor> contributors = contributorRepository.findByProjectProjectIdOrderByAddedAtDesc(projectId);
        List<ProjectContributorResDto> dtos = contributors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Ajout d'un contributeur à un projet ({@code POST /projects/{projectId}/contributors}).
     *
     * <p>Corps de requête : une map contenant {@code userId} (pas de DTO dédié).
     * L'autorisation est vérifiée manuellement : seul le propriétaire du
     * projet peut ajouter des contributeurs (403 sinon), sans passer par
     * {@code @PreAuthorize} ni par l'évaluateur de permissions.
     *
     * <p>Règles visibles ici :
     * <ul>
     *   <li>projet introuvable -> exception {@code RuntimeException} ;</li>
     *   <li>utilisateur non propriétaire -> HTTP 403 ;</li>
     *   <li>{@code userId} absent du corps -> HTTP 400 ;</li>
     *   <li>utilisateur cible introuvable -> exception
     *       {@code RuntimeException} ;</li>
     *   <li>utilisateur déjà contributeur du projet -> HTTP 400 (unicité).</li>
     * </ul>
     *
     * <p>Effets collatéraux : création d'une notification pour l'utilisateur
     * ajouté ({@code CONTRIBUTOR_ADDED}), journalisation d'une activité, et
     * envoi d'un email de bienvenue (échec silencieux, voir
     * {@link #sendContributorAddedEmail}).
     *
     * <p>NOTE : l'opération n'est pas transactionnelle — la création du
     * contributeur, la notification et l'activité sont des opérations
     * distinctes.
     */
    @PostMapping("/{projectId}/contributors")
    public ResponseEntity<?> addContributor(
            @PathVariable Long projectId,
            @RequestBody Map<String, Long> body
    ) {
        Long currentUserId = getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // Autorisation manuelle : seul le propriétaire (owner) du projet est
        // accepté. Aucune règle @PreAuthorize ni permission de rôle ici.
        if (!project.getOwner().getUsersId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Seul le propriétaire peut ajouter des contributeurs"));
        }

        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId requis"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Contrôle d'unicité : un même utilisateur ne peut apparaître qu'une
        // fois dans les contributeurs d'un même projet.
        if (contributorRepository.existsByProjectProjectIdAndUserUsersId(projectId, userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet utilisateur est déjà contributeur"));
        }

        ProjectContributor contributor = new ProjectContributor();
        contributor.setProject(project);
        contributor.setUser(user);
        contributor.setAddedAt(LocalDateTime.now());

        ProjectContributor saved = contributorRepository.save(contributor);

        // Notification adressée à l'utilisateur ajouté, mentionnant le nom du
        // propriétaire et le titre du projet.
        String ownerName = project.getOwner().getFirstname() + " " + project.getOwner().getLastname();
        String notifMessage = ownerName + " vous a ajouté(e) comme contributeur au projet \"" + project.getTitle() + "\"";
        notificationService.createNotification(userId, notifMessage, NotificationType.CONTRIBUTOR_ADDED, projectId, null);

        // Journalisation de l'activité vue depuis le projet.
        String userName = user.getFirstname() + " " + user.getLastname();
        activityService.logActivity(projectId, currentUserId, ActivityType.CONTRIBUTOR_ADDED,
                userName + " a été ajouté(e) comme contributeur", null);

        // Envoi d'email "ajouté(e) comme contributeur" (échec silencieux).
        sendContributorAddedEmail(user, project, ownerName);

        return ResponseEntity.ok(toDto(saved));
    }

    /**
     * Retrait d'un contributeur d'un projet ({@code DELETE /projects/{projectId}/contributors/{userId}}).
     *
     * <p>Méthode transactionnelle ({@code @Transactional}) : la suppression
     * en base est rendue atomique.
     *
     * <p>Règles visibles ici :
     * <ul>
     *   <li>projet introuvable -> exception {@code RuntimeException} ;</li>
     *   <li>utilisateur non propriétaire -> HTTP 403 ;</li>
     *   <li>utilisateur NON contributeur -> HTTP 400 (conserve le message
     *       d'erreur "Cet utilisateur n'est pas contributeur").</li>
     * </ul>
     *
     * <p>Effets collatéraux : journalisation d'une activité
     * {@code CONTRIBUTOR_REMOVED} UNIQUEMENT si l'utilisateur retiré est
     * encore présent en base (sinon, la suppression passe silencieusement
     * sans activité). Aucune notification ni email n'est envoyé au retrait
     * (contrairement à l'ajout).
     */
    @DeleteMapping("/{projectId}/contributors/{userId}")
    @Transactional
    public ResponseEntity<?> removeContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId
    ) {
        Long currentUserId = getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // Autorisation manuelle identique à l'ajout : seul le propriétaire
        // peut retirer des contributeurs.
        if (!project.getOwner().getUsersId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Seul le propriétaire peut supprimer des contributeurs"));
        }

        if (!contributorRepository.existsByProjectProjectIdAndUserUsersId(projectId, userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet utilisateur n'est pas contributeur"));
        }
        contributorRepository.deleteByProjectProjectIdAndUserUsersId(projectId, userId);

        User removedUser = userRepository.findById(userId).orElse(null);
        // L'activité n'est journalisée que si l'utilisateur existe encore en
        // base ; sinon (null), le retrait reste silencieux.
        if (removedUser != null) {
            String removedName = removedUser.getFirstname() + " " + removedUser.getLastname();
            activityService.logActivity(projectId, currentUserId, ActivityType.CONTRIBUTOR_REMOVED,
                    removedName + " a été retiré(e) des contributeurs", null);
        }

        return ResponseEntity.ok(Map.of("message", "Contributeur retiré"));
    }

    /**
     * Envoi (best-effort) de l'email "ajouté comme contributeur" à
     * l'utilisateur concerné.
     *
     * <p>L'email est généré via le template et envoyé en HTML. Toute
     * exception (template, SMTP, destinataire invalide...) est absorbée : un
     * échec d'email ne doit jamais faire échouer la requête HTTP.
     */
    private void sendContributorAddedEmail(User user, Project project, String ownerName) {
        try {
            String html = templateEmailService.generateContributorAddedEmail(
                    user.getFirstname() + " " + user.getLastname(),
                    project.getTitle(),
                    ownerName
            );
            emailService.sendHtmlEmail(
                    user.getEmail(),
                    "Vous avez été ajouté(e) comme contributeur - Collab Task",
                    html
            );
        } catch (Exception e) {
            // Email failure should not block the request
        }
    }

    /**
     * Conversion d'une entité {@code ProjectContributor} en DTO de réponse.
     *
     * <p>Le nom complet est reconstruit à partir du prénom et du nom de
     * l'utilisateur ; chaque partie nulle est remplacée par une chaîne vide
     * puis l'ensemble est trimé.
     */
    private ProjectContributorResDto toDto(ProjectContributor entity) {
        String userName = (entity.getUser().getFirstname() != null ? entity.getUser().getFirstname() : "")
                + " " + (entity.getUser().getLastname() != null ? entity.getUser().getLastname() : "");
        return new ProjectContributorResDto(
                entity.getId(),
                entity.getProject().getProjectId(),
                entity.getUser().getUsersId(),
                userName.trim(),
                entity.getUser().getEmail(),
                entity.getAddedAt()
        );
    }
}
