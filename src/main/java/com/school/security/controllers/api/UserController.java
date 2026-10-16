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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Contrôleur de gestion des utilisateurs, sous le préfixe {@code /users}.
 * Délègue toute la logique à {@link UserService}.
 *
 * <p>Le contrôle d'accès réel combine la filter-chain
 * ({@code SecurityConfig}) et les annotations {@code @PreAuthorize}. Vue
 * d'ensemble par endpoint (vérifié dans {@code SecurityConfig}) :
 * <ul>
 *   <li>publics (aucune sécurité, via {@code permitAll}) : {@code GET
 *       /users} reste toutefois soumis à
 *       {@code @PreAuthorize(hasAction('MANAGE_USERS','SELECT'))},
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
 *       ({@code MANAGE_USERS} + action {@code SELECT}), {@code DELETE /users/{id}}
 *       ({@code MANAGE_USERS}), {@code PUT /users/role} et
 *       {@code DELETE /users/role} ({@code MANAGE_USERS}) ainsi que
 *       {@code GET /users/project/{id}/contributors}
 *       ({@code MANAGE_PROJECT_CONTRIBUTORS} sur le projet) ;</li>
 *   <li>{@code POST /users/{id}/image} : n'est plus {@code permitAll},
 *       soumis à une exigence d'authentification via
 *       {@code anyRequest().authenticated()}.</li>
 * </ul>
 */
@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs : consultation, création de comptes par un admin, rôles, activation/désactivation, mot de passe et images de profil. L'accès combine la filter-chain et les annotations @PreAuthorize (MANAGE_USERS, SUPER_ADMIN, MANAGE_PROJECT_CONTRIBUTORS).")
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
     * <p>Bien que {@code permitAll} au niveau de la filter-chain, l'accès exige
     * la permission {@code MANAGE_USERS} et l'action {@code SELECT}
     * ({@code VIEW_USERS} a été supprimée : la consultation est désormais
     * couverte par l'action SELECT de {@code MANAGE_USERS}). Délègue à
     * {@code UserService.findAll()}.
     */
    @Operation(summary = "Lister tous les utilisateurs", description = "Retourne la liste complète des utilisateurs. Endpoint public en filter-chain mais soumis à la permission MANAGE_USERS avec l'action SELECT via @PreAuthorize.")
    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasAction('MANAGE_USERS', 'SELECT')")
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
    @Operation(summary = "Supprimer définitivement un utilisateur", description = "Supprime l'utilisateur identifié par son id. Protégé par la permission MANAGE_USERS via @PreAuthorize et soumis à authentification.")
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
    @Operation(summary = "Récupérer un utilisateur par son identifiant", description = "Renvoie les informations d'un utilisateur à partir de son identifiant. Endpoint public, accessible sans authentification.")
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
    @Operation(summary = "Récupérer un utilisateur par email", description = "Renvoie les informations d'un utilisateur à partir de son adresse email. Endpoint public, accessible sans authentification.")
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
    @Operation(summary = "Réinitialiser un mot de passe", description = "Finalise la récupération de mot de passe pour un utilisateur non connecté. Exige le cookie recoveryToken valide (posé par POST /auth/code) et correspondant à l'email du corps, sinon une EntityException est levée.")
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
    @Operation(summary = "Attribuer un rôle à un utilisateur", description = "Assigne un rôle à l'utilisateur identifié par son email. Protégé par la permission MANAGE_USERS via @PreAuthorize, refusée aux requêtes non authentifiées.")
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
    @Operation(summary = "Retirer un rôle à un utilisateur", description = "Retire le rôle spécifié de l'utilisateur identifié par son email. Protégé par la permission MANAGE_USERS via @PreAuthorize et soumis à authentification.")
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
    @Operation(summary = "Activer ou désactiver un compte", description = "Active ou désactive le compte correspondant à l'email fourni. Sans @PreAuthorize mais limité en filter-chain aux autorités ADMIN et SUPER_ADMIN.")
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
    @Operation(summary = "Lister les comptes actifs", description = "Renvoie la liste des utilisateurs dont le compte est actif. Endpoint public, sans authentification requise.")
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
    @Operation(summary = "Lister les comptes désactivés", description = "Renvoie la liste des utilisateurs dont le compte est désactivé. Endpoint public, sans authentification requise.")
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
    @Operation(summary = "Lister les administrateurs", description = "Renvoie tous les utilisateurs possédant le rôle ADMIN. Sans @PreAuthorize mais limité en filter-chain à l'autorité SUPER_ADMIN.")
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
    @Operation(summary = "Contributeurs potentiels d'un projet", description = "Liste les utilisateurs pouvant être ajoutés comme contributeurs du projet indiqué. Protégé par la permission MANAGE_PROJECT_CONTRIBUTORS sur le projet via @PreAuthorize, avec authentification requise.")
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
    @Operation(summary = "Ajouter ou remplacer une image de profil", description = "Téléverse (multipart/form-data) l'image de l'utilisateur identifié et renvoie le compte mis à jour. Endpoint public : aucune restriction d'accès n'est appliquée au niveau de la filter-chain ou des permissions.")
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
    @Operation(summary = "Récupérer l'image de profil", description = "Renvoie l'image de l'utilisateur identifié, le Content-Type étant déduit du fichier (avec repli sur application/octet-stream). Endpoint public, sans authentification requise.")
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
