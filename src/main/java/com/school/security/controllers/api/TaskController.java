package com.school.security.controllers.api;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.services.contracts.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de gestion des tâches, sous le préfixe {@code /tasks}.
 *
 * <p>Délègue l'intégralité du traitement à {@link TaskService}. Les règles
 * métier (validation des dates, règles parent/enfant, archivage/désarchivage
 * automatique du projet, remplacement des assignés, notification de priorité)
 * sont appliquées dans le service, pas ici.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} n'est présente sur ce
 *       contrôleur : les endpoints reposent uniquement sur la règle globale
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}.
 *       Aucune permission liée au rôle ou au projet n'est vérifiée à ce
 *       niveau ;</li>
 *   <li>aucun filtrage par utilisateur : {@code GET /tasks} et
 *       {@code GET /tasks/{id}} ne restreignent pas la visibilité des tâches
 *       à l'utilisateur courant ;</li>
 *   <li>la suppression ({@code DELETE}) est en réalité un archivage
 *       (suppression logique) effectué par {@code TaskService.deleteById}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Liste de toutes les tâches ({@code GET /tasks}).
     *
     * <p>Délègue à {@code TaskService.findAll()} : retourne l'ensemble des
     * tâches en base, sans filtrage par utilisateur ni par projet.
     */
    @GetMapping
    public List<TaskResDto> findAllTasks() {
        return this.taskService.findAll();
    }

    /**
     * Récupération d'une tâche par identifiant ({@code GET /tasks/{id}}),
     * déléguée à {@code TaskService.findById(id)}.
     */
    @GetMapping("/{id}")
    public TaskResDto getTaskById(@PathVariable Long id) {
        return this.taskService.findById(id);
    }

    /**
     * Liste des tâches appartenant à un projet ({@code GET /tasks/project/{projectId}}),
     * déléguée à {@code TaskService.findByProject(projectId)}.
     */
    @GetMapping("/project/{projectId}")
    public List<TaskResDto> findTasksByProject(@PathVariable Long projectId) {
        return this.taskService.findByProject(projectId);
    }

    /**
     * Création d'une tâche ({@code POST /tasks}).
     *
     * <p>Le DTO est validé par bean validation ({@code @Valid}) puis transmis à
     * {@code TaskService.createOrUpdate(taskReqDto)}. Les règles parent/enfant,
     * la validation des dates et l'assignation sont gérées dans le service.
     */
    @PostMapping
    public TaskResDto createTask(@Valid @RequestBody TaskReqDto taskReqDto) {
        return this.taskService.createOrUpdate(taskReqDto);
    }

    /**
     * Mise à jour d'une tâche existante ({@code PUT /tasks/{id}}).
     *
     * <p>Délègue à {@code TaskService.save(toSave, id)} : les contrôles de
     * dates (régression interdite) et de structure parent/enfant sont appliqués
     * dans le service.
     */
    @PutMapping("/{id}")
    public TaskResDto updateTask(@Valid @RequestBody TaskReqDto toSave, @PathVariable Long id) {
        return this.taskService.save(toSave, id);
    }

    /**
     * Archivage d'une tâche ({@code DELETE /tasks/{id}}).
     *
     * <p>Malgré le verbe HTTP {@code DELETE}, l'opération correspond à un
     * ARCHIVAGE (suppression logique) via {@code TaskService.deleteById}.
     * Conséquence connue (voir service) : si le projet parent ne possède plus
     * aucune tâche active, il est automatiquement désarchivé.
     */
    @DeleteMapping("/{id}")
    public TaskResDto archiveTask(@PathVariable Long id) {
        return this.taskService.deleteById(id);
    }

    /**
     * Changement d'état (statut) d'une tâche ({@code PATCH /tasks/{taskId}/status}).
     *
     * <p>Le nouveau statut est passé en query parameter {@code statusId}
     * (et non dans le corps de la requête). L'application au statut
     * {@code "Termine"} fige la date de terminaison ({@code completedAt}) ;
     * voir {@code TaskService.changerStatut}.
     */
    @PatchMapping("/{taskId}/status")
    public TaskResDto changeTaskStatus(
            @PathVariable Long taskId, @RequestParam Long statusId) {
        return this.taskService.changerStatut(taskId, statusId);
    }
}
