package com.school.security.controllers.api;

import com.school.security.dtos.requests.DirectionReqDto;
import com.school.security.dtos.responses.DirectionResDto;
import com.school.security.services.contracts.DirectionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de gestion des directions (entités organisationnelles), sous le
 * préfixe {@code /directions}. Délègue toute la logique à
 * {@link DirectionService}.
 *
 * <p>Accès réel par endpoint :
 * <ul>
 *   <li>lecture ({@code GET /directions} et {@code GET /directions/{id}}) :
 *       AUCUNE annotation {@code @PreAuthorize} ; en cohérence avec la
 *       filter-chain ({@code SecurityConfig} les déclare {@code permitAll}),
 *       la lecture des directions est donc accessible SANS authentification ;</li>
 *   <li>écriture ({@code POST}, {@code PUT}, {@code DELETE}) : protégée par
 *       {@code @PreAuthorize} exigeant la permission métier
 *       {@code MANAGE_DIRECTIONS}. La filter-chain les laisse aussi
 *       transiter en {@code permitAll}, mais l'évaluateur de permission
 *       refuse les requêtes non authentifiées (il exige un
 *       {@code SecurityContext} authentifié) : seuls les utilisateurs
 *       connectés disposant de la permission {@code MANAGE_DIRECTIONS}
 *       obtiennent l'accès.</li>
 * </ul>
 *
 * <p>Note de maintenance : le mapping du PUT ({@code @PutMapping("{id}")})
 * et les noms de méthodes ne sont pas homogènes avec les autres contrôleurs
 * du backend (PUT nommé {@code registerDirection}, POST nommé
 * {@code save}).
 */
@RestController
@RequestMapping("/directions")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    /**
     * Liste toutes les directions ({@code GET /directions}).
     *
     * <p>Endpoint de LECTURE public : aucune annotation de sécurité,
     * accessible sans authentification (cf. {@code SecurityConfig}/
     * {@code permitAll}). Délègue à {@code DirectionService.findAll()}.
     */
    @GetMapping
    public List<DirectionResDto> findAllDirection() {
        return this.directionService.findAll();
    }

    /**
     * Récupère une direction par identifiant ({@code GET /directions/{id}}).
     *
     * <p>Endpoint de LECTURE public (aucune annotation de sécurité, accès sans
     * authentification). Délègue à {@code DirectionService.findById(id)}.
     */
    @GetMapping("/{id}")
    public DirectionResDto getByIdDirection(@PathVariable Long id) {
        return this.directionService.findById(id);
    }

    /**
     * Mise à jour d'une direction existante ({@code PUT /directions/{id}}).
     *
     * <p>Endpoint d'ÉCRITURE protégé par la permission métier
     * {@code MANAGE_DIRECTIONS} ({@code @PreAuthorize}). Délègue à
     * {@code DirectionService.save(toSave, id)}.
     */
    @PutMapping("{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto registerDirection(
            @RequestBody DirectionReqDto toSave, @PathVariable Long id) {
        return this.directionService.save(toSave, id);
    }

    /**
     * Suppression d'une direction ({@code DELETE /directions/{id}}).
     *
     * <p>Endpoint d'ÉCRITURE protégé par la permission métier
     * {@code MANAGE_DIRECTIONS} ({@code @PreAuthorize}). Délègue à
     * {@code DirectionService.deleteById(id)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto deleteDirection(@PathVariable Long id) {
        return this.directionService.deleteById(id);
    }

    /**
     * Création d'une direction ({@code POST /directions}).
     *
     * <p>Endpoint d'ÉCRITURE protégé par la permission métier
     * {@code MANAGE_DIRECTIONS} ({@code @PreAuthorize}). Délègue à
     * {@code DirectionService.createOrUpdate(directionReqDto)}.
     */
    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_DIRECTIONS')")
    public DirectionResDto save(@RequestBody DirectionReqDto directionReqDto) {
        return this.directionService.createOrUpdate(directionReqDto);
    }
}
