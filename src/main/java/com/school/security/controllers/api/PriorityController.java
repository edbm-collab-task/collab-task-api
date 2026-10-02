package com.school.security.controllers.api;

import com.school.security.dtos.responses.PriorityResDto;
import com.school.security.entities.Priority;
import com.school.security.repositories.PriorityRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur référentiel des priorités de tâches, sous le préfixe
 * {@code /priorities}.
 *
 * <p>Les priorités sont un référentiel indépendant des tâches (table
 * {@code priorities}, seedée dans {@code data.sql}). Ce contrôleur les expose
 * directement à partir de {@link PriorityRepository}, sur le même principe que
 * {@link StatusController} pour les statuts : accès direct au repository, sans
 * service métier intermédiaire.
 *
 * <p>Points d'attention (comportement actuel, documentés, non corrigés) :
 * <ul>
 *   <li>AUCUNE annotation {@code @PreAuthorize} : seule la règle globale
 *       {@code anyRequest().authenticated()} de {@code SecurityConfig}
 *       s'applique — tout utilisateur authentifié peut consulter le
 *       référentiel ;</li>
 *   <li>aucun endpoint d'écriture (POST/PUT/PATCH/DELETE) n'est exposé : les
 *       priorités ne sont gérées qu'au niveau du seed {@code data.sql}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/priorities")
public class PriorityController {

    @Autowired
    private PriorityRepository priorityRepository;

    /**
     * Liste toutes les priorités du référentiel ({@code GET /priorities}),
     * triées par {@code sortOrder} croissant (Urgente → Haute → Moyenne →
     * Basse). Les priorités éventuellement sans {@code sortOrder} sont
     * placées en fin de liste.
     */
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

    /**
     * Conversion d'une entité {@code Priority} en DTO de réponse.
     */
    private PriorityResDto toDto(Priority entity) {
        return new PriorityResDto(
                entity.getPriorityId(),
                entity.getName(),
                entity.getSortOrder()
        );
    }
}