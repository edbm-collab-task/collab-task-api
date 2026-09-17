package com.school.security.controllers.api;

import com.school.security.dtos.responses.ActivityResDto;
import com.school.security.services.contracts.ActivityService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'accès au journal d'activités d'un projet, sous le préfixe
 * {@code /projects/{projectId}/activities}.
 *
 * <p>Le journal retrace les événements du projet (ajout/retrait de
 * contributeur, modification de tâche, etc.) tels qu'enregistrés par
 * {@code ActivityService.logActivity} appelé depuis d'autres contrôleurs
 * ou services. La lecture est déléguée à {@link ActivityService}.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>exposition d'un UNIQUE endpoint en lecture ;</li>
 *   <li>AUCUNE annotation {@code @PreAuthorize} ni contrôle de projet dans le
 *       service : tout utilisateur authentifié peut consulter le journal de
 *       n'importe quel projet (la seule protection globale est
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}) ;</li>
 *   <li>si le projet n'existe pas, la liste retournée est simplement vide
 *       (aucune exception, aucun contrôle d'existence) ;</li>
 *   <li>aucune pagination ni limitation du nombre d'activités retournées.</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects/{projectId}/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Liste des activités d'un projet ({@code GET /projects/{projectId}/activities}).
     *
     * <p>Délègue à {@code ActivityService.findByProjectId(projectId)} : les
     * activités sont triées de la plus récente à la plus ancienne
     * ({@code createdAt} décroissant). Chaque entrée inclut le type
     * d'événement, la description, l'utilisateur à l'origine de l'action et
     * éventuellement l'identifiant de la tâche concernée.
     *
     * <p>Aucun contrôle d'appartenance au projet n'est appliqué (ni ici ni
     * dans le service).
     */
    @GetMapping
    public ResponseEntity<List<ActivityResDto>> getActivities(@PathVariable Long projectId) {
        return ResponseEntity.ok(activityService.findByProjectId(projectId));
    }
}
