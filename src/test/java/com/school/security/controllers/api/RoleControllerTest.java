package com.school.security.controllers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.enums.PermissionCategoryType;
import com.school.security.enums.PermissionType;
import com.school.security.services.contracts.RoleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests de {@link RoleController} verrouillant le contrat JSON exposé au
 * client : la clé de catégorie est {@code categoryPermission}, et le corps de
 * requête des rôles reste une liste de <em>noms</em> de permissions.
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock private RoleService roleService;

    @InjectMocks private RoleController roleController;

    @Captor private ArgumentCaptor<RoleReqDto> roleReqCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
    }

    // ─── GET /roles/permissions ─────────────────────────────────────

    @Test
    void getAllPermissionsShouldSerializeCategoryPermission() throws Exception {
        when(roleService.findAllPermissions())
                .thenReturn(
                        List.of(
                                new PermissionResDto(
                                        1L,
                                        "MANAGE_PROJECTS",
                                        "Créer et gérer les projets",
                                        PermissionCategoryType.PROJETS),
                                new PermissionResDto(
                                        2L,
                                        "VIEW_REPORTS",
                                        "Voir les rapports et statistiques",
                                        PermissionCategoryType.RAPPORTS)));

        mockMvc.perform(get("/roles/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("MANAGE_PROJECTS"))
                .andExpect(jsonPath("$[0].description").value("Créer et gérer les projets"))
                .andExpect(jsonPath("$[0].categoryPermission").value("PROJETS"))
                .andExpect(jsonPath("$[1].categoryPermission").value("RAPPORTS"));
    }

    @Test
    void getAllPermissionsShouldSerializeNullCategory() throws Exception {
        when(roleService.findAllPermissions())
                .thenReturn(
                        List.of(
                                new PermissionResDto(
                                        9L, "MANAGE_STATUSES", "Gérer les statuts", null)));

        mockMvc.perform(get("/roles/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("MANAGE_STATUSES"))
                .andExpect(jsonPath("$[0].categoryPermission").doesNotExist());
    }

    @Test
    void getAllPermissionsShouldReturnEmptyArray() throws Exception {
        when(roleService.findAllPermissions()).thenReturn(List.of());

        mockMvc.perform(get("/roles/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /roles ────────────────────────────────────────────────

    @Test
    void getAllShouldExposeCategoryOnEachRolePermission() throws Exception {
        when(roleService.findAll())
                .thenReturn(
                        List.of(
                                new RoleResDto(
                                        1L,
                                        "ADMIN",
                                        "ADM",
                                        List.of(
                                                new PermissionResDto(
                                                        1L,
                                                        "MANAGE_PROJECTS",
                                                        "Créer et gérer les projets",
                                                        PermissionCategoryType.PROJETS),
                                                new PermissionResDto(
                                                        2L,
                                                        "VIEW_USERS",
                                                        "Voir la liste des utilisateurs",
                                                        PermissionCategoryType.UTILISATEURS)))));

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ADMIN"))
                .andExpect(jsonPath("$[0].permissions").isArray())
                .andExpect(jsonPath("$[0].permissions[0].name").value("MANAGE_PROJECTS"))
                .andExpect(jsonPath("$[0].permissions[0].categoryPermission").value("PROJETS"))
                .andExpect(jsonPath("$[0].permissions[1].categoryPermission").value("UTILISATEURS"));
    }

    // ─── POST /roles : requête inchangée (noms de permissions) ──────

    @Test
    void createShouldAcceptPermissionNamesInRequestBody() throws Exception {
        when(roleService.create(any(RoleReqDto.class)))
                .thenReturn(new RoleResDto(5L, "EDITOR", "EDT", List.of()));

        mockMvc.perform(
                        post("/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "EDITOR",
                                          "codeRole": "EDT",
                                          "permissions": ["MANAGE_PROJECTS", "VIEW_REPORTS"]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeRole").value("EDT"));

        verify(roleService).create(roleReqCaptor.capture());
        assertEquals(
                List.of(PermissionType.MANAGE_PROJECTS, PermissionType.VIEW_REPORTS),
                roleReqCaptor.getValue().permissions());
    }
}
