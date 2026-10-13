package com.school.security.controllers.api;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleActionsResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.PermissionType;
import com.school.security.services.contracts.RoleService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
     * ({@code permitAll}). Chaque rôle expose ses permissions sous leur forme
     * complète (nom, description et catégorie), ce qui évite un second appel à
     * {@code GET /roles/permissions} pour les regrouper. Délègue à
     * {@code RoleService.findAll()}.
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
     * Ajout de permissions à un rôle existant ({@code POST /roles/{id}/permissions}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN}
     * uniquement. Le service associe les permissions spécifiées par leur nom
     * au rôle identifié par {@code id}. Délègue à
     * {@code RoleService.addPermissions(id, permissionNames)}.
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<Void> addPermissions(@PathVariable Long id, @RequestBody List<PermissionType> permissionNames) {
        roleService.addPermissions(id, permissionNames);
        return ResponseEntity.ok().build();
    }

    /**
     * Liste des permissions disponibles ({@code GET /roles/permissions}).
     *
     * <p>Lecture publique au niveau de la filter-chain
     * ({@code permitAll}). Chaque élément expose {@code id}, {@code name},
     * {@code description} et {@code categoryPermission} ; la liste est triée
     * par catégorie puis par nom afin que le regroupement par catégorie soit
     * stable côté client. Délègue à
     * {@code RoleService.findAllPermissions()}.
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResDto>> getAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }

    /**
     * Catalogue des actions gérables ({@code GET /roles/actions}).
     *
     * <p>Lecture publique au niveau de la filter-chain ({@code permitAll}).
     * Renvoie les noms techniques des actions (INSERT, UPDATE, DELETE,
     * GENERATE_REPORT, EXPORT) ; les libellés lisibles sont fournis par le
     * client. Délègue à {@code RoleService.findAllActions()}.
     */
    @GetMapping("/actions")
    public ResponseEntity<List<String>> getActions() {
        return ResponseEntity.ok(roleService.findAllActions().stream()
                .map(Enum::name)
                .collect(Collectors.toList()));
    }

    /**
     * Actions de chaque rôle ({@code GET /roles/actions-map}).
     *
     * <p>Lecture publique au niveau de la filter-chain ({@code permitAll}).
     * Renvoie une map indexée par identifiant de rôle, chaque valeur exposant
     * le nom du rôle et ses actions regroupées par module. Délègue à
     * {@code RoleService.getActionsMap()}.
     */
    @GetMapping("/actions-map")
    public ResponseEntity<Map<Long, RoleActionsResDto>> getActionsMap() {
        return ResponseEntity.ok(roleService.getActionsMap());
    }

    /**
     * Actions d'un rôle ({@code GET /roles/{id}/actions}).
     *
     * <p>Non listé dans les {@code permitAll} : soumis à
     * {@code anyRequest().authenticated()}. Délègue à
     * {@code RoleService.getRoleActions(id)}.
     */
    @GetMapping("/{id}/actions")
    public ResponseEntity<RoleActionsResDto> getRoleActions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleActions(id));
    }

    /**
     * Remplace les actions d'un rôle ({@code PUT /roles/{id}/actions}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN} uniquement.
     * Le corps associe chaque module (nom de permission) à sa liste d'actions ;
     * une liste vide conserve l'accès en lecture seule. Délègue à
     * {@code RoleService.setRoleActions(id, actions)}.
     */
    @PutMapping("/{id}/actions")
    public ResponseEntity<RoleActionsResDto> setRoleActions(
            @PathVariable Long id, @RequestBody Map<String, List<String>> actions) {
        return ResponseEntity.ok(roleService.setRoleActions(id, actions));
    }
}
