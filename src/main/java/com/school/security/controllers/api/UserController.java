package com.school.security.controllers.api;


import com.school.security.dtos.requests.AttachRoleReqDto;
import com.school.security.dtos.requests.PwdReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.exceptions.EntityException;
import com.school.security.securities.services.JwtService;
import com.school.security.services.contracts.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur de gestion des utilisateurs, sous le préfixe {@code /users}.
 * Délègue toute la logique à {@link UserService}.
 *
 * <p>Le contrôle d'accès réel combine la filter-chain
 * ({@code SecurityConfig}) et les annotations {@code @PreAuthorize}. Vue
 * d'ensemble par endpoint (vérifié dans {@code SecurityConfig}) :
 * <ul>
 *   <li>publics (aucune sécurité, via {@code permitAll}) : {@code GET
 *       /users} reste toutefois soumis à {@code @PreAuthorize(VIEW_USERS)},
 *       {@code GET /users/email}, {@code GET /users/active},
 *       {@code GET /users/disable} et {@code GET /users/{id}/image} ;</li>
 *   <li>{@code PUT /users/pwd} reste {@code permitAll} (récupération de
 *       compte, utilisateur non connecté) mais exige le cookie
 *       {@code recoveryToken} valide et correspondant à l'email du corps ;</li>
 *   <li>limites directement en filter-chain (sans {@code @PreAuthorize}) :
 *       {@code PUT /users/account} exige {@code ADMIN} ou
 *       {@code SUPER_ADMIN}, et {@code GET /users/admins} exige
 *       {@code SUPER_ADMIN} ;</li>
 *   <li>protégés par {@code @PreAuthorize} : {@code GET /users}
 *       ({@code VIEW_USERS}), {@code DELETE /users/{id}}
 *       ({@code MANAGE_USERS}), {@code PUT /users/role} et
 *       {@code DELETE /users/role} ({@code MANAGE_USERS}) ainsi que
 *       {@code GET /users/project/{id}/contributors}
 *       ({@code MANAGE_PROJECT_CONTRIBUTORS} sur le projet) ;</li>
 *   <li>{@code POST /users/{id}/image} : n'est plus {@code permitAll},
 *       soumis à une exigence d'authentification via
 *       {@code anyRequest().authenticated()}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/users")

public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Liste tous les utilisateurs ({@code GET /users}).
     *
     * <p>Bien que {@code permitAll} au niveau de la filter-chain, l'accès est
     * restreint à la permission métier {@code VIEW_USERS} par
     * {@code @PreAuthorize}. Délègue à {@code UserService.findAll()}.
     */
    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('VIEW_USERS')")
    public List<UserResDto> getAllUsers() {
        return this.userService.findAll();
    }

    /**
     * Suppression définitive d'un utilisateur ({@code DELETE /users/{id}}).
     *
     * <p>Protégé par la permission métier {@code MANAGE_USERS} et soumis à
     * authentification (rule {@code anyRequest().authenticated()}). Délègue à
     * {@code UserService.deleteById(id)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public UserResDto deleteById(@PathVariable Long id) {
        return this.userService.deleteById(id);
    }

    /**
     * Récupère un utilisateur par identifiant ({@code GET /users/{id}}).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public, accessible sans authentification. Délègue à
     * {@code UserService.findById(id)}.
     */
    @GetMapping("/{id}")
    public UserResDto getUser(@PathVariable Long id) {
        return this.userService.findById(id);
    }

    /**
     * Récupère un utilisateur par adresse email ({@code GET /users/email}).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public exposant les données d'un utilisateur à partir de son
     * email. Délègue à {@code UserService.getUserRestByEmail(email)}.
     */
    @GetMapping("/email")
    public UserResDto getUserByEmail(@RequestParam String email) {
        return this.userService.getUserRestByEmail(email);
    }

    /**
     * Finalisation de la récupération de mot de passe ({@code PUT /users/pwd}).
     *
     * <p>Endpoint déclaré {@code permitAll} en filter-chain car il sert au flux
     * de récupération de compte (utilisateur non connecté). Pour empêcher la
     * réinitialisation frauduleuse du mot de passe de n'importe quel compte, la
     * requête doit porter le cookie de récupération {@code recoveryToken} (posé
     * par {@code POST /auth/code} pour l'email concerné, voir
     * {@code AuthController.generateCode}) : si le cookie est absent ou invalide/
     * expiré, ou si l'email du corps ne correspond pas à celui du token, une
     * {@code EntityException} est levée. Délègue à
     * {@code UserService.updatePassword(email, password)}.
     */
    @PutMapping("/pwd")
    public UserResDto updatePassword(
            @RequestBody PwdReqDto pwdReqDto,
            @CookieValue(value = "recoveryToken", required = false) String recoveryToken) {
        if (recoveryToken == null || !jwtService.isRecoveryTokenValid(recoveryToken)) {
            throw new EntityException("Token de récupération invalide ou expiré.");
        }
        String tokenEmail = jwtService.extractUsername(recoveryToken);
        String requestEmail = pwdReqDto.email() == null ? "" : pwdReqDto.email().trim();
        if (!tokenEmail.equalsIgnoreCase(requestEmail)) {
            throw new EntityException("L'email ne correspond pas à la session de récupération.");
        }
        return userService.updatePassword(pwdReqDto.email(), pwdReqDto.password());
    }

    /**
     * Attribution d'un rôle à un utilisateur ({@code PUT /users/role}).
     *
     * <p>La filter-chain déclare {@code permitAll} ce chemin, mais
     * l'annotation {@code @PreAuthorize} restreint l'accès à la permission
     * métier {@code MANAGE_USERS} (refusée aux requêtes non authentifiées).
     * Délègue à {@code UserService.attachRole(email, role)}.
     */
    @PutMapping("/role")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public ResponseEntity<UserResDto> updateRole(@RequestBody AttachRoleReqDto attachRoleRegDto) {
        UserResDto userResDto =
                userService.attachRole(attachRoleRegDto.email(), attachRoleRegDto.role());
        return ResponseEntity.ok(userResDto);
    }

    /**
     * Retrait d'un rôle ({@code DELETE /users/role}).
     *
     * <p>Protégé par la permission métier {@code MANAGE_USERS}
     * ({@code @PreAuthorize}) — symétrique de {@code PUT /users/role}. Le
     * chemin n'est pas {@code permitAll} en filter-chain : il tombe sous
     * {@code anyRequest().authenticated()}. Délègue à
     * {@code UserService.detachRole(email, role)}.
     */
    @DeleteMapping("/role")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public ResponseEntity<UserResDto> deleteRole(@RequestBody AttachRoleReqDto attachRoleRegDto) {
        UserResDto userResDto =
                userService.detachRole(attachRoleRegDto.email(), attachRoleRegDto.role());
        return ResponseEntity.ok(userResDto);
    }

    // Pour activer et désactiver une compte

    /**
     * Activation / désactivation d'un compte ({@code PUT /users/account}).
     *
     * <p>Aucune {@code @PreAuthorize}, mais la filter-chain limite ce chemin
     * aux autorités {@code ADMIN} et {@code SUPER_ADMIN}. Le corps de
     * réponse est un simple message ({@code Map}). Délègue à
     * {@code UserService.updateAccount(email, isActive)}.
     */
    @PutMapping("/account")
    public ResponseEntity<?> updateAccountStatus(
            @RequestParam String email,
            @RequestParam Boolean isActive
    ) {

        userService.updateAccount(email, isActive);

        return ResponseEntity.ok(
                Map.of("message", "Account  updated successfully")
        );
    }

    /**
     * Liste des comptes actifs ({@code GET /users/active}).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public. Délègue à {@code UserService.findAllUserActive()}.
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserResDto>> findAllUserActive() {
        return ResponseEntity.ok(userService.findAllUserActive());
    }

    /**
     * Liste des comptes désactivés ({@code GET /users/disable}).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public. Délègue à {@code UserService.findAllUserDisable()}.
     */
    @GetMapping("/disable")
    public ResponseEntity<List<UserResDto>> findAllUsersDisable() {
        return ResponseEntity.ok(userService.findAllUserDisable());
    }

    /**
     * Liste des utilisateurs ayant le rôle {@code ADMIN}
     * ({@code GET /users/admins}).
     *
     * <p>Aucune {@code @PreAuthorize}, mais la filter-chain limite ce chemin
     * à l'autorité {@code SUPER_ADMIN}. Délègue à
     * {@code UserService.findAllByRole(RoleType.ADMIN)}.
     */
    @GetMapping("/admins")
    public ResponseEntity<List<UserResDto>> findAllAdmins() {
        return ResponseEntity.ok(userService.findAllByRole("ADMIN"));
    }

    /**
     * Contributeurs potentiels d'un projet
     * ({@code GET /users/project/{projectId}/contributors}).
     *
     * <p>Protégé par {@code @PreAuthorize} exigeant la permission métier
     * {@code MANAGE_PROJECT_CONTRIBUTORS} sur le projet désigné (annotation
     * avec liaison d'argument {@code #projectId}) ; soumis par ailleurs à
     * authentification (le chemin multi-segments n'est pas couvert par les
     * {@code permitAll} de la filter-chain). Délègue à
     * {@code UserService.findPotentialContributors(projectId)}.
     */
    @GetMapping("/project/{projectId}/contributors")
    @PreAuthorize("@permissionEvaluator.hasProjectPermission(#projectId, 'MANAGE_PROJECT_CONTRIBUTORS')")
    public List<UserResDto> getPotentialContributors(@PathVariable Long projectId) {
        return userService.findPotentialContributors(projectId);
    }

    /**
     * Ajout / remplacement de l'image d'un utilisateur
     * ({@code POST /users/{id}/image}, multipart/form-data).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public, tout client peut poser l'image de n'importe quel
     * utilisateur. Délègue à {@code UserService.addImageToUser(id, image)}.
     */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResDto> addImageToUser(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image
    )
    {
        return ResponseEntity.ok(
                userService.addImageToUser(id, image)
        );
    }

    /**
     * Récupération de l'image d'un utilisateur ({@code GET /users/{id}/image}).
     *
     * <p>Aucune {@code @PreAuthorize} et {@code permitAll} en filter-chain :
     * endpoint public. Le Content-Type est déduit du fichier via
     * {@code Files.probeContentType} (avec repli sur
     * {@code application/octet-stream}). Délègue à
     * {@code UserService.getUserImage(id)}.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getUserImage(
            @PathVariable Long id
    ) throws IOException {

        Resource image = userService.getUserImage(id);

        String contentType = Files.probeContentType(
                image.getFile().toPath()
        );

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .body(image);
    }

}
