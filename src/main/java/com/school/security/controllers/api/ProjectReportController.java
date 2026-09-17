package com.school.security.controllers.api;

import com.school.security.dtos.responses.ProjectReportResDto;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.ProjectReportReportService;
import com.school.security.services.contracts.ProjectReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Contrôleur de rapport d'un projet, sous le préfixe
 * {@code /projects/{projectId}/report}.
 *
 * <p>AUCUNE annotation {@code @PreAuthorize} ; la filter-chain
 * ({@code SecurityConfig}) déclare les deux routes ({@code /report} et
 * {@code /report/pdf}) {@code permitAll}. En pratique l'accès réel est porté
 * par le service (voir {@code ProjectReportServiceImpl}) :
 * <ul>
 *   <li>l'utilisateur courant est résolu depuis le {@code SecurityContext}
 *       (les helpers lèvent une {@code RuntimeException} si l'email est
 *       introuvable, ce qui rejette de fait les appels sans session
 *       authentifiée) ;</li>
 *   <li>le périmètre est vérifié dans le service : le rapport n'est retourné
 *       (statistiques) ou généré (PDF) que pour un SUPER_ADMIN, un ADMIN,
 *       le propriétaire du projet ou un contributeur du projet ; sinon le
 *       service renvoie {@code null} (corps vide, HTTP 200) pour les
 *       statistiques, et la génération PDF lève une exception métier
 *       ({@code "Projet non accessible"}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects/{projectId}/report")
public class ProjectReportController {

    private final ProjectReportService projectReportService;
    private final ProjectReportReportService projectReportReportService;
    private final UserRepository userRepository;

    public ProjectReportController(ProjectReportService projectReportService,
                                   ProjectReportReportService projectReportReportService,
                                   UserRepository userRepository) {
        this.projectReportService = projectReportService;
        this.projectReportReportService = projectReportReportService;
        this.userRepository = userRepository;
    }

    /**
     * Résout l'identifiant de l'utilisateur authentifié courant (email du
     * {@code SecurityContext} converti via {@code UserRepository}). Lève une
     * {@code RuntimeException} si l'utilisateur n'existe pas en base.
     */
    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    /**
     * Résout l'utilisateur authentifié courant (entité complète). Même
     * comportement que {@link #getCurrentUserId()}.
     */
    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Détermine le rôle "principal" de l'utilisateur : son PREMIER rôle dans
     * la collection (ordre de la base). En l'absence de rôle, le fallback est
     * {@code RoleType.USER}.
     */
    private RoleType resolvePrimaryRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> role.getName())
                .orElse(RoleType.USER);
    }

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY", "LAST_7_DAYS", "LAST_30_DAYS",
            "LAST_3_MONTHS", "THIS_YEAR", "CUSTOM"
    );

    /**
     * Statistiques du rapport d'un projet ({@code GET
     * /projects/{projectId}/report}).
     *
     * <p>Le contrôle d'accès est entièrement délégué à
     * {@code ProjectReportService.getProjectReport(userId, projectId)} :
     * SUPER_ADMIN, ADMIN, propriétaire ou contributeur du projet → rapport
     * complet ; sinon (ou si l'utilisateur ou le projet n'existe pas) le
     * service renvoie {@code null}, donc réponse HTTP 200 au corps vide.
     */
    @GetMapping
    public ProjectReportResDto getProjectReport(@PathVariable Long projectId) {
        Long currentUserId = getCurrentUserId();
        return projectReportService.getProjectReport(currentUserId, projectId);
    }

    /**
     * Génération du rapport PDF d'un projet ({@code GET
     * /projects/{projectId}/report/pdf}).
     *
     * <p>Validation de la période identique aux autres contrôleurs de
     * rapport : periode invalide ou plage {@code CUSTOM} mal formée →
     * réponse 400 (vide). L'utilisateur courant et son rôle principal sont
     * transmis au service de génération, qui réutilise
     * {@code ProjectReportService.getProjectReport} pour le contrôle d'accès
     * et les données : projet non accessible → exception métier
     * ({@code "Projet non accessible"}).
     *
     * <p>Le nom de fichier est {@code rapport-projet-<projectId>-<date>.pdf} ;
     * la réponse porte {@code application/pdf}, Content-Disposition
     * {@code attachment} et Content-Length explicite.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @PathVariable Long projectId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            return ResponseEntity.badRequest().build();
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) {
                    return ResponseEntity.badRequest().build();
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        User user = getCurrentUser();
        RoleType role = resolvePrimaryRole(user);
        byte[] pdfBytes = projectReportReportService.generateReport(
                currentUserId, projectId, role, period, startDate, endDate);

        String filename = "rapport-projet-" + projectId + "-" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}