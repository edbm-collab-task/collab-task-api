package com.school.security.securities.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utilitaire d'accès à l'utilisateur porté par le {@code SecurityContext}.
 *
 * <p>Classe non instanciable (constructeur privé). Utilisée par les services
 * pour résoudre l'email de l'utilisateur courant.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Retourne le nom (email) de l'utilisateur authentifié.
     *
     * <p>Comportement constaté :
     * <ul>
     *   <li>si l'authentification est absente ou non authentifiée, lève une
     *       {@code IllegalStateException} ("Utilisateur non authentifié.") ;</li>
     *   <li>si le principal est un {@code UserDetails}, retourne son
     *       {@code username} ; sinon retourne {@code principal.toString()} ;</li>
     *   <li>attention : une authentification anonyme est considérée comme
     *       authentifiée par Spring Security, donc ce cas ne déclenche PAS
     *       l'exception et retourne la représentation textuelle du principal
     *       anonyme.</li>
     * </ul>
     */
    public static String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "Utilisateur non authentifié."
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return principal.toString();
    }
}
