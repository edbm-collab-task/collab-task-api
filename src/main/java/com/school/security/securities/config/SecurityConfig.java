package com.school.security.securities.config;

import com.school.security.services.contracts.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration de la sécurité HTTP de l'application.
 *
 * <p>L'API est purement stateless : aucune session HTTP n'est créée
 * ({@code SessionCreationPolicy.STATELESS}). Le CSRF est désactivé car
 * l'authentification repose exclusivement sur des tokens JWT (portés en cookie
 * HttpOnly ou en header Authorization Bearer). Le CORS est restreint à
 * l'origine du frontend de développement ({@code localhost:5173}).
 *
 * <p>La chaîne de filtres évalue les règles d'autorisation dans l'ordre
 * déclaré : la première règle qui correspond au chemin de la requête est
 * appliquée (first-match-wins). Les chemins marqués {@code permitAll}
 * acceptent les requêtes non authentifiées ; les autres exigent un token
 * JWT valide. Les annotations {@code @PreAuthorize} au niveau des méthodes
 * de contrôleurs s'appliquent toujours, même sur les endpoints
 * {@code permitAll}.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Définit la chaîne de sécurité HTTP.
     *
     * <p>Les endpoints d'authentification, de consultation d'utilisateurs,
     * d'images, de directions, de rôles et de rapports sont en
     * {@code permitAll} car conçus pour être utilisables sans authentification.
     * Tout autre chemin exige un token JWT valide via le {@code JwtAuthenticationFilter}.
     *
     * <p>Le filtre JWT est inséré avant {@code UsernamePasswordAuthenticationFilter}
     * afin de peupler le SecurityContext (authentification) avant que les
     * annotations {@code @PreAuthorize} des contrôleurs ne soient évaluées.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Aucune session HTTP créée : l'authentification est entièrement
                // portée par le JWT transporté en cookie ou header Authorization.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                        .addHeaderWriter((request, response) -> {
                            if (request.getRequestURI().startsWith("/uploads/comments/")) {
                                response.setHeader("X-Frame-Options", "ALLOWALL");
                                response.setHeader("Content-Security-Policy", "frame-ancestors *");
                            }
                        }))
.authorizeHttpRequests(auth -> auth
        // Endpoint WebSocket public (STOMP handshake) : l'authentification
        // n'est pas requise pour l'ouverture initiale de la connexion.
        .requestMatchers("/ws").permitAll()
        // Endpoints d'authentification publics : login, register, create
        // (création par admin), logout, refresh (rotation du refresh token),
        // code (envoi de code de récupération par email).
        .requestMatchers(HttpMethod.POST, "/auth/login","/auth/create", "/auth/register", "/auth/logout", "/auth/refresh", "/auth/code").permitAll()
        // Upload et lecture d'images utilisateur — public pour affichage de
        // profils sans authentification.
        .requestMatchers(HttpMethod.POST,"/users/{id}/image").permitAll()
        .requestMatchers(HttpMethod.GET, "/users", "/users/*", "/users/email","/users/active","/users/disable").permitAll()
        .requestMatchers(HttpMethod.GET, "/users/{id}/image").permitAll()
        // Changement de mot de passe et attribution de rôle — endpoints publics
        // (la logique métier s'applique au niveau du service/contrôleur).
        .requestMatchers(HttpMethod.PUT,"/users/pwd","/users/role").permitAll()
        // Activation/désactivation de compte : réservé aux ADMIN et SUPER_ADMIN
        // au niveau de la filter-chain (authority Spring Security).
        .requestMatchers(HttpMethod.PUT, "/users/account").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
        // Mise à jour du statut de présence (online/offline) — endpoint public
        // utilisé par le login/logout côté client.
        .requestMatchers(HttpMethod.PUT,"/auth/status").permitAll()
        // Profil courant (/me) et vérification de code de récupération — publics.
        .requestMatchers(HttpMethod.GET,"/auth/me","/auth/verification-code","/auth/recovery/me").permitAll()
        // Directions : toutes les opérations (lecture, création, modification,
        // suppression) sont en permitAll. Les contrôles de sécurité réels
        // s'appliquent via @PreAuthorize dans les contrôleurs.
        .requestMatchers(HttpMethod.GET, "/directions", "/directions/{id}").permitAll()
        .requestMatchers(HttpMethod.PUT, "/directions/{id}").permitAll()
        .requestMatchers(HttpMethod.POST, "/directions").permitAll()
        .requestMatchers(HttpMethod.DELETE, "/directions/{id}").permitAll()
        // Téléchargement de pièces jointes de messages — public.
        .requestMatchers(HttpMethod.GET, "/uploads/messages/{filename}").permitAll()
        .requestMatchers(HttpMethod.GET, "/uploads/comments/{filename}").permitAll()
        .requestMatchers(HttpMethod.GET, "/roles", "/roles/permissions").permitAll()
        // Gestion des rôles réservée exclusivement au SUPER_ADMIN.
        .requestMatchers(HttpMethod.POST, "/roles").hasAuthority("SUPER_ADMIN")
        .requestMatchers(HttpMethod.PUT, "/roles/{id}").hasAuthority("SUPER_ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/roles/{id}").hasAuthority("SUPER_ADMIN")
        // Liste des administrateurs : réservée au SUPER_ADMIN.
        .requestMatchers(HttpMethod.GET, "/users/admins").hasAuthority("SUPER_ADMIN")
        // Rapports projet : publics au niveau filter-chain, mais la logique
        // d'accès (super-admin, admin, owner ou contributeur) est vérifiée
        // dans le service ProjectReportServiceImpl.
        .requestMatchers(HttpMethod.GET, "/projects/{projectId}/report").permitAll()
        .requestMatchers(HttpMethod.GET, "/projects/{projectId}/report/pdf").permitAll()
        // Par défaut : toute requête non couverte par les règles ci-dessus
        // exige un token JWT valide (utilisateur authentifié).
        .anyRequest().authenticated())
                // Le filtre JWT est placé AVANT UsernamePasswordAuthenticationFilter
                // afin de peupler le SecurityContext dès réception de la requête,
                // avant que les @PreAuthorize des contrôleurs ne soient évalués.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Fournit le provider d'authentification basé sur la base de données :
     * utilise {@code BCryptPasswordEncoder} pour la vérification des mots de
     * passe et le {@code UserService} comme source des comptes utilisateurs.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService.userDetailsService());
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Configuration CORS globale : seule l'origine du frontend Vite de
     * développement ({@code http://localhost:5173}) est autorisée, avec
     * envoi d'identifiants (cookies). Le header {@code Set-Cookie} est exposé
     * pour permettre au navigateur d'accepter les cookies créés par l'API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "Content-Disposition", "Content-Length", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}