package com.school.security.config;

import com.school.security.entities.Action;
import com.school.security.entities.Permission;
import com.school.security.enums.ActionType;
import com.school.security.repositories.PermissionRepository;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcule en Java, au démarrage de l'application, le masque dénormalisé
 * {@code permissions.total_actions}.
 *
 * <p>Le code binaire des actions n'est plus stocké en base : il est calculé
 * depuis {@link ActionType} ({@code 1 << bitIndex}). Ce composant remplace donc
 * l'ancien calcul SQL ({@code SUM(a.code_action)} dans {@code data.sql}) : pour
 * chaque permission, on fait le OU bit à bit des codes de ses actions.
 *
 * <p>Un {@link ApplicationRunner} s'exécute après l'initialisation de la source
 * de données ({@code data.sql}), donc après le seed. L'opération est idempotente :
 * on n'écrit que si la valeur calculée diffère de celle en base.
 */
@Component
@AllArgsConstructor
public class PermissionActionMaskInitializer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Permission> permissions = permissionRepository.findAll();
        boolean dirty = false;
        for (Permission permission : permissions) {
            int mask = 0;
            for (Action action : permission.getActions()) {
                mask |= ActionType.valueOf(action.getName()).getCode();
            }
            if (!Objects.equals(permission.getTotalActions(), mask)) {
                permission.setTotalActions(mask);
                dirty = true;
            }
        }
        if (dirty) {
            permissionRepository.saveAll(permissions);
        }
    }
}
