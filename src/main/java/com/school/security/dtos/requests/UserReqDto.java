package com.school.security.dtos.requests;

import com.school.security.enums.Gender;

public record UserReqDto(
        String firstname,
        String lastname,
        String job,
        Long directionId,
        String email,
        String number,
        Boolean status,
        String password,
        Gender gender) {}
