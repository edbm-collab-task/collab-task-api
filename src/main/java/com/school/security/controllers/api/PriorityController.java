package com.school.security.controllers.api;

import com.school.security.dtos.responses.PriorityResDto;
import com.school.security.entities.Priority;
import com.school.security.repositories.PriorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Contrôleur des priorités de tâches, sous le préfixe {@code /priorities}.
 *
 * <p>Référentiel indépendant des tâches : renvoie TOUTES les priorités du
 * système (Urgente, Haute, Moyenne, Basse), triées par {@code sortOrder}
 * croissant, même lorsqu'aucune tâche ne les utilise. Le frontend s'appuie sur
 * cet endpoint pour alimenter le sélecteur de priorité des tâches
 * ({@code taskService.getPriorities()}).
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>accès direct au repository ({@code PriorityRepository}) — aucun service
 *       métier intermédiaire ; injection par champs {@code @Autowired} ;</li>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : seule la règle globale
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}
 *       s'applique ;</li>
 *   <li>aucun endpoint de création/modification/suppression n'est exposé
 *       (référentiel géré par le seed {@code data.sql}).</li>
 * </ul>
 */
@Tag(name = "Priorités", description = "Référentiel des priorités de tâches : lecture seule, triées par ordre de priorité.")
@RestController
@RequestMapping("/priorities")
public class PriorityController {

    @Autowired
    private PriorityRepository priorityRepository;

    /**
     * Liste toutes les priorités ({@code GET /priorities}), triées par
     * {@code sortOrder} croissant (Urgente → Haute → Moyenne → Basse). Les
     * priorités sans {@code sortOrder} (exceptionnelles) sont placées en fin
     * de liste.
     */
    @Operation(summary = "Lister toutes les priorités", description = "Retourne toutes les priorités du système, triées par sortOrder croissant. Référentiel en lecture seule. Accessible à tout utilisateur authentifié.")
    @GetMapping
    public ResponseEntity<List<PriorityResDto>> getAll() {
        List<PriorityResDto> dtos = priorityRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Priority::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private PriorityResDto toDto(Priority entity) {
        return new PriorityResDto(
                entity.getPriorityId(),
                entity.getName(),
                entity.getSortOrder()
        );
    }
}