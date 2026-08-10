package com.school.security.services.contracts;


import com.school.security.dtos.requests.DirectionReqDto;
import com.school.security.dtos.responses.DirectionResDto;
import com.school.security.entities.Direction;

public interface DirectionService extends Service<DirectionReqDto, DirectionResDto, Long> {
    Direction findByName(String email);

    public DirectionResDto save(DirectionReqDto toSave, Long id);
}