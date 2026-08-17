package com.school.security.securities.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {

    private SecurityUtils() {
    }

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
