package com.school.security.controllers.api;

import com.school.security.dtos.requests.MoveTaskReqDto;
import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.services.contracts.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Tâches", description = "Gestion des tâches : consultation, création, mise à jour, archivage logique, changement de statut, déplacement et tri.")
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
    @Operation(summary = "Lister toutes les tâches", description = "Retourne l'ensemble des tâches en base, sans filtrage par utilisateur ni par projet. Accessible à tout utilisateur authentifié.")
    @GetMapping
    public List<TaskResDto> findAllTasks() {
        return this.taskService.findAll();
    }

    /**
     * Récupération d'une tâche par identifiant ({@code GET /tasks/{id}}),
     * déléguée à {@code TaskService.findById(id)}.
     */
    @Operation(summary = "Récupérer une tâche par ID", description = "Récupère les informations d'une tâche à partir de son identifiant. Accessible à tout utilisateur authentifié.")
    @GetMapping("/{id}")
    public TaskResDto getTaskById(@PathVariable Long id) {
        return this.taskService.findById(id);
    }

    /**
     * Liste des tâches appartenant à un projet ({@code GET /tasks/project/{projectId}}),
     * déléguée à {@code TaskService.findByProject(projectId)}.
     */
    @Operation(summary = "Lister les tâches d'un projet", description = "Retourne toutes les tâches associées au projet spécifié. Accessible à tout utilisateur authentifié.")
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
    @Operation(summary = "Créer une nouvelle tâche", description = "Crée une tâche. Requiert la permission MANAGE_TASKS avec action INSERT. Applique les règles de validation et de structure définies dans le service.")
    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'INSERT')")
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
    @Operation(summary = "Mettre à jour une tâche", description = "Met à jour intégralement une tâche existante. Requiert la permission MANAGE_TASKS avec action UPDATE. Les contrôles de dates et de structure sont appliqués dans le service.")
    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'UPDATE')")
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
    @Operation(summary = "Archiver une tâche", description = "Effectue un archivage logique de la tâche (suppression logique). Requiert la permission MANAGE_TASKS avec action DELETE. Si le projet parent n'a plus de tâches actives, il peut être automatiquement désarchivé.")
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'DELETE')")
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
    @Operation(summary = "Changer le statut d'une tâche", description = "Change l'état d'une tâche via le paramètre statusId. Requiert la permission MANAGE_TASKS avec action UPDATE. Passer au statut 'Terminé' fige la date de fin.")
    @PatchMapping("/{taskId}/status")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'UPDATE')")
    public TaskResDto changeTaskStatus(@PathVariable Long taskId, @RequestParam Long statusId) {
        return this.taskService.changerStatut(taskId, statusId);
    }

    @Operation(summary = "Mettre à jour l'ordre de tri", description = "Met à jour le sortOrder d'une tâche. Requiert la permission MANAGE_TASKS avec action UPDATE.")
    @PatchMapping("/{taskId}/sort-order")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'UPDATE')")
    public TaskResDto updateTaskSortOrder(@PathVariable Long taskId, @RequestParam Integer sortOrder) {
        return this.taskService.updateTaskSortOrder(taskId, sortOrder);
    }

    /**
     * Déplacement (drag & drop) d'une tâche ({@code PATCH /tasks/{taskId}/move}).
     *
     * <p>Le corps contient le statut cible et, optionnellement, une carte de
     * référence ({@code beforeTaskId}) : si fournie, la tâche adopte sa priorité
     * et s'insère juste avant elle dans son groupe ; sinon elle garde sa priorité
     * et rejoint la fin de son groupe dans le nouveau statut. Le
     * {@code sortOrder} du groupe cible et de l'ancien groupe est renuméroté de
     * 1 à N par {@code TaskService.moveTask}.
     */
    @Operation(summary = "Déplacer une tâche", description = "Déplace une tâche vers un autre statut (drag & drop). Requiert la permission MANAGE_TASKS avec action UPDATE. La réorganisation des sortOrder est gérée dans le service.")
    @PatchMapping("/{taskId}/move")
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_TASKS', 'UPDATE')")
    public TaskResDto moveTask(@PathVariable Long taskId, @Valid @RequestBody MoveTaskReqDto req) {
        return this.taskService.moveTask(taskId, req.statusId(), req.beforeTaskId());
    }
}
