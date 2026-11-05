package com.school.security.controllers.api;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.ActionResDto;
import com.school.security.dtos.responses.PermissionActionsResDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleActionsResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.PermissionType;
import com.school.security.services.contracts.RoleService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Rôles et permissions", description = "Gestion des rôles, permissions et actions (RBAC). Aucune annotation @PreAuthorize : la protection est portée par la filter-chain — lectures en permitAll, routes d'écriture réservées à l'autorité SUPER_ADMIN.")
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
    @Operation(summary = "Lister tous les rôles", description = "Renvoie la liste des rôles avec, pour chacun, ses permissions complètes (nom, description, catégorie). Lecture publique en filter-chain (permitAll), sans authentification requise.")
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
    @Operation(summary = "Récupérer un rôle par son identifiant", description = "Renvoie le rôle correspondant à l'identifiant fourni. Non couvert par les permitAll : soumis à une simple authentification.")
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
    @Operation(summary = "Créer un rôle", description = "Crée un rôle à partir du DTO fourni. Réservé à l'autorité SUPER_ADMIN. Le service valide le nom du rôle (enum RoleType), son unicité et la résolution des permissions ; toute valeur inconnue déclenche une exception.")
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
    @Operation(summary = "Mettre à jour un rôle", description = "Remplace le jeu de permissions du rôle indiqué — le nom du rôle n'est pas modifié. Réservé à l'autorité SUPER_ADMIN.")
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
    @Operation(summary = "Supprimer un rôle", description = "Supprime définitivement le rôle indiqué et répond 204 No Content. Réservé à l'autorité SUPER_ADMIN ; le service ne vérifie pas que le rôle est encore attaché à des utilisateurs.")
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
    @Operation(summary = "Ajouter des permissions à un rôle", description = "Associe les permissions indiquées (par leur nom) au rôle identifié, sans supprimer les permissions existantes. Réservé à l'autorité SUPER_ADMIN.")
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
    @Operation(summary = "Lister les permissions disponibles", description = "Renvoie le catalogue des permissions (id, nom, description, catégorie), trié par catégorie puis par nom pour un regroupement stable côté client. Lecture publique (permitAll).")
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResDto>> getAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }

    /**
     * Catalogue des permissions avec le masque de leurs actions disponibles
     * ({@code GET /roles/permissions/actions}).
     *
     * <p>Lecture publique au niveau de la filter-chain ({@code permitAll}).
     * Chaque permission expose uniquement {@code totalActions} (masque des
     * actions disponibles sur le module) : le détail des actions
     * ({@code codeAction}, nom, libellé) n'est plus répété ici mais fourni par
     * {@code GET /roles/actions}. Délègue à
     * {@code RoleService.findAllPermissionsWithActions()}.
     */
    @Operation(summary = "Permissions avec leurs actions disponibles", description = "Renvoie chaque permission avec uniquement le masque totalActions de ses actions disponibles. Lecture publique (permitAll) ; le détail des actions est fourni par GET /roles/actions.")
    @GetMapping("/permissions/actions")
    public ResponseEntity<List<PermissionActionsResDto>> getAllPermissionsWithActions() {
        return ResponseEntity.ok(roleService.findAllPermissionsWithActions());
    }

    /**
     * Catalogue des actions gérables ({@code GET /roles/actions}).
     *
     * <p>Lecture publique au niveau de la filter-chain ({@code permitAll}).
     * Renvoie chaque action avec son code binaire et son libellé
     * ({@code [{codeAction, name, label}]}). Délègue à
     * {@code RoleService.findAllActions()}.
     */
    @Operation(summary = "Lister les actions gérables", description = "Renvoie le catalogue des actions avec leur code binaire et leur libellé. Lecture publique (permitAll).")
    @GetMapping("/actions")
    public ResponseEntity<List<ActionResDto>> getActions() {
        return ResponseEntity.ok(roleService.findAllActions());
    }

    /**
     * Actions de chaque rôle ({@code GET /roles/actions-map}).
     *
     * <p>Lecture publique au niveau de la filter-chain ({@code permitAll}).
     * Renvoie une map indexée par identifiant de rôle ; chaque valeur expose le
     * nom du rôle et, par module, les deux masques
     * ({@code totalActions}, {@code totalActionsAccordees}) — sans le détail des
     * actions (fourni par {@code GET /roles/actions}). Délègue à
     * {@code RoleService.getActionsMap()}.
     */
    @Operation(summary = "Carte des actions par rôle", description = "Renvoie, pour chaque rôle, les deux masques d'actions par module (totalActions et totalActionsAccordees). Lecture publique (permitAll).")
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
    @Operation(summary = "Actions d'un rôle", description = "Renvoie les actions accordées au rôle indiqué, module par module. Soumis à une simple authentification (non couvert par les permitAll).")
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
    @Operation(summary = "Remplacer les actions d'un rôle", description = "Définit pour chaque module la liste des actions accordées au rôle ; une liste vide conserve l'accès en lecture seule. Réservé à l'autorité SUPER_ADMIN.")
    @PutMapping("/{id}/actions")
    public ResponseEntity<RoleActionsResDto> setRoleActions(
            @PathVariable Long id, @RequestBody Map<String, List<String>> actions) {
        return ResponseEntity.ok(roleService.setRoleActions(id, actions));
    }

    /**
     * Redéfinit les actions disponibles d'un module
     * ({@code PUT /roles/permissions/{id}/actions}).
     *
     * <p>Protégé par la filter-chain : autorité {@code SUPER_ADMIN} uniquement.
     * Le corps est la liste des noms techniques d'actions à conserver sur le
     * module. Le {@code totalActions} du module est recalculé et les actions
     * retirées sont retirées des masques accordés des rôles. Délègue à
     * {@code RoleService.updatePermissionActions(id, actionNames)}.
     */
    @Operation(summary = "Redéfinir les actions d'un module", description = "Remplace la liste des actions disponibles d'une permission : totalActions est recalculé et les actions retirées sont aussi retirées des masques accordés des rôles. Réservé à l'autorité SUPER_ADMIN.")
    @PutMapping("/permissions/{id}/actions")
    public ResponseEntity<PermissionActionsResDto> updatePermissionActions(
            @PathVariable Long id, @RequestBody List<String> actionNames) {
        return ResponseEntity.ok(roleService.updatePermissionActions(id, actionNames));
    }
}
