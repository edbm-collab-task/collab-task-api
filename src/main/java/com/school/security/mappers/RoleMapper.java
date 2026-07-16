package com.school.security.mappers;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.entities.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper implements Mapper<RoleReqDto, Role, RoleResDto> {
    @Override
    public Role fromDto(RoleReqDto d) {
        Role role = new Role();
        role.setName(d.name());
        return role;
    }

    @Override
    public RoleResDto toDto(Role entity) {
        return new RoleResDto(entity.getRolesId(), entity.getName());
    }
}
