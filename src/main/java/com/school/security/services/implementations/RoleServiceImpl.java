package com.school.security.services.implementations;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.ActionResDto;
import com.school.security.dtos.responses.PermissionActionsResDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleActionsResDto;
import com.school.security.dtos.responses.RolePermissionActionsResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.ActionType;
import com.school.security.enums.PermissionType;
import com.school.security.entities.Action;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import com.school.security.entities.RolePermission;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.repositories.ActionRepository;
import com.school.security.repositories.PermissionRepository;
import com.school.security.repositories.RolePermissionRepository;
import com.school.security.repositories.RoleRepository;
import com.school.security.services.contracts.RoleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des rôles et des permissions.
 *
 * <p>Les actions sont représentées par un <strong>masque binaire</strong> :
 * chaque {@link ActionType} porte un code (puissance de 2) et un ensemble
 * d'actions se réduit à la somme de leurs codes.
 * <ul>
 *   <li>le masque des actions <em>disponibles</em> d'un module est stocké dans
 *       {@code permissions.total_actions} (référentiel éditable) ;</li>
 *   <li>le masque des actions <em>accordées</em> à un rôle sur un module est
 *       stocké dans {@code roles_permissions.actions}.</li>
 * </ul>
 *
 * <p>Règles métier (documentées) :
 * <ul>
 *   <li>la création d'un rôle ({@code create}) est refusée si le rôle existe déjà ;</li>
 *   <li>la modification d'un rôle ({@code update}) ne remplace que les
 *       permissions fournies ; les actions déjà configurées sont préservées ;</li>
 *   <li>la suppression d'un rôle est refusée s'il est affecté à des utilisateurs ;</li>
 *   <li>les permissions sont résolues par leur nom ({@code PermissionType}) ;
 *       une {@code EntityException} est levée si introuvable.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private RoleMapper roleMapper;
    private ActionRepository actionRepository;
    private RolePermissionRepository rolePermissionRepository;

    /** Retourne l'ensemble des rôles avec leurs permissions associées. */
    @Override
    public List<RoleResDto> findAll() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Retourne le rôle par son identifiant.
     *
     * <p>Lève une {@code EntityException} si le rôle n'existe pas.</p>
     */
    @Override
    public RoleResDto findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));
        return roleMapper.toDto(role);
    }

/** Crée un nouveau rôle.
     *
     * <p>Le nom est un libellé libre (normalisé en majuscules) associé à un
     * {@code codeRole} distinct. Vérifie que le rôle n'existe pas déjà ; lève
     * une {@code BadRequestException} si le nom est vide et une
     * {@code EntityException} en cas de doublon. Les permissions optionnelles
     * sont résolues depuis la base de données via
     * {@code PermissionRepository}.</p>
     */
    @Override
    public RoleResDto create(RoleReqDto dto) {
        String name = normalizeName(dto.name());
        if (roleRepository.findByName(name).isPresent()) {
            throw new EntityException("Role already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setCodeRole(dto.codeRole());

        if (dto.permissions() != null) {
            List<Permission> permissions = dto.permissions().stream()
                    .map(p -> permissionRepository.findByName(p)
                            .orElseThrow(() -> new EntityException("Permission not found: " + p)))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        }

        return roleMapper.toDto(roleRepository.save(role));
    }

    /** Modifie un rôle existant : remplace le {@code codeRole} fourni et/ou les
     * permissions fournies (les actions déjà configurées sont préservées).
     *
     * <p>Lève une {@code EntityException} si le rôle n'existe pas.</p>
     */
    @Override
    public RoleResDto update(Long id, RoleReqDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));

        if (dto.codeRole() != null && !dto.codeRole().isBlank()) {
            role.setCodeRole(dto.codeRole());
        }

        if (dto.permissions() != null) {
            List<Permission> permissions = dto.permissions().stream()
                    .map(p -> permissionRepository.findByName(p)
                            .orElseThrow(() -> new EntityException("Permission not found: " + p)))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        }

        return roleMapper.toDto(roleRepository.save(role));
    }

    /** Supprime un rôle existant.
     *
     * <p>Lève une {@code EntityException} si le rôle n'existe pas ou s'il est
     * encore affecté à des utilisateurs.</p>
     */
    @Override
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));
        if (!role.getUsers().isEmpty()) {
            throw new EntityException("Impossible de supprimer un rôle qui est affecté à des utilisateurs.");
        }
        roleRepository.delete(role);
    }

    /**
     * Retourne la liste de toutes les permissions du système.
     *
     * <p>Les permissions sont triées par catégorie puis par nom afin que le
     * regroupement affiché par le client soit déterministe.
     */
    @Override
    public List<PermissionResDto> findAllPermissions() {
        return permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc().stream()
                .map(p -> new PermissionResDto(p.getPermissionId(), p.getName().name(), p.getDescription(), p.getCategoryPermission()))
                .collect(Collectors.toList());
    }

    /**
     * Catalogue : chaque permission accompagnée des actions disponibles sur ce
     * module (objets {@code codeAction/name/label}) et du masque
     * {@code totalActions} (somme des codes).
     *
     * <p>Sert au frontend à construire les cases à cocher d'actions. Les
     * permissions sont triées par catégorie puis par nom.
     */
    @Override
    public List<PermissionActionsResDto> findAllPermissionsWithActions() {
        return permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc().stream()
                .map(p -> new PermissionActionsResDto(
                        p.getPermissionId(),
                        p.getName().name(),
                        p.getDescription(),
                        p.getCategoryPermission(),
                        toActionDtos(p.getActions()),
                        p.getTotalActions()))
                .collect(Collectors.toList());
    }

/** Ajoute des permissions à un rôle existant.
     *
     * @param roleId Identifiant du rôle
     * @param permissionNames Noms des permissions à associer (enum PermissionType)
     * @throws EntityException si le rôle ou une permission n'existe pas
     */
    @Transactional
    public void addPermissions(Long roleId, List<PermissionType> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityException("Role not found: " + roleId));
        List<Permission> permissions = permissionNames.stream()
                .map(p -> permissionRepository.findByName(p)
                        .orElseThrow(() -> new EntityException("Permission not found: " + p)))
                .collect(Collectors.toList());
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

    /**
     * Catalogue des actions gérables, triées par code binaire et exposées
     * avec leur code et leur libellé ({@code [{codeAction, name, label}]}).
     */
    @Override
    public List<ActionResDto> findAllActions() {
        return toActionDtos(actionRepository.findAllByOrderByCodeActionAsc());
    }

    /**
     * Actions de chaque rôle, indexées par identifiant de rôle, triées par
     * identifiant pour un rendu déterministe.
     */
    @Override
    public Map<Long, RoleActionsResDto> getActionsMap() {
        Map<Long, RoleActionsResDto> result = new LinkedHashMap<>();
        roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getRolesId))
                .forEach(role -> result.put(role.getRolesId(), toRoleActions(role)));
        return result;
    }

    /** Actions d'un rôle, regroupées par module, avec les deux totaux. */
    @Override
    public RoleActionsResDto getRoleActions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityException("Role not found with ID " + roleId));
        return toRoleActions(role);
    }

    /**
     * Remplace la configuration d'actions d'un rôle.
     *
     * <p>Le corps associe chaque module à sa liste d'actions (noms techniques) ;
     * une liste vide conserve l'accès en lecture seule (masque 0). Les modules
     * absents sont retirés du rôle. Les lignes existantes sont réutilisées afin
     * d'éviter toute collision de clé composite lors du flush.
     *
     * <p>Seules les actions disponibles sur le module ({@code permissions_actions})
     * sont acceptées : une action étrangère au module lève une
     * {@code BadRequestException}.
     */
    @Override
    public RoleActionsResDto setRoleActions(Long roleId, Map<String, List<String>> actions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityException("Role not found: " + roleId));

        Map<Long, RolePermission> existing = role.getRolePermissions().stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getPermissionId(),
                        rp -> rp,
                        (a, b) -> a));

        List<RolePermission> rebuilt = new ArrayList<>();
        if (actions != null) {
            for (Map.Entry<String, List<String>> entry : actions.entrySet()) {
                Permission permission = permissionRepository.findByName(parsePermission(entry.getKey()))
                        .orElseThrow(() -> new EntityException("Permission not found: " + entry.getKey()));

                Set<String> available = actionNames(permission);
                int mask = 0;
                for (String actionName : entry.getValue()) {
                    ActionType action = parseAction(actionName);
                    if (!available.contains(action.name())) {
                        throw new BadRequestException(
                                "Action non disponible pour le module " + entry.getKey() + " : " + actionName);
                    }
                    mask |= action.getCode();
                }

                RolePermission rp = existing.get(permission.getPermissionId());
                if (rp == null) {
                    rp = new RolePermission();
                    rp.setRole(role);
                    rp.setPermission(permission);
                }
                rp.setActions(mask);
                rebuilt.add(rp);
            }
        }

        role.getRolePermissions().clear();
        role.getRolePermissions().addAll(rebuilt);
        roleRepository.save(role);
        return toRoleActions(role);
    }

    /**
     * Redéfinit les actions disponibles d'un module.
     *
     * <p>La liste fournie (noms techniques) devient la référence. Le
     * {@code totalActions} du module est recalculé, puis, pour tous les rôles
     * possédant ce module, les bits des actions désormais indisponibles sont
     * retirés de leur masque accordé (cohérence : on ne peut pas conserver une
     * action qui n'existe plus).
     */
    @Override
    public PermissionActionsResDto updatePermissionActions(Long permissionId, List<String> actionNames) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityException("Permission not found: " + permissionId));

        Set<Action> resolved = new LinkedHashSet<>();
        if (actionNames != null) {
            for (String name : actionNames) {
                ActionType type = parseAction(name);
                Action action = actionRepository.findByName(type.name())
                        .orElseThrow(() -> new EntityException("Action not found: " + name));
                resolved.add(action);
            }
        }

        permission.setActions(resolved);
        int total = maskOf(resolved);
        permission.setTotalActions(total);
        permissionRepository.save(permission);

        // Retire des masques accordés les bits devenus indisponibles.
        List<RolePermission> rows = rolePermissionRepository.findByPermissionPermissionId(permissionId);
        for (RolePermission rp : rows) {
            int current = rp.getActions() == null ? 0 : rp.getActions();
            int clamped = current & total;
            if (clamped != current) {
                rp.setActions(clamped);
            }
        }
        rolePermissionRepository.saveAll(rows);

        return new PermissionActionsResDto(
                permission.getPermissionId(),
                permission.getName().name(),
                permission.getDescription(),
                permission.getCategoryPermission(),
                toActionDtos(resolved),
                total);
    }

    /** Regroupe les actions d'un rôle par module, modules triés par nom. */
    private RoleActionsResDto toRoleActions(Role role) {
        Map<String, RolePermissionActionsResDto> permissions = new java.util.TreeMap<>();
        for (RolePermission rp : role.getRolePermissions()) {
            Permission permission = rp.getPermission();
            permissions.put(permission.getName().name(), new RolePermissionActionsResDto(
                    permission.getTotalActions(),
                    rp.getActions() == null ? 0 : rp.getActions(),
                    toActionDtos(permission.getActions())));
        }
        return new RoleActionsResDto(role.getName(), permissions);
    }

    /** Convertit les actions en DTO (triées par code binaire). */
    private List<ActionResDto> toActionDtos(Collection<Action> actions) {
        return actions.stream()
                .sorted(Comparator.comparing(Action::getCodeAction))
                .map(a -> new ActionResDto(a.getCodeAction(), a.getName(), a.getLabel()))
                .collect(Collectors.toList());
    }

    /** Masque (somme des codes) d'un ensemble d'actions. */
    private int maskOf(Collection<Action> actions) {
        return actions.stream().mapToInt(Action::getCodeAction).sum();
    }

    /** Noms techniques des actions disponibles sur une permission. */
    private Set<String> actionNames(Permission permission) {
        return permission.getActions().stream()
                .map(Action::getName)
                .collect(Collectors.toSet());
    }

    private PermissionType parsePermission(String name) {
        try {
            return PermissionType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Permission inconnue : " + name);
        }
    }

    private ActionType parseAction(String name) {
        try {
            return ActionType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Action inconnue : " + name);
        }
    }

    /**
     * Normalise un nom de rôle : vide/nul refusé ({@code BadRequestException}),
     * sinon trim + majuscules pour éviter les doublons de casse.
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Le nom du rôle est requis");
        }
        return name.trim().toUpperCase();
    }
}
