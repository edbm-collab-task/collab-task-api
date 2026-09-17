package com.school.security.controllers.api;

import com.school.security.dtos.responses.StatusResDto;
import com.school.security.dtos.requests.StatusReqDto;
import com.school.security.entities.Status;
import com.school.security.entities.Project;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur de gestion des statuts (états de tâches), sous le préfixe
 * {@code /statuses}.
 *
 * <p>Deux types de statuts coexistent côté entité {@code Status} :
 * <ul>
 *   <li>les statuts DÉFAUT, avec {@code project == null} : disponibles pour
 *       tous les projets (ex. le statut "Termine" utilisé par les tâches) ;</li>
 *   <li>les statuts SPÉCIFIQUES à un projet, avec {@code project != null}.</li>
 * </ul>
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>ce contrôleur accède directement aux repositories
 *       ({@code StatusRepository}, {@code ProjectRepository}) — aucun service
 *       métier intermédiaire ; injection par champs {@code @Autowired} ;</li>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : seule la règle globale
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}
 *       s'applique — tout utilisateur authentifié peut créer, lister ou
 *       supprimer des statuts ;</li>
 *   <li>aucun endpoint d'édition (PUT/PATCH) n'est exposé ;</li>
 *   <li>le corps de création ({@code StatusReqDto}) n'est pas soumis à la
 *       bean validation ({@code @Valid} absent).</li>
 * </ul>
 */
@RestController
@RequestMapping("/statuses")
public class StatusController {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Liste les statuts ({@code GET /statuses}).
     *
     * <p>Comportement selon le paramètre optionnel {@code projectId} :
     * <ul>
     *   <li>avec {@code projectId} : retourne les statuts du projet concerné
     *       AINSI QUE les statuts défaut ({@code project == null}), triés par
     *       {@code sortOrder} croissant ;</li>
     *   <li>sans {@code projectId} : retourne TOUS les statuts de la table
     *       (défaut et projets confondus), sans tri garanti.</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<List<StatusResDto>> getAll(
            @RequestParam(required = false) Long projectId) {
        List<Status> statuses;
        if (projectId != null) {
            statuses = statusRepository.findByProjectProjectIdOrProjectIsNullOrderBySortOrderAsc(projectId);
        } else {
            statuses = statusRepository.findAll();
        }
        List<StatusResDto> dtos = statuses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Création d'un statut ({@code POST /statuses}).
     *
     * <p>Le DTO reçoit {@code name}, {@code sortOrder} et optionnellement
     * {@code projectId} :
     * <ul>
     *   <li>si {@code projectId} est renseigné : le statut est créé pour ce
     *       projet (projet introuvable -> exception {@code RuntimeException}) ;</li>
     *   <li>si {@code projectId} est absent : un statut DÉFAUT (global,
     *       {@code project == null}) est créé.</li>
     * </ul>
     *
     * <p>Aucune validation du contenu n'est appliquée au niveau du contrôleur.
     */
    @PostMapping
    public ResponseEntity<StatusResDto> create(@RequestBody StatusReqDto dto) {
        Status status = new Status();
        status.setName(dto.name());
        status.setSortOrder(dto.sortOrder());
        if (dto.projectId() != null) {
            Project project = projectRepository.findById(dto.projectId())
                    .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
            status.setProject(project);
        }
        Status saved = statusRepository.save(status);
        return ResponseEntity.ok(toDto(saved));
    }

    /**
     * Suppression d'un statut ({@code DELETE /statuses/{id}}).
     *
     * <p>Un statut DÉFAUT ({@code project == null}) ne peut PAS être supprimé :
     * la requête est refusée (HTTP 400 "Impossible de supprimer un statut par
     * défaut"). Seul un statut spécifique à un projet peut être supprimé.
     *
     * <p>Point d'attention : aucun contrôle n'est effectué sur l'existence de
     * tâches utilisant encore ce statut avant la suppression.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statut non trouvé"));
        if (status.getProject() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Impossible de supprimer un statut par défaut"));
        }
        statusRepository.delete(status);
        return ResponseEntity.ok(Map.of("message", "Statut supprimé"));
    }

    /**
     * Conversion d'une entité {@code Status} en DTO de réponse
     * ({@code projectId} vaut {@code null} pour un statut défaut/global).
     */
    private StatusResDto toDto(Status entity) {
        return new StatusResDto(
                entity.getStatusId(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getProject() != null ? entity.getProject().getProjectId() : null
        );
    }
}
