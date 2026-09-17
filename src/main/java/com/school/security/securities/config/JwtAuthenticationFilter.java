package com.school.security.securities.config;

import com.school.security.securities.services.JwtService;
import com.school.security.services.contracts.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre d'authentification exécuté pour CHAQUE requête HTTP
 * ({@code OncePerRequestFilter}), avant le traitement Spring Security.
 *
 * <p>Il récupère le token JWT dans l'ordre de priorité suivant :
 * <ol>
 *   <li>le header {@code Authorization: Bearer <token>} ;</li>
 *   <li>à défaut (header absent ou ne commençant pas par {@code "Bearer "}),
 *       le cookie {@code accessToken}.</li>
 * </ol>
 *
 * <p>Si un token valide est trouvé, l'utilisateur est chargé en base
 * (via {@code UserService}) et l'authentification est injectée dans le
 * {@code SecurityContextHolder}, ce qui rend l'utilisateur authentifié pour
 * les annotations {@code @PreAuthorize} des contrôleurs. Si le token est
 * absent ou invalide, la requête continue simplement sans authentification
 * (les endpoints {@code permitAll} restent accessibles ; les autres sont
 * refusés par la filter-chain de Spring Security).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Traitement d'authentification par token JWT.
     *
     * <p>Le token est d'abord recherché dans le header
     * {@code Authorization: Bearer}, puis en repli dans le cookie
     * {@code accessToken}. Un token vide ou absent laisse la chaîne de
     * filtres se poursuivre sans authentification.
     *
     * <p>Si le token est exploitable : extraction de l'email, chargement des
     * {@code UserDetails}, validation du token, puis injection de
     * l'authentification dans le {@code SecurityContext}. Toute exception
     * (token invalide, expiré, utilisateur inexistant...) est absorbée :
     * le contexte est vidé et la requête continue sans authentification.
     *
     * <p>IMPORTANT : du fait de la priorité du header sur le cookie, une
     * requête portant un header {@code Authorization} présent mais
     * non-Bearer fera quand même peser la tentative sur le cookie
     * {@code accessToken}.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = null;
        String authHeader = request.getHeader("Authorization");
        // Le header Authorization Bearer a la priorité sur le cookie accessToken.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            jwt = getCookie(request, "accessToken");
        }

        // Aucun token (ou token vide) : pas d'authentification, on continue
        // la chaîne de filtres. La requête arrivera non authentifiée.
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String email = jwtService.extractUsername(jwt);

            // On ne charge l'utilisateur que si l'email est présent et qu'aucune
            // authentification n'est déjà établie dans le contexte.
            if (email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userService.userDetailsService().loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // Injection de l'authentification dans le SecurityContext :
                    // c'est ce qui rend la requête "authentifiée".
                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            // Token invalide ou expiré : on laisse Spring Security gérer
            // Toute erreur (token invalide/expiré, utilisateur introuvable,
            // email mal formé...) fait tomber la requête dans l'accès non
            // authentifié : le contexte est vidé, aucune exception ne remonte.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lire un cookie par son nom.
     */
    private String getCookie(HttpServletRequest request, String name) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}