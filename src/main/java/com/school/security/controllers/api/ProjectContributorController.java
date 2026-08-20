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

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    @GetMapping("/{projectId}/contributors")
    public ResponseEntity<List<ProjectContributorResDto>> getContributors(@PathVariable Long projectId) {
        List<ProjectContributor> contributors = contributorRepository.findByProjectProjectIdOrderByAddedAtDesc(projectId);
        List<ProjectContributorResDto> dtos = contributors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{projectId}/contributors")
    public ResponseEntity<?> addContributor(
            @PathVariable Long projectId,
            @RequestBody Map<String, Long> body
    ) {
        Long currentUserId = getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        if (!project.getOwner().getUsersId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Seul le propriétaire peut ajouter des contributeurs"));
        }

        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId requis"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (contributorRepository.existsByProjectProjectIdAndUserUsersId(projectId, userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet utilisateur est déjà contributeur"));
        }

        ProjectContributor contributor = new ProjectContributor();
        contributor.setProject(project);
        contributor.setUser(user);
        contributor.setAddedAt(LocalDateTime.now());

        ProjectContributor saved = contributorRepository.save(contributor);

        String ownerName = project.getOwner().getFirstname() + " " + project.getOwner().getLastname();
        String notifMessage = ownerName + " vous a ajouté(e) comme contributeur au projet \"" + project.getTitle() + "\"";
        notificationService.createNotification(userId, notifMessage, NotificationType.CONTRIBUTOR_ADDED, projectId, null);

        String userName = user.getFirstname() + " " + user.getLastname();
        activityService.logActivity(projectId, currentUserId, ActivityType.CONTRIBUTOR_ADDED,
                userName + " a été ajouté(e) comme contributeur", null);

        sendContributorAddedEmail(user, project, ownerName);

        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{projectId}/contributors/{userId}")
    @Transactional
    public ResponseEntity<?> removeContributor(
            @PathVariable Long projectId,
            @PathVariable Long userId
    ) {
        Long currentUserId = getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        if (!project.getOwner().getUsersId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Seul le propriétaire peut supprimer des contributeurs"));
        }

        if (!contributorRepository.existsByProjectProjectIdAndUserUsersId(projectId, userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet utilisateur n'est pas contributeur"));
        }
        contributorRepository.deleteByProjectProjectIdAndUserUsersId(projectId, userId);

        User removedUser = userRepository.findById(userId).orElse(null);
        if (removedUser != null) {
            String removedName = removedUser.getFirstname() + " " + removedUser.getLastname();
            activityService.logActivity(projectId, currentUserId, ActivityType.CONTRIBUTOR_REMOVED,
                    removedName + " a été retiré(e) des contributeurs", null);
        }

        return ResponseEntity.ok(Map.of("message", "Contributeur retiré"));
    }

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
