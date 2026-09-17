package com.school.security.controllers.api;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.services.contracts.RoleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur de gestion des rôles et des permissions, sous le préfixe
 * {@code /roles}. Délègue toute la logique à {@link RoleService}.
 *
 * <p>AUCUNE annotation {@code @PreAuthorize} sur ce contrôleur : la
 * protection est entièrement portée par la filter-chain
 * ({@code SecurityConfig}) :
 * <ul>
 *   <li>{@code GET /roles} et {@code GET /roles/permissions} :
 *       {@code permitAll} (lecture publique sans authentification) ;</li>
 *   <li>{@code GET /roles/{id}} : non listé explicitement, tombe sous
 *       {@code anyRequest().authenticated()} (simple authentification) ;</li>
 *   <li>{@code POST /roles}, {@code PUT /roles/{id}} et
 *       {@code DELETE /roles/{id}} : autorité {@code SUPER_ADMIN}
 *       ({@code hasAuthority("SUPER_ADMIN")}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Liste tous les rôles ({@code GET /roles}).
     *
     * <p>Lecture publique au niveau de la filter-chain
     * ({@code permitAll}). Délègue à {@code RoleService.findAll()}.
     */
    @GetMapping
    public ResponseEntity<List<RoleResDto>> getAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    /**
     * Récupère un rôle par identifiant ({@code GET /roles/{id}}).
     *
     * <p>Non couvert par les {@code permitAll} de la filter-chain :
     * soumis au {@code anyRequest().authenticated()}. Délègue à
     * {@code RoleService.findById(id)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    /**
     * Création d'un rôle ({@code POST /roles}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN}
     * uniquement. Le service valide le nom comme valeur de l'enum
     * {@code RoleType} (nom inconnu → {@code IllegalArgumentException}),
     * l'unicité du nom et la résolution des permissions par nom (permission
     * inconnue → exception métier). Délègue à
     * {@code RoleService.create(dto)}.
     */
    @PostMapping
    public ResponseEntity<RoleResDto> create(@RequestBody RoleReqDto dto) {
        return ResponseEntity.ok(roleService.create(dto));
    }

    /**
     * Mise à jour d'un rôle ({@code PUT /roles/{id}}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN}
     * uniquement. Attention au comportement réel du service : seul le jeu de
     * permissions est remplacé, le nom du rôle n'est PAS modifié. Délègue à
     * {@code RoleService.update(id, dto)}.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleResDto> update(@PathVariable Long id, @RequestBody RoleReqDto dto) {
        return ResponseEntity.ok(roleService.update(id, dto));
    }

    /**
     * Suppression d'un rôle ({@code DELETE /roles/{id}}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN}
     * uniquement. Réponse {@code 204 No Content} si le rôle est supprimé.
     * Le service supprime le rôle sans vérifier s'il est encore attaché à
     * des utilisateurs. Délègue à {@code RoleService.delete(id)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Liste des permissions disponibles ({@code GET /roles/permissions}).
     *
     * <p>Lecture publique au niveau de la filter-chain
     * ({@code permitAll}). Délègue à
     * {@code RoleService.findAllPermissions()}.
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResDto>> getAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }
}
