package com.school.security.mappers;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper implements Mapper<RoleReqDto, Role, RoleResDto> {
    @Override
    public Role fromDto(RoleReqDto d) {
        Role role = new Role();
        role.setName(d.name());
        role.setCodeRole(d.codeRole());
        return role;
    }

    @Override
    public RoleResDto toDto(Role entity) {
        List<PermissionResDto> perms = entity.getPermissions() != null
            ? entity.getPermissions().stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList())
            : new ArrayList<>();
        return new RoleResDto(entity.getRolesId(), entity.getName(), entity.getCodeRole(), perms);
    }

    /** Convertit une permission en DTO de réponse, catégorie comprise. */
    private PermissionResDto toPermissionDto(Permission p) {
        return new PermissionResDto(
                p.getPermissionId(),
                p.getName().name(),
                p.getDescription(),
                p.getCategoryPermission());
    }
}
