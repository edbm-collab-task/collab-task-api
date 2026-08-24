package com.school.security.services.implementations;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.entities.Permission;
import com.school.security.entities.Role;
import com.school.security.enums.RoleType;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.RoleMapper;
import com.school.security.repositories.PermissionRepository;
import com.school.security.repositories.RoleRepository;
import com.school.security.services.contracts.RoleService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private RoleMapper roleMapper;

    @Override
    public List<RoleResDto> findAll() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResDto findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));
        return roleMapper.toDto(role);
    }

    @Override
    public RoleResDto create(RoleReqDto dto) {
        RoleType roleType = RoleType.valueOf(dto.name());
        if (roleRepository.findByName(roleType).isPresent()) {
            throw new EntityException("Role already exists: " + dto.name());
        }

        Role role = new Role();
        role.setName(roleType);

        if (dto.permissions() != null) {
            List<Permission> permissions = dto.permissions().stream()
                    .map(p -> permissionRepository.findByName(p)
                            .orElseThrow(() -> new EntityException("Permission not found: " + p)))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        }

        return roleMapper.toDto(roleRepository.save(role));
    }

    @Override
    public RoleResDto update(Long id, RoleReqDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));

        if (dto.permissions() != null) {
            List<Permission> permissions = dto.permissions().stream()
                    .map(p -> permissionRepository.findByName(p)
                            .orElseThrow(() -> new EntityException("Permission not found: " + p)))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        }

        return roleMapper.toDto(roleRepository.save(role));
    }

    @Override
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityException("Role not found with ID " + id));
        roleRepository.delete(role);
    }

    @Override
    public List<PermissionResDto> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResDto(p.getPermissionId(), p.getName().name(), p.getDescription()))
                .collect(Collectors.toList());
    }
}
