package com.school.security.controllers.auth;


import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.EmailReq;
import com.school.security.dtos.requests.LoginReqDto;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.LoginResDto;
import com.school.security.dtos.responses.PermissionSummary;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Permission;
import com.school.security.enums.ActionType;
import com.school.security.entities.RolePermission;
import com.school.security.entities.User;
import com.school.security.mappers.DirectionMapper;
import com.school.security.mappers.UserMapper;
import com.school.security.repositories.DirectionRepository;
import com.school.security.securities.services.JwtService;
import com.school.security.securities.utils.CookieUtils;
import com.school.security.services.contracts.DirectionService;
import com.school.security.services.contracts.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import io.jsonwebtoken.JwtException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



/**
 * Contrôleur d'authentification : gère les mappings sous le préfixe
 * {@code /auth}.
 *
 * <p>C'est ici que se fait la remise des tokens JWT. Le login émet l'access
 * token, le refresh token et l'email sous forme de cookies HTTP
 * (via {@code CookieUtils}) : les réponses successives n'ont donc pas besoin
 * de renvoyer un header {@code Authorization}, le navigateur renvoie
 * automatiquement les cookies {@code accessToken} / {@code refreshToken}
 * (exploités par {@code JwtAuthenticationFilter}).
 *
 * <p>Points d'attention constatés (documentés, non corrigés) :
 * <ul>
 *   <li>le constructeur reçoit deux paramètres {@code UserMapper}
 *       ({@code userMapper} et {@code userMapper1}) ; seul {@code userMapper}
 *       est stocké en champ ;</li>
 *   <li>{@code DirectionService} est importé mais non utilisé ici ;</li>
 *   <li>le champ {@code directionRepository} est injecté mais jamais
 *       utilisé dans ce contrôleur ;</li>
 *   <li>l'annotation {@code @Slf4j} déclare un logger
 *       ({@code log}) sans usage dans cette classe.</li>
 * </ul>
 *
 * <p>Les contrôleurs {@code /auth} étant en {@code permitAll} dans la
 * filter-chain (voir {@code SecurityConfig}), les endpoints de ce contrôleur
 * ne reposent que sur les vérifications internes (votes
 * {@code authenticationManager}, contrôle du refresh token...). Exception :
 * {@code POST /auth/create} est soumis à {@code @PreAuthorize(MANAGE_USERS)}
 * pour réserver la création de compte à l'administration.
 */
@Tag(name = "Authentification", description = "Endpoints d'authentification et de gestion des sessions : connexion, inscription, rafraîchissement des tokens JWT, récupération de mot de passe et statut de présence. La plupart des routes sont publiques (permitAll), sauf la création de compte réservée à la permission MANAGE_USERS.")
@RestController
@Slf4j
@RequestMapping("/auth")

public class AuthController {


    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserMapper userMapper;
    private final DirectionRepository directionRepository;




    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService, UserMapper userMapper, EmailService emailService, UserMapper userMapper1, DirectionRepository directionRepository
    ) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.directionRepository = directionRepository;
    }

    /**
     * Connexion d'un utilisateur (endpoint public {@code POST /auth/login}).
     *
     * <p>Etapes :
     * <ol>
     *   <li>normalisation de l'email (trim + minuscules) ;</li>
     *   <li>authentification via {@code AuthenticationManager} (échec ->
     *       exception BadCredentials, non interceptée ici) ;</li>
     *   <li>déclaration de présence en ligne ({@code updateStatus(email, true)}) ;</li>
     *   <li>génération d'un access token (1 h) et d'un refresh token (7 j) ;</li>
     *   <li>mise en cookie de {@code accessToken}, {@code refreshToken} et
     *       {@code email}.</li>
     * </ol>
     *
     * <p>Le {@code LoginResDto} renvoyé expose le PREMIER rôle de l'utilisateur
     * ({@code getRoles().getFirst()}) ainsi que la liste de ses permissions.
     */
    /**
     * LOGIN
     */
    @Operation(summary = "Connexion d'un utilisateur", description = "Authentifie un utilisateur par email et mot de passe. En cas de succès, marque l'utilisateur en ligne et remet l'access token (1 h), le refresh token (7 j) et l'email sous forme de cookies HTTP ; renvoie aussi le premier rôle et les permissions de l'utilisateur. Endpoint public.")
    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(
            @RequestBody LoginReqDto credential,
            HttpServletResponse response
    ) {

        // Normalisation : l'email est aplati en minuscules/trim avant toute
        // recherche en base et authentification.
        String email = credential.email() != null
                ? credential.email().trim().toLowerCase()
                : null;

        // Vérification des identifiants (email + mot de passe haché BCrypt).
        // En cas d'échec, l'exception remonte et la réponse est une 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        credential.password()
                )
        );

        var user = userService.findByEmail(email);
        // Marquage de l'utilisateur comme en ligne AVANT la génération des
        // tokens (l'état de présence suit le cycle login/logout).
        updateStatus(user.getEmail(),true);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        // Les tokens sont remis exclusivement via cookies : le client n'a pas
        // besoin de stocker les tokens en mémoire (HttpOnly côté navigateur).
        response.addCookie(
                CookieUtils.createAccessTokenCookie(accessToken)
        );

        response.addCookie(
                CookieUtils.createRefreshTokenCookie(refreshToken)
        );

        response.addCookie(
                CookieUtils.createEmail(email)
        );


        return ResponseEntity.ok(
                new LoginResDto(
                        user.getUsersId(),
                        user.getRoles().getFirst().getName(),
                        user.getRoles().getFirst().getCodeRole(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        buildPermissionSummaries(user),
                        accessToken,
                        refreshToken
                )
        );
    }

    /**
     * Enregistrement/mise à jour d'un utilisateur : délégation à
     * {@code UserService.createOrUpdate}. Endpoint public.
     */
    /**
     * REGISTER
     */
    @Operation(summary = "Inscription ou mise à jour d'un compte", description = "Crée ou met à jour un utilisateur à partir des données fournies, via UserService.createOrUpdate. Endpoint public, sans authentification requise.")
    @PostMapping("/register")
    public UserResDto register(@RequestBody UserReqDto userReqDto)
    {
        return userService.createOrUpdate(userReqDto);
    }

    /**
     * Création d'un utilisateur par un administrateur : délégation à
     * {@code UserService.create} (mot de passe temporaire généré,
     * voir {@link #generateRandomNumericString}).
     *
     * <p>Bien que la filter-chain déclare {@code POST /auth/create} en
     * {@code permitAll}, l'accès est restreint à la permission métier
     * {@code MANAGE_USERS} par {@code @PreAuthorize} (refusée aux requêtes
     * non authentifiées).
     */
    /**
     * CREATE
     */
    @Operation(summary = "Création d'un compte par un administrateur", description = "Crée un utilisateur avec un mot de passe temporaire généré automatiquement. Restreint aux requêtes authentifiées disposant de la permission MANAGE_USERS via @PreAuthorize, malgré le permitAll de la filter-chain.")
    @PostMapping("/create")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public UserResDto create(@RequestBody UserReqDto userReqDto) {return userService.create(userReqDto);}

    /**
     * Génère une chaîne aléatoire de lettres uniquement (majuscules et
     * minuscules), de la longueur demandée. Source : {@code SecureRandom}.
     *
     * <p>NB : malgré son nom, cette méthode ne produit PAS de chiffres
     * (la constante {@code LETTERS} ne contient que des lettres).
     */
    public static String generateRandomString(int length) {

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        return sb.toString();
    }

    /**
     * Génère une chaîne aléatoire composée uniquement de chiffres, de la
     * longueur demandée. Source : {@code SecureRandom}.
     *
     * <p>Utilisée par {@code UserServiceImpl.create} pour le mot de passe
     * temporaire chiffré (12 chiffres) lors de la création d'un compte.
     */
    public static String generateRandomNumericString(int length) {

        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }

        return sb.toString();
    }


    /**
     * Renouvellement des tokens (endpoint public {@code POST /auth/refresh}).
     *
     * <p>Le refresh token est lu dans le cookie {@code refreshToken}. S'il est
     * absent/blank, réponse 401. Sinon :
     * <ol>
     *   <li>extraction de l'email (sujet) et chargement de l'utilisateur ;</li>
     *   <li>validation du token via {@code jwtService.isTokenValid} ;</li>
     *   <li>rotation : nouveau access token (1 h) et nouveau refresh token (7 j)
     *       remis en cookies.</li>
     * </ol>
     *
     * <p>Points d'attention (documentés, non corrigés) :
     * <ul>
     *   <li>{@code isTokenValid} valide n'importe quel token non expiré dont le
     *       sujet correspond : il ne vérifie PAS que le token est bien un refresh
     *       token (un access token ou un token de récupération placé dans le
     *       cookie serait aussi accepté) ;</li>
     *   <li>sur validation refusée ({@code isTokenValid == false}), les cookies
     *       ne sont PAS supprimés ;</li>
     *   <li>les cookies ne sont effacés que sur une invalidation réelle de la
     *       session ({@code JwtException}, compte supprimé, compte sans rôle). Les
     *       erreurs serveur (indisponibilité de la base, erreur de
     *       configuration) remontent en 500 et préservent la session, qui reste
     *       récupérable ;</li>
     * </ul>
     */
    /**
     * REFRESH TOKEN
     */
    @Operation(summary = "Renouvellement des tokens JWT", description = "Valide le refresh token présent dans le cookie refreshToken puis le fait tourner : renvoie un nouvel access token (1 h) et un nouveau refresh token (7 j) en cookies. Renvoie 401 si le cookie est absent, invalide ou expiré, auquel cas les cookies de session sont supprimés. Endpoint public.")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String refreshToken = getCookie(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Refresh token missing"
                    )
            );
        }

        try {

            String email = jwtService.extractUsername(refreshToken);

            // findByEmail lève une IllegalArgumentException si la ligne a disparu
            // (jamais de null) : l'ancien test « if (user == null) » était donc
            // du code mort et ce cas retombait dans le message « expiré ».
            var user = userService.findByEmail(email);

            if (!jwtService.isTokenValid(refreshToken, user)) {

                return ResponseEntity.status(401).body(
                        Map.of(
                                "message",
                                "Invalid refresh token"
                        )
                );
            }

            /*
             * Rotation du Refresh Token
             */

            String newAccessToken = jwtService.generateToken(user);

            String newRefreshToken =
                    jwtService.generateRefreshToken(new HashMap<>(), user);

            response.addCookie(
                    CookieUtils.createAccessTokenCookie(newAccessToken)
            );

            response.addCookie(
                    CookieUtils.createRefreshTokenCookie(newRefreshToken)
            );

            return ResponseEntity.ok(
                    new LoginResDto(
                            user.getUsersId(),
                            user.getRoles().getFirst().getName(),
                            user.getRoles().getFirst().getCodeRole(),
                            user.getFirstname(),
                            user.getLastname(),
user.getEmail(),
                        buildPermissionSummaries(user),
                        newAccessToken,
                        newRefreshToken
                    )
            );

        } catch (JwtException | IllegalArgumentException e) {

            // Session réellement invalide : jeton expiré, malformé, signature
            // invalide, ou compte supprimé. Ces cas-là seuls justifient la
            // suppression des cookies.
            log.warn("[AUTH-REFRESH] refresh token rejeté : {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());

            response.addCookie(CookieUtils.deleteAccessTokenCookie());
            response.addCookie(CookieUtils.deleteRefreshTokenCookie());

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Refresh token invalide ou expiré"
                    )
            );

        } catch (NoSuchElementException e) {

            // Compte sans rôle : getFirst() échoue sur une liste vide. Ce n'est
            // pas une expiration ; le dire plutôt que de le masquer.
            log.warn("[AUTH-REFRESH] aucun rôle associé au compte");

            response.addCookie(CookieUtils.deleteAccessTokenCookie());
            response.addCookie(CookieUtils.deleteRefreshTokenCookie());

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Aucun rôle associé à ce compte"
                    )
            );
        }
    }

    /**
     * Génération d'un code de récupération de mot de passe (endpoint public
     * {@code POST /auth/code}).
     *
     * <p>Comportement :
     * <ol>
     *   <li>chargement de l'utilisateur par email ;</li>
     *   <li>si le compte est désactivé ({@code isActive == false}), réponse 403 ;</li>
     *   <li>sinon, génération d'un code à 6 chiffres, d'un recovery token
     *       (10 min) posé en cookie {@code recoveryToken}, et envoi de l'email
     *       contenant le code.</li>
     * </ol>
     *
     * <p>Points d'attention (documentés, non corrigés) :
     * <ul>
     *   <li>aucune vérification d'existence de l'utilisateur avant l'appel à
     *       {@code user.getIsActive()} : un email inconnu en base déclenche une
     *       erreur côté exécution</li>
     *   <li>le code est généré avec {@code Math.random()} (générateur non
     *       cryptographique, contrairement à {@code SecureRandom}).</li>
     * </ul>
     */
    /**
     * GENERATE CODE
     */
    @Operation(summary = "Génération d'un code de récupération", description = "Génère un code à 6 chiffres et un recovery token (10 min) posé en cookie, puis envoie le code par email. Répond 403 si le compte est désactivé. Endpoint public, utilisé dans le flux de récupération de mot de passe.")
    @PostMapping("/code")
    public ResponseEntity<?> generateCode(
            @RequestBody EmailReq emailReq,
            HttpServletResponse response
    ) {

        var user = userService.findByEmail(emailReq.email());

        // Aucun contrôle préalable : si user == null, l'appel suivant échoue
        // (user.getIsActive() sur null) au lieu de renvoyer une réponse claire.
        if (Boolean.FALSE.equals(user.getIsActive())) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "message",
                            "Ce compte est désactivé, la récupération du mot de passe n'est pas possible."
                    ));
        }

        int code = (int) (Math.random() * 900000) + 100000;

        String recoveryToken =
                jwtService.generateRecoveryToken(user, code);

        String email = user.getEmail();

        response.addCookie(
                CookieUtils.createRecoveryTokenCookie(recoveryToken)

        );

        response.addCookie(
                CookieUtils.createEmail(email)
        );

        emailService.sendRecoveryCodeEmail(
                user.getEmail(),
                user.getFirstname(),
                code
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Le code de récupération a été envoyé à votre adresse email."
                )
        );
    }

    /**
     * Vérification d'un code de récupération saisi par l'utilisateur
     * (endpoint public {@code GET /auth/verification-code}).
     *
     * <p>Le recovery token est lu dans le cookie {@code recoveryToken}, puis le
     * code qu'il embarque (claim {@code code}) est comparé au {@code code}
     * reçu en query param :
     * <ul>
     *   <li>token absent -> 401 "Token introuvable" ;</li>
     *   <li>code différent -> 400 "Code incorrect" ;</li>
     *   <li>toute exception (token invalide/expiré) -> 401 "Code expiré".</li>
     * </ul>
     *
     * <p>NOTE : la validité "recovery" du token (claim {@code type}) n'est PAS
     * vérifiée ici — seul le code est extrait. Un token signé valide portant un
     * code permettrait la vérification.
     */
    @Operation(summary = "Vérification du code de récupération", description = "Compare le code saisi en query param avec celui du recovery token conservé dans le cookie. Répond 401 si le token est absent ou expiré, 400 si le code est incorrect. Endpoint public.")
    @GetMapping("/verification-code")
    public ResponseEntity<?> verification(
            @RequestParam int code,
            HttpServletRequest request
    ) {

        String recoveryToken = getCookie(request, "recoveryToken");

        if (recoveryToken == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Token introuvable"));
        }

        try {

            int tokenCode = jwtService.extractRecoveryCode(recoveryToken);

            if (tokenCode != code) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Code incorrect"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Code valide",
                            "valid", true
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(401)
                    .body(Map.of("message", "Code expiré"));
        }
    }

    /**
     * Déconnexion (endpoint public {@code POST /auth/logout}).
     *
     * <p>Lit l'email dans le cookie {@code email}, marque l'utilisateur comme
     * hors ligne ({@code updateStatus(email, false)}), puis expire les trois
     * cookies d'authentification ({@code accessToken}, {@code refreshToken},
     * {@code email}) côté client.
     */
    /**
     * LOGOUT
     */
    @Operation(summary = "Déconnexion de l'utilisateur", description = "Marque l'utilisateur comme hors ligne à partir du cookie email, puis expire les cookies accessToken, refreshToken et email côté client. Endpoint public.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,HttpServletResponse response) {

        String email = getCookie(request,"email");
        updateStatus(email,false);

        response.addCookie(
                CookieUtils.deleteAccessTokenCookie()
        );

        response.addCookie(
                CookieUtils.deleteRefreshTokenCookie()
        );

        response.addCookie(
                CookieUtils.deleteEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful"
                )
        );
    }

    /**
     * Lire un cookie
     */
    private String getCookie(HttpServletRequest request, String name) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {

            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * Met à jour le statut de présence (online / offline) d'un utilisateur
     * (endpoint public {@code PUT /auth/status}).
     *
     * <p>Appelé par le login (status=true) et le logout (status=false) de ce
     * contrôleur, mais aussi accessible directement par le front via cette URL
     * (cf. filter-chain SecurityConfig, {@code permitAll}).
     */
    @Operation(summary = "Mise à jour du statut de présence", description = "Passe le statut de présence (en ligne / hors ligne) d'un utilisateur identifié par email. Appelé par le login et le logout, mais aussi accessible directement au front. Endpoint public.")
    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(
            @RequestParam String email,
            @RequestParam Boolean status
    ) {
        userService.updateStatus(email, status);

        return ResponseEntity.ok(
                Map.of("message", "Status updated successfully")
        );
    }

    /**
     * Extrait la liste des noms de permissions d'un utilisateur (rôle par rôle,
     * sans doublon). Utilisée dans les DTOs de réponse de login/refresh/me.
     */
    /**
     * Construit le résumé des permissions de l'utilisateur authentifié :
     * pour chaque permission, le masque des actions disponibles ({@code totalActions})
     * et le masque des actions accordées ({@code totalActionsAccordees}) via ses rôles.
     */
    private List<PermissionSummary> buildPermissionSummaries(User user) {
        // Collecte des noms de permissions distincts présents dans ses rôles
        Set<String> permNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName().name())
                .collect(Collectors.toSet());

        // Map du totalActions par permission (puisque c'est la même valeur catalogue pour tous les rôles)
        Map<String, Integer> totalActionsMap = permNames.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        perm -> user.getRoles().stream()
                                .flatMap(role -> role.getPermissions().stream())
                                .filter(p -> p.getName().name().equals(perm))
                                .findFirst()
                                .map(Permission::getTotalActions)
                                .orElse(0)
                ));

        // Map du totalActionsAccordees : agrégation des masques binaires sur tous les rolePermissions
        Map<String, Integer> accruedMap = permNames.stream()
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        perm -> user.getRoles().stream()
                                .flatMap(role -> role.getRolePermissions().stream())
                                .filter(rp -> rp.getPermission().getName().name().equals(perm))
                                // Prérequis « Voir » : un masque sans SELECT ne compte pour rien.
                                .map(rp -> ActionType.applySelectPrerequisite(
                                        rp.getActions() == null ? 0 : rp.getActions()))
                                .reduce(0, (a, b) -> a | b)
                ));

        return permNames.stream()
                .map(name -> new PermissionSummary(
                        name,
                        totalActionsMap.get(name),
                        accruedMap.get(name)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Récupère les informations de l'utilisateur authentifié (endpoint public
     * {@code GET /auth/me}).
     *
     * <p>L'email est tiré du {@code SecurityContext} (peuplé par
     * {@code JwtAuthenticationFilter}). La réponse est un {@code LoginResDto}
     * sans les champs tokens (ils seront {@code null} dans le corps de la
     * réponse). Si aucun utilisateur n'est authentifié, 401 est retourné.
     *
     * <p>NOTE : {@code user.getRoles().getFirst()} est appelé sans contrôle :
     * un rôle absent ferait échouer la construction du DTO.
     */
    @Operation(summary = "Profil de l'utilisateur connecté", description = "Renvoie les informations (rôle, permissions) de l'utilisateur tiré du SecurityContext, sans les tokens. Répond 401 si aucun utilisateur n'est authentifié. Nécessite donc une session valide.")
    @GetMapping("/me")
    public ResponseEntity<LoginResDto> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        var user = userService.findByEmail(email);

        return ResponseEntity.ok(
                new LoginResDto(
                        user.getUsersId(),
                        user.getRoles().getFirst().getName(),
                        user.getRoles().getFirst().getCodeRole(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        buildPermissionSummaries(user)
                )
        );
    }

    /**
     * Vérifie l'état d'un token de récupération sans le consommer
     * (endpoint public {@code GET /auth/recovery/me}).
     *
     * <p>Le token est extrait du cookie {@code recoveryToken} via
     * {@code @CookieValue}. S'il est absent ou invalide/expiré, 401 est
     * retourné. Sinon, l'email associé est renvoyé dans la réponse (permet
     * au front d'afficher l'email et de valider que la session de récupération
     * est toujours ouverte).
     */
    @Operation(summary = "Vérification d'une session de récupération", description = "Contrôle la validité du recovery token sans le consommer et renvoie l'email associé, permettant au front de poursuivre la réinitialisation. Répond 401 si le cookie est absent ou invalide. Endpoint public.")
    @GetMapping("/recovery/me")
    public ResponseEntity<?> recoveryMe(
            @CookieValue(value = "recoveryToken", required = false) String recoveryToken
    ) {

        if (recoveryToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Recovery token missing"));
        }

        if (!jwtService.isRecoveryTokenValid(recoveryToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Recovery token invalid"));
        }

        String email = jwtService.extractUsername(recoveryToken);

        return ResponseEntity.ok(
                Map.of(
                        "email", email,
                        "recovery", true
                )
        );
    }

}