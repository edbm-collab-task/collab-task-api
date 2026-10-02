package com.school.security.controllers.api;

import com.school.security.dtos.responses.NotificationResDto;
import com.school.security.entities.User;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de gestion des notifications de l'utilisateur courant, sous le
 * préfixe {@code /notifications}.
 *
 * <p>La logique métier est déléguée à {@link NotificationService}. L'identité
 * de l'utilisateur courant est résolue via le {@code SecurityContext} (email)
 * puis convertie en identifiant utilisateur via {@code UserRepository}.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : seules les règles globales
 *       de {@code SecurityConfig} s'appliquent (utilisateur authentifié
 *       requis) ;</li>
 *   <li>le marquage "lu" d'une notification ({@code PATCH /notifications/{id}/read})
 *       vérifie désormais que la notification appartient à l'utilisateur
 *       courant via {@code NotificationService.markAsRead(id, currentUserId)} ;</li>
 *   <li>les trois autres endpoints ({@code GET}, {@code /unread-count},
 *       {@code /read-all}) sont bien scopés à l'utilisateur courant.</li>
 * </ul>
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

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
     * Liste les notifications de l'utilisateur courant
     * ({@code GET /notifications}).
     *
     * <p>Délègue à {@code NotificationService.findByUserId(currentUserId)} :
     * les notifications sont retournées de la plus récente à la plus ancienne.
     */
    @GetMapping
    public List<NotificationResDto> getNotifications() {
        Long currentUserId = getCurrentUserId();
        return notificationService.findByUserId(currentUserId);
    }

    /**
     * Nombre de notifications NON LUIES de l'utilisateur courant
     * ({@code GET /notifications/unread-count}).
     *
     * <p>Retourne une map {@code {"count": <nombre>}} via
     * {@code NotificationService.countUnread(currentUserId)} (recherche scopée
     * à l'utilisateur).
     */
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        Long currentUserId = getCurrentUserId();
        return Map.of("count", notificationService.countUnread(currentUserId));
    }

    /**
     * Marque une notification comme lue ({@code PATCH /notifications/{id}/read}).
     *
     * <p>L'identifiant de l'utilisateur courant est résolu puis transmis à
     * {@code NotificationService.markAsRead(id, currentUserId)} : le service
     * vérifie que la notification appartient à l'utilisateur courant. Une
     * notification inexistante provoque une exception {@code RuntimeException}
     * ("Notification non trouvée") ; une notification appartenant à un autre
     * utilisateur provoque une {@code EntityException} (→ 404).
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResDto> markAsRead(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUserId));
    }

    /**
     * Marque TOUTES les notifications de l'utilisateur courant comme lues
     * ({@code PATCH /notifications/read-all}).
     *
     * <p>Scopé à l'utilisateur courant (identifiant résolu puis transmis à
     * {@code NotificationService.markAllAsRead}). Retourne un message de
     * confirmation.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long currentUserId = getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }
}
