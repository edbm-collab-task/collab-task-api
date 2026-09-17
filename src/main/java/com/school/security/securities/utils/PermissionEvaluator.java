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

@Component("permissionEvaluator")
@AllArgsConstructor
public class PermissionEvaluator {

    private UserRepository userRepository;
    private UserProjectPermissionRepository userProjectPermissionRepository;
    private ProjectRepository projectRepository;

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
