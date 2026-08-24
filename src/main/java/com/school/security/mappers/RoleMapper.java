package com.school.security.mappers;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.entities.Role;
import com.school.security.enums.PermissionType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper implements Mapper<RoleReqDto, Role, RoleResDto> {
    @Override
    public Role fromDto(RoleReqDto d) {
        Role role = new Role();
        return role;
    }

    @Override
    public RoleResDto toDto(Role entity) {
        List<PermissionType> perms = entity.getPermissions() != null
            ? entity.getPermissions().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList())
            : new ArrayList<>();
        return new RoleResDto(entity.getRolesId(), entity.getName(), perms);
    }
}
