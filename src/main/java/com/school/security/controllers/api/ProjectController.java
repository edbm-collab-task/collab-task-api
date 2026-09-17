package com.school.security.controllers.api;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.User;
import com.school.security.mappers.ProjectMapper;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de gestion des projets, sous le préfixe {@code /projects}.
 *
 * <p>Toutes les opérations sont déléguées à {@link ProjectService}
 * (séparation couche HTTP / logique métier). La quasi-totalité des endpoints
 * est protégée par la permission globale {@code MANAGE_PROJECTS} via
 * {@code @PreAuthorize} ; seul le transfert d'ownership repose sur une
 * permission scoped au projet ({@code MANAGE_PROJECT_CONTRIBUTORS}).
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>les endpoints de mise à jour, d'archivage et de désarchivage ne
 *       portent que la permission globale {@code MANAGE_PROJECTS} : aucun
 *       contrôle de permission lié au projet n'est appliqué à ce niveau ;</li>
 *   <li>la suppression ({@code DELETE}) est en réalité un archivage
 *       (suppression logique) réalisé par {@code ProjectService.deleteById} ;</li>
 *   <li>l'identité de l'utilisateur courant est résolue à partir du
 *       {@code SecurityContext} (l'email) puis convertie en identifiant
 *       utilisateur via une requête en base ;</li>
 *   <li>le champ {@code projectMapper} est injecté mais aucunement utilisé
 *       par ce contrôleur.</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper, UserRepository userRepository) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.userRepository = userRepository;
    }

    /**
     * Résout l'identifiant de l'utilisateur actuellement authentifié.
     *
     * <p>L'email est extrait du {@code SecurityContext} via
     * {@code SecurityUtils.getCurrentUsername()}, puis converti en
     * identifiant utilisateur (requête à {@code UserRepository}).
     * Un utilisateur inconnu en base déclenche une {@code RuntimeException}.
     */
    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    /**
     * Liste des projets actifs accessibles à l'utilisateur courant
     * ({@code GET /projects}).
     *
     * <p>Délègue à {@code ProjectService.findAllWithUser(currentUserId)} :
     * la visibilité (administrateur global vs propriétaire/contributeur)
     * est déterminée dans le service.
     */
    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findAllProjects() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUser(currentUserId);
    }

    /**
     * Liste des projets accessibles à l'utilisateur courant, Y COMPRIS ceux
     * archivés ({@code GET /projects/all}).
     *
     * <p>Appelle la même méthode de service que {@link #findAllProjects()}
     * dans sa variante avec les projets archivés
     * ({@code findAllWithUserIncludingArchived}).
     */
    @GetMapping("/all")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findAllProjectsIncludingArchived() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUserIncludingArchived(currentUserId);
    }

    /**
     * Récupère un projet par son identifiant ({@code GET /projects/{id}}).
     *
     * <p>{@code ProjectService.findByIdWithUser(id, currentUserId)} retourne
     * le projet tel que visible par l'utilisateur courant (contrôle
     * d'appartenance dans le service).
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto getProjectById(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findByIdWithUser(id, currentUserId);
    }

    /**
     * Création d'un projet ({@code POST /projects}).
     *
     * <p>Le DTO est validé par bean validation ({@code @Valid}). L'utilisateur
     * courant est enregistré comme propriétaire du projet
     * ({@code ProjectService.createWithOwner}) : le propriétaire est
     * automatiquement ajouté comme contributeur avec la permission
     * {@code MANAGE_PROJECT_CONTRIBUTORS} (voir service).
     */
    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto createProject(@Valid @RequestBody ProjectReqDto projectReqDto) {
        Long currentUserId = getCurrentUserId();
        return this.projectService.createWithOwner(projectReqDto, currentUserId);
    }

    /**
     * Mise à jour d'un projet existant ({@code PUT /projects/{id}}).
     *
     * <p>Délègue à {@code ProjectService.save(toSave, id)}. Les règles de
     * dates (validation, régression) sont appliquées dans le service.
     * Aucune permission liée au projet n'est vérifiée ici (permission globale
     * {@code MANAGE_PROJECTS} uniquement).
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto updateProject(
            @Valid @RequestBody ProjectReqDto toSave, @PathVariable Long id) {
        return this.projectService.save(toSave, id);
    }

    /**
     * Archivage d'un projet ({@code DELETE /projects/{id}}).
     *
     * <p>Malgré le verbe HTTP {@code DELETE}, l'opération correspond à un
     * ARCHIVAGE (suppression logique) réalisé par
     * {@code ProjectService.deleteById} : le projet est conservé en base et
     * n'apparaît plus dans les listes "actives".
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto archiveProject(@PathVariable Long id) {
        return this.projectService.deleteById(id);
    }

    /**
     * Désarchivage d'un projet ({@code PATCH /projects/{id}/unarchive}).
     *
     * <p>Réactive un projet précédemment archivé via {@code unarchiver(id)}.
     */
    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto unarchiveProject(@PathVariable Long id) {
        return this.projectService.unarchiver(id);
    }

    /**
     * Transfert de la propriété d'un projet à un autre utilisateur
     * ({@code POST /projects/{id}/transfer-ownership}).
     *
     * <p>UNIQUE exception de ce contrôleur : la protection repose sur la
     * permission scoped au projet {@code MANAGE_PROJECT_CONTRIBUTORS}
     * (et non sur la permission globale {@code MANAGE_PROJECTS}).
     * Le corps de la requête doit contenir {@code newOwnerId} (identifiant du
     * nouvel owner) ; s'il est absent, réponse 400.
     * L'exécution du transfert est déléguée à
     * {@code ProjectService.transferOwnership(id, currentUserId, newOwnerId)}.
     */
    @PostMapping("/{id}/transfer-ownership")
    @PreAuthorize("@permissionEvaluator.hasProjectPermission(#id, 'MANAGE_PROJECT_CONTRIBUTORS')")
    public ResponseEntity<?> transferOwnership(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        Long currentUserId = getCurrentUserId();
        Long newOwnerId = body.get("newOwnerId");
        if (newOwnerId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "newOwnerId is required"));
        }
        this.projectService.transferOwnership(id, currentUserId, newOwnerId);
        return ResponseEntity.ok(java.util.Map.of("message", "Ownership transferred successfully"));
    }

    /**
     * Liste des projets ARCHIVÉS uniquement, accessibles à l'utilisateur
     * courant ({@code GET /projects/archived}).
     *
     * <p>Réutilise la même méthode de service que {@code /projects/all}
     * ({@code findAllWithUserIncludingArchived}) puis filtre côté contrôleur
     * les projets actifs ({@code !p.isActive()}) : les projets archivés sont
     * donc récupérés puis écartés en mémoire.
     */
    @GetMapping("/archived")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findArchivedProjects() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUserIncludingArchived(currentUserId)
                .stream()
                .filter(p -> !p.isActive())
                .collect(java.util.stream.Collectors.toList());
    }
}
