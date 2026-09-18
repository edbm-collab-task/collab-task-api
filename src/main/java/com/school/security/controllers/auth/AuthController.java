package com.school.security.controllers.auth;


import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.EmailReq;
import com.school.security.dtos.requests.LoginReqDto;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.LoginResDto;
import com.school.security.dtos.responses.UserResDto;
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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



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
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        extractPermissions(user),
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
     *       ne sont PAS supprimés (seule l'exception de la rotation les efface).</li>
     * </ul>
     */
    /**
     * REFRESH TOKEN
     */
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

            var user = userService.findByEmail(email);

            if (user == null) {

                return ResponseEntity.status(401).body(
                        Map.of(
                                "message",
                                "User not found"
                        )
                );
            }

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
                            user.getFirstname(),
                            user.getLastname(),
                            user.getEmail(),
                            extractPermissions(user),
                            newAccessToken,
                            newRefreshToken
                    )
            );

        } catch (Exception e) {

            response.addCookie(CookieUtils.deleteAccessTokenCookie());
            response.addCookie(CookieUtils.deleteRefreshTokenCookie());

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Refresh token expired"
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
    private List<String> extractPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName().name())
                .distinct()
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
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail(),
                        extractPermissions(user)
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