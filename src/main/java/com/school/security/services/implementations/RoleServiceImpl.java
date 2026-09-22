package com.school.security.services.implementations;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.PermissionType;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.repositories.PermissionRepository;
import com.school.security.repositories.RoleRepository;
import com.school.security.services.contracts.RoleService;
import java.util.ArrayList;
import java.util.List;
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
     * <p>Vérifie que le rôle n'existe pas déjà ; lève une {@code EntityException}
     * en cas de doublon. Les permissions optionnelles sont résolues depuis la
 * base de données via {@link PermissionRepository}.</p>
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

    /** Retourne la liste de toutes les permissions du système. */
    @Override
    public List<PermissionResDto> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResDto(p.getPermissionId(), p.getName().name(), p.getDescription()))
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
