package com.school.security.services.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import com.school.security.enums.PermissionCategoryType;
import com.school.security.enums.PermissionType;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.repositories.PermissionRepository;
import com.school.security.repositories.RoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests de {@link RoleServiceImpl} ciblés sur l'exposition de la catégorie de
 * permission ({@code categoryPermission}) et sur le fait que le contrat de
 * requête reste exprimé en noms de permissions.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleRepository roleRepository;

    @Mock private PermissionRepository permissionRepository;

    @Mock private RoleMapper roleMapper;

    @InjectMocks private RoleServiceImpl roleService;

    private Permission viewUsers;
    private Permission manageProjects;
    private Permission manageRoles;

    @BeforeEach
    void setUp() {
        viewUsers = buildPermission(1L, PermissionType.VIEW_USERS, PermissionCategoryType.UTILISATEURS);
        manageProjects = buildPermission(2L, PermissionType.MANAGE_PROJECTS, PermissionCategoryType.PROJETS);
        manageRoles = buildPermission(3L, PermissionType.MANAGE_ROLES, PermissionCategoryType.ORGANISATION);
    }

    // ─── findAllPermissions ────────────────────────────────────────

    @Test
    void findAllPermissionsShouldExposeCategoryPermission() {
        when(permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc())
                .thenReturn(List.of(viewUsers, manageProjects, manageRoles));

        List<PermissionResDto> result = roleService.findAllPermissions();

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("VIEW_USERS", result.get(0).name());
        assertEquals("Description VIEW_USERS", result.get(0).description());
        assertEquals(PermissionCategoryType.UTILISATEURS, result.get(0).categoryPermission());
        assertEquals(PermissionCategoryType.PROJETS, result.get(1).categoryPermission());
        assertEquals(PermissionCategoryType.ORGANISATION, result.get(2).categoryPermission());
    }

    @Test
    void findAllPermissionsShouldUseTheCategorizedSortedQuery() {
        when(permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc())
                .thenReturn(List.of(viewUsers));

        roleService.findAllPermissions();

        // L'ordre de tri est garanti par la requête : sans elle, le tri des
        // catégories côté client serait non déterministe.
        verify(permissionRepository).findAllByOrderByCategoryPermissionAscNameAsc();
        verify(permissionRepository, never()).findAll();
    }

    @Test
    void findAllPermissionsShouldReturnNullCategoryForUnmigratedRow() {
        // Une permission héritée d'une base antérieure au backfill de data.sql
        // doit rester sérialisable (catégorie nulle) plutôt que faire échouer
        // tout l'endpoint.
        Permission legacy = buildPermission(9L, PermissionType.MANAGE_STATUSES, null);
        when(permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc())
                .thenReturn(List.of(legacy));

        List<PermissionResDto> result = roleService.findAllPermissions();

        assertEquals(1, result.size());
        assertNull(result.get(0).categoryPermission());
    }

    @Test
    void findAllPermissionsShouldReturnEmptyListWhenNoPermission() {
        when(permissionRepository.findAllByOrderByCategoryPermissionAscNameAsc())
                .thenReturn(List.of());

        assertEquals(List.of(), roleService.findAllPermissions());
    }

    // ─── create : le contrat de requête reste exprimé en noms ──────

    @Test
    void createShouldStillResolvePermissionsByName() {
        when(roleRepository.findByName("EDITOR")).thenReturn(Optional.empty());
        when(permissionRepository.findByName(PermissionType.MANAGE_PROJECTS))
                .thenReturn(Optional.of(manageProjects));
        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        roleService.create(new RoleReqDto("editor", "EDT", List.of(PermissionType.MANAGE_PROJECTS)));

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertEquals(List.of(manageProjects), captor.getValue().getPermissions());
        // La catégorie n'est pas modifiable via l'API : elle vient de la base.
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    void createShouldFailOnUnknownPermissionName() {
        when(roleRepository.findByName("EDITOR")).thenReturn(Optional.empty());
        when(permissionRepository.findByName(PermissionType.MANAGE_PROJECTS))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityException.class,
                () ->
                        roleService.create(
                                new RoleReqDto("editor", "EDT", List.of(PermissionType.MANAGE_PROJECTS))));
    }

    // ─── helpers ───────────────────────────────────────────────────

    private Permission buildPermission(
            Long id, PermissionType name, PermissionCategoryType category) {
        Permission permission = new Permission();
        permission.setPermissionId(id);
        permission.setName(name);
        permission.setDescription("Description " + name.name());
        permission.setCategoryPermission(category);
        return permission;
    }
}
