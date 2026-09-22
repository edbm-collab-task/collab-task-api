package com.school.security.controllers.api;

import com.school.security.dtos.responses.AdminDashboardStatsResDto;
import com.school.security.enums.RoleType;
import com.school.security.entities.User;

import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.AdminDashboardReportService;
import com.school.security.services.contracts.AdminDashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Contrôleur du tableau de bord "administration", sous le préfixe
 * {@code /admin/dashboard}.
 *
 * <p>Délègue la construction des statistiques à {@link AdminDashboardService}
 * et l'export PDF à {@link AdminDashboardReportService}. Les périodes de
 * filtre sont identiques à celles du {@code DashboardController}
 * ({@code TODAY}, {@code LAST_7_DAYS}, {@code LAST_30_DAYS},
 * {@code LAST_3_MONTHS}, {@code THIS_YEAR}, {@code CUSTOM}).
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} et AUCUNE règle
 *       {@code hasAuthority} dans {@code SecurityConfig} ne protège les
 *       routes {@code /admin/dashboard/**} : le préfixe "admin" n'implique
 *       donc pas de contrôle SUPER_ADMIN au niveau HTTP, seule la règle
 *       globale {@code anyRequest().authenticated()} s'applique ;</li>
 *   <li>la limitation du périmètre selon le rôle est déléguée au service :
 *       un utilisateur SUPER_ADMIN voit tous les projets actifs, un autre
 *       profil ne voit que ses projets accessibles
 *       (findAccessibleProjectsByUserId) ;</li>
 *   <li>non-constance dans la gestion des paramètres invalides :
 *       {@code /stats} lève une {@code IllegalArgumentException}
 *       (aucune réponse 400 explicite), alors que {@code /reports/pdf}
 *       répond 400 ;</li>
 *   <li>la résolution de l'utilisateur courant lève une
 *       {@code RuntimeException} si l'email du SecurityContext est
 *       introuvable en base.</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminDashboardReportService adminDashboardReportService;
    private final UserRepository userRepository;

    public AdminDashboardController(AdminDashboardService adminDashboardService,
                                    AdminDashboardReportService adminDashboardReportService,
                                    UserRepository userRepository) {
        this.adminDashboardService = adminDashboardService;
        this.adminDashboardReportService = adminDashboardReportService;
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
     * Résout l'utilisateur authentifié courant. Même comportement que
     * {@link #getCurrentUserId()} mais avec l'entité complète.
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
    private String resolvePrimaryRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> RoleType.fromNameOrUser(role.getName()).name())
                .orElse("USER");
    }

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY", "LAST_7_DAYS", "LAST_30_DAYS",
            "LAST_3_MONTHS", "THIS_YEAR", "CUSTOM"
    );

    /**
     * Statistiques administratives ({@code GET /admin/dashboard/stats}).
     *
     * <p>Paramètres : {@code period} (obligatoire, une des valeurs de
     * {@code VALID_PERIODS}) et {@code startDate}/{@code endDate}
     * (optionnels, requis et contrôlés pour {@code CUSTOM}).
     *
     * <p>En cas de période ou de dates invalides, une
     * {@code IllegalArgumentException} est levée (aucune réponse 400
     * construite — contrairement à {@code /reports/pdf}).
     *
     * <p>Le périmètre des données est déterminé dans le service :
     * l'utilisateur SUPER_ADMIN concerne tous les projets actifs, les autres
     * profils uniquement leurs projets accessibles. Un utilisateur inexistant
     * en base donne des statistiques vides.
     */
    @GetMapping("/stats")
    public AdminDashboardStatsResDto getAdminDashboardStats(
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            throw new IllegalArgumentException("Période invalide");
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                throw new IllegalArgumentException("Dates requises pour période personnalisée");
            }
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) throw new IllegalArgumentException("Date début après date fin");
            } catch (Exception e) {
                throw new IllegalArgumentException("Format de date invalide");
            }
        }

        Long currentUserId = getCurrentUserId();
        return adminDashboardService.getAdminDashboardStats(currentUserId, period, startDate, endDate);
    }

    /**
     * Génération du rapport PDF administratif ({@code GET /admin/dashboard/reports/pdf}).
     *
     * <p>Même validation de période que {@code /stats}, mais ici les
     * paramètres invalides produisent une réponse 400 (sans corps).
     * L'utilisateur courant et son rôle principal sont résolus pour être
     * transmis au service de rapport (le rôle influe sur le contenu généré).
     *
     * <p>Le nom de fichier est fixe (format {@code rapport-admin-<date>.pdf},
     * date complète {@code LocalDate.now()}). La réponse porte le
     * Content-Type {@code application/pdf}, Content-Disposition {@code
     * attachment} et une Content-Length explicite.
     */
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
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
        String role = resolvePrimaryRole(user);
        byte[] pdfBytes = adminDashboardReportService.generateReport(
                currentUserId, role, period, startDate, endDate);

        String filename = "rapport-admin-" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}