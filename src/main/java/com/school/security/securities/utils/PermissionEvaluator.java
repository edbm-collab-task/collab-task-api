package com.school.security.securities.utils;

import com.school.security.entities.User;
import com.school.security.enums.PermissionType;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.UserProjectPermissionRepository;
import com.school.security.repositories.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Evaluateur des permissions applicatives invoqué par les annotations
 * {@code @PreAuthorize} des contrôleurs (règles du type
 * {@code @permissionEvaluator.hasProjectPermission(...)}).
 *
 * <p>Deux sources de permissions sont distinguées :
 * <ul>
 *   <li>les permissions globales portées par les rôles de l'utilisateur ;</li>
 *   <li>les permissions accordées sur un projet précis, stockées dans la
 *       table {@code user_project_permissions}.</li>
 * </ul>
 *
 * <p>NOTE : cette classe n'implémente pas l'interface Spring Security
 * {@code HasPermissionEvaluator} : c'est un bean applicatif invoqué
 * directement dans les expressions des contrôleurs (d'où la persistance
 * de requêtes en base à chaque évaluation).
 */
@Component("permissionEvaluator")
@AllArgsConstructor
public class PermissionEvaluator {

    private UserRepository userRepository;
    private UserProjectPermissionRepository userProjectPermissionRepository;
    private ProjectRepository projectRepository;

    /**
     * Vérifie si l'utilisateur authentifié possède une permission GLOBALE,
     * c'est-à-dire portée par au moins un de ses rôles.
     *
     * <p>Seules les permissions de rôles sont évaluées ici : une permission
     * accordée uniquement sur un projet précis (table
     * {@code user_project_permissions}) ne sera pas détectée par cette méthode.
     *
     * @param permission nom de la permission recherchée (ex. "MANAGE_PROJECT")
     * @return {@code true} si l'utilisateur authentifié possède la permission,
     *         sinon {@code false} (y compris si l'utilisateur n'existe pas
     *         en base)
     */
    public boolean hasPermission(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().name().equals(permission));
    }

    /**
     * Vérifie si l'utilisateur authentifié possède une permission sur un
     * projet donné.
     *
     * <p>Deux sources sont consultées dans l'ordre :
     * <ol>
     *   <li>les permissions globales des rôles de l'utilisateur
     *       (si accordée, retour {@code true} immédiat) ;</li>
     *   <li>une permission dédiée au projet dans la table
     *       {@code user_project_permissions}.</li>
     * </ol>
     *
     * <p>La permission par projet n'est consultée que si la permission n'est
     * pas déjà détenue via un rôle. Si le nom de la permission ne correspond
     * à aucune valeur de l'enum {@code PermissionType}, l'accès est refusé
     * (retour {@code false}).
     *
     * @param projectId identifiant du projet concerné
     * @param permission nom de la permission recherchée
     * @return {@code true} si l'utilisateur authentifié a la permission
     *         (rôle ou projet), sinon {@code false}
     */
    public boolean hasProjectPermission(Long projectId, String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        boolean hasRolePermission = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().name().equals(permission));

        if (hasRolePermission) return true;

        // Le propriétaire du projet a toujours tous les droits sur son projet
        boolean isOwner = projectRepository.findById(projectId)
                .map(p -> p.getOwner() != null
                        && user.getUsersId().equals(p.getOwner().getUsersId()))
                .orElse(false);
        if (isOwner) return true;

        try {
            PermissionType permType = PermissionType.valueOf(permission);
            return userProjectPermissionRepository
                    .existsByUserIdAndProjectIdAndPermissionName(user.getUsersId(), projectId, permType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur authentifié possède au moins une permission parmi
     * une liste donnée, en se basant uniquement sur les permissions globales
     * de ses rôles (aucune évaluation par projet).
     *
     * <p>Actuellement non référencée par les contrôleurs : aucune règle
     * {@code @PreAuthorize} n'invoque cette méthode.
     *
     * @param permissions liste des noms de permissions recherchés
     * @return {@code true} si au moins une permission de la liste est détenue
     *         via un rôle, sinon {@code false}
     */
    public boolean hasAnyPermission(String... permissions) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        List<String> userPerms = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName().name())
                .collect(Collectors.toList());

        for (String perm : permissions) {
            if (userPerms.contains(perm)) return true;
        }
        return false;
    }
}
