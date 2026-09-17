package com.school.security.controllers.api;

import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur du tableau de bord (statistiques et rapport PDF), sous le
 * préfixe {@code /dashboard}.
 *
 * <p>Le contrôleur se charge de la validation des paramètres de période et
 * délègue la construction des données à {@link DashboardService}
 * (statistiques) et {@link DashboardReportService} (export PDF). Le
 * PÉRIMÈTRE des données est déterminé dans le service, pas ici : pour les
 * statistiques, l'utilisateur courant est transmis et le service restreint
 * les résultats au périmètre qui lui est réellement accessible
 * (SUPER_ADMIN -> tous les projets actifs ; ADMIN/USER -> projets accessibles ;
 * filtre éventuel par {@code projectId}).
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : seul
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}
 *       s'applique ;</li>
 *   <li>la validation de la période (constante {@code VALID_PERIODS}) et des
 *       dates est dupliquée entre {@code /stats} et {@code /reports/pdf} ;</li>
 *   <li>contrairement au PDF qui résout l'utilisateur via
 *       {@link #getCurrentUser()}, les statistiques passent uniquement
 *       l'identifiant utilisateur au service (qui gère un utilisateur
 *       inconnu en retournant un tableau de bord vide).</li>
 * </ul>
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardReportService dashboardReportService;
    private final UserRepository userRepository;

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY",
            "LAST_7_DAYS",
            "LAST_30_DAYS",
            "LAST_3_MONTHS",
            "THIS_YEAR",
            "CUSTOM");

    public DashboardController(
            DashboardService dashboardService,
            DashboardReportService dashboardReportService,
            UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.dashboardReportService = dashboardReportService;
        this.userRepository = userRepository;
    }

    /**
     * Statistiques du tableau de bord ({@code GET /dashboard/stats}).
     *
     * <p>Paramètres :
     * <ul>
     *   <li>{@code period} (obligatoire) : une des valeurs de
     *       {@code VALID_PERIODS} ({@code TODAY}, {@code LAST_7_DAYS},
     *       {@code LAST_30_DAYS}, {@code LAST_3_MONTHS}, {@code THIS_YEAR},
     *       {@code CUSTOM}) ;</li>
     *   <li>{@code startDate} / {@code endDate} (optionnels, requis pour
     *       {@code CUSTOM}) : dates ISO {@code LocalDate}, contrôlées
     *       (parse + cohérence start &lt;= end) ;</li>
     *   <li>{@code projectId} (optionnel) : restriction au projet donné.</li>
     * </ul>
     *
     * <p>Toute période/dates invalides entraînent une réponse 400 (aucun corps).
     * Les données sont ensuite calculées par
     * {@code DashboardService.getDashboardStats(userId, period, ...)} : le
     * service détermine le périmètre accessible à l'utilisateur et renvoie un
     * tableau de bord vide (200) si l'utilisateur n'existe pas, s'il n'a accès
     * à aucun projet, ou si le {@code projectId} demandé ne lui est pas
     * accessible.
     *
     * <p>Résultat : statistiques (tâches totales/terminées/en retard,
     * contributeurs, utilisateurs), évolution, distribution par statut,
     * activité récente (10 max) et projets récents (5 max) — le périmètre
     * "personnel" (tâches assignées à l'utilisateur) est appliqué par le
     * service pour les profils non admin.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardDataResDto> getDashboardStats(
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long projectId) {
        // Validation manuelle de la période (pas de bean validation).
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            return ResponseEntity.badRequest().build();
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            LocalDate start;
            LocalDate end;
            try {
                start = LocalDate.parse(startDate);
                end = LocalDate.parse(endDate);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        DashboardDataResDto result =
                dashboardService.getDashboardStats(currentUserId, period, startDate, endDate, projectId);
        return ResponseEntity.ok(result);
    }

    /**
     * Génération du rapport PDF du tableau de bord ({@code GET /dashboard/reports/pdf}).
     *
     * <p>Reprend la même validation de période/dates que {@code /stats}
     * (même bloc dupliqué). L'utilisateur courant est ensuite résolu
     * intégralement et son PREMIER rôle est déterminé
     * ({@link #resolvePrimaryRole}) : il est transmis au service de rapport
     * afin d'ajuster le contenu du PDF au profil (plateforme / administration /
     * activité personnelle).
     *
     * <p>Le nom de fichier est dérivé du rôle et du mois courant
     * ({@code yyyy-MM}, voir {@link #resolvePdfFilename}). La réponse porte le
     * Content-Type {@code application/pdf}, l'entête Content-Disposition
     * {@code attachment} et une Content-Length explicite.
     */
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long projectId) {
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            return ResponseEntity.badRequest().build();
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            LocalDate start;
            LocalDate end;
            try {
                start = LocalDate.parse(startDate);
                end = LocalDate.parse(endDate);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build();
            }
        }

        User user = getCurrentUser();
        RoleType role = resolvePrimaryRole(user);

        byte[] pdfBytes =
                dashboardReportService.generateReport(
                        user.getUsersId(), role, period, startDate, endDate, projectId);

        String filename = resolvePdfFilename(role, LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * Résout l'utilisateur courant à partir de l'email du
     * {@code SecurityContext} (requête à {@code UserRepository}). Lève une
     * {@code ResourceNotFoundException} si l'utilisateur n'existe pas en base.
     */
    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Raccourci retournant l'identifiant de l'utilisateur courant
     * (délégué à {@link #getCurrentUser()}).
     */
    private Long getCurrentUserId() {
        return getCurrentUser().getUsersId();
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

    /**
     * Construit le nom de fichier du rapport PDF selon le rôle et le mois
     * courant ({@code yyyy-MM}) :
     * <ul>
     *   <li>SUPER_ADMIN -> {@code rapport-plateforme-<mois>.pdf} ;</li>
     *   <li>ADMIN -> {@code rapport-administration-<mois>.pdf} ;</li>
     *   <li>USER -> {@code mon-rapport-activite-<mois>.pdf}.</li>
     * </ul>
     */
    private String resolvePdfFilename(RoleType role, LocalDate now) {
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return switch (role) {
            case SUPER_ADMIN -> "rapport-plateforme-" + datePart + ".pdf";
            case ADMIN -> "rapport-administration-" + datePart + ".pdf";
            case USER -> "mon-rapport-activite-" + datePart + ".pdf";
        };
    }
}
