package com.school.security.dtos.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.school.security.enums.Gender;
import java.util.Optional;

public record UserReqDto(
        String firstname,
        String lastname,
        String job,
        @JsonDeserialize(using = OptionalDirectionIdDeserializer.class) Optional<Long> directionId,
        String email,
        String number,
        Boolean status,
        String password,
        Gender gender,
        String role) {}
