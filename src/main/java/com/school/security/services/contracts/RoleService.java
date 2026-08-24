package com.school.security.services.contracts;

import com.school.security.dtos.requests.RoleReqDto;
import com.school.security.dtos.responses.PermissionResDto;
import com.school.security.dtos.responses.RoleResDto;
import java.util.List;

public interface RoleService {
    List<RoleResDto> findAll();
    RoleResDto findById(Long id);
    RoleResDto create(RoleReqDto dto);
    RoleResDto update(Long id, RoleReqDto dto);
    void delete(Long id);
    List<PermissionResDto> findAllPermissions();
}
