package com.school.security.dtos.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Vérifie la distinction entre champ absent, null explicite et identifiant fourni. */
class UserReqDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Test
    void missingDirectionIdShouldStayNull() throws Exception {
        UserReqDto dto = objectMapper.readValue("{}", UserReqDto.class);

        assertNull(dto.directionId());
    }

    @Test
    void explicitNullDirectionIdShouldBeEmpty() throws Exception {
        UserReqDto dto = objectMapper.readValue("{\"directionId\":null}", UserReqDto.class);

        assertEquals(Optional.empty(), dto.directionId());
    }

    @Test
    void numericDirectionIdShouldBePresent() throws Exception {
        UserReqDto dto = objectMapper.readValue("{\"directionId\":3}", UserReqDto.class);

        assertTrue(dto.directionId().isPresent());
        assertEquals(3L, dto.directionId().get());
    }
}
