package com.school.security.dtos.responses;

import com.school.security.enums.RoleType;

public record LoginResDto( RoleType role,String firstname, String lastname, String email) {}
