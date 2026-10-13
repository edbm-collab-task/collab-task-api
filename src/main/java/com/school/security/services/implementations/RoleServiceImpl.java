package com.school.security.services.implementations;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionActionsResDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleActionsResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.ActionType;
import com.school.security.enums.PermissionType;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import com.school.security.entities.RolePermission;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.repositories.PermissionRepository;
import com.school.security.repositories.RoleRepository;
import com.school.security.services.contracts.RoleService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des rôles et des permissions.
 *
 * <p>Règles métier constatées (documentées, non modifiées) :
 * <ul>
 *   <li>la création d'un rôle ({@code create}) n'est autorisée que si ce rôle
 *       n'existe pas déjà ; une {@code EntityException} est levée en cas de doublon.</li>
 *   <li>la modification d'un rôle ({@code update}) ne remplace que les {@link
 *       PermissionType permissions} fournis dans le DTO ; les autres attributs
 *       du rôle ne sont pas modifiés.</li>
 *   <li>la suppression d'un rôle ({@code delete}) ne vérifie pas la présence de
 *       dépendances (utilisateurs ou projets affectés) ; la suppression en base
 *       est directe via {@code roleRepository.delete}.</li>
 *   <li>les permissions sont résolues via {@link PermissionRepository} à partir
 *       de leur nom ({@code PermissionType.name}) ; une {@code EntityException}
 *       est levée si une permission sollicitée est introuvable.</li>
 *   <li>la catégorie d'une permission ({@code PermissionCategoryType}) est une
 *       donnée de référence, alimentée par {@code data.sql} et exposée en
 *       lecture seule ; aucune méthode de ce service ne la modifie.</li>
 *   <li>Aucune vérification {@code @PreAuthorize} {@code SUPER_ADMIN} n'est
 *       effectuée dans ce service ; les contrôles d'accès s'appliquent au niveau
 *       des contrôleurs ou de la filter-chain {@code SecurityConfig}.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private RoleMapper roleMapper;

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

    /** Met à jour les permissions d'un rôle existant.
     *
     * <p>Lève une {@code EntityException} si le rôle n'existe pas. Les
     * permissions optionnelles du DTO remplacent les permissions existantes ;
     * les autres attributs du rôle ne sont pas modifiés.</p>
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
     * <p>Lève une {@code EntityException} si le rôle n'existe pas. Aucune
     * vérification de dépendances (utilisateurs ou projets affectés) n'est
     * effectuée ; la suppression en base est directe.</p>
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
     * Retourne toutes les permissions, chacune accompagnée de la liste complète
     * des actions possibles ({@link ActionType}).
     *
     * <p>Le jeu d'actions est générique et réutilisé pour tous les modules : il
     * est donc calculé une seule fois puis partagé par toutes les entrées. Le
     * tri (catégorie puis nom) est identique à {@link #findAllPermissions()}.
     */
    @Override
    public List<PermissionActionsResDto> findAllPermissionsWithActions() {
        List<String> allActions = Arrays.stream(ActionType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc().stream()
                .map(p -> new PermissionActionsResDto(
                        p.getPermissionId(),
                        p.getName().name(),
                        p.getDescription(),
                        p.getCategoryPermission(),
                        allActions))
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

    /** Catalogue complet des actions gérables (INSERT, UPDATE, ...). */
    @Override
    public List<ActionType> findAllActions() {
        return List.of(ActionType.values());
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

    /** Actions d'un rôle, regroupées par module. */
    @Override
    public RoleActionsResDto getRoleActions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityException("Role not found with ID " + roleId));
        return toRoleActions(role);
    }

    /**
     * Remplace la configuration d'actions d'un rôle.
     *
     * <p>Le corps associe chaque module à sa liste d'actions ; une liste vide
     * conserve l'accès en lecture seule. Les modules absents sont retirés du
     * rôle (l'assignation d'une action implique l'accès au module). Les lignes
     * existantes sont réutilisées afin d'éviter toute collision de clé
     * composite lors du flush.
     */
    @Override
    public RoleActionsResDto setRoleActions(Long roleId, Map<String, List<String>> actions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityException("Role not found with ID " + roleId));

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
                RolePermission rp = existing.get(permission.getPermissionId());
                if (rp == null) {
                    rp = new RolePermission();
                    rp.setRole(role);
                    rp.setPermission(permission);
                }
                rp.setActions(joinActions(entry.getValue()));
                rebuilt.add(rp);
            }
        }

        role.getRolePermissions().clear();
        role.getRolePermissions().addAll(rebuilt);
        roleRepository.save(role);
        return toRoleActions(role);
    }

    /** Regroupe les actions d'un rôle par module, modules triés par nom. */
    private RoleActionsResDto toRoleActions(Role role) {
        Map<String, List<String>> permissions = new LinkedHashMap<>();
        role.getRolePermissions().stream()
                .sorted(Comparator.comparing(rp -> rp.getPermission().getName().name()))
                .forEach(rp -> permissions.put(
                        rp.getPermission().getName().name(),
                        parseActions(rp.getActions())));
        return new RoleActionsResDto(role.getName(), permissions);
    }

    /** Convertit une liste d'actions en chaîne normalisée, sans doublon. */
    private String joinActions(List<String> actionNames) {
        if (actionNames == null) return "";
        return actionNames.stream()
                .map(this::parseAction)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /** Convertit une chaîne d'actions stockée en liste. */
    private List<String> parseActions(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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
