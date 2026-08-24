package com.school.security.securities.utils;

import com.school.security.entities.User;
import com.school.security.enums.PermissionType;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.services.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PermissionUtils {

    private UserRepository userRepository;

    public List<String> getCurrentUserPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return List.of();

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return List.of();

        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName().name())
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean hasPermission(PermissionType permission) {
        return getCurrentUserPermissions().contains(permission.name());
    }

    public boolean hasAnyPermission(PermissionType... permissions) {
        List<String> userPerms = getCurrentUserPermissions();
        for (PermissionType p : permissions) {
            if (userPerms.contains(p.name())) return true;
        }
        return false;
    }
}
