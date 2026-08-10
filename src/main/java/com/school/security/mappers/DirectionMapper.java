package com.school.security.mappers;

import com.school.security.dtos.requests.DirectionReqDto;
import com.school.security.dtos.responses.DirectionResDto;
import com.school.security.entities.Direction;
import org.springframework.stereotype.Component;

@Component
public class DirectionMapper implements Mapper<DirectionReqDto, Direction, DirectionResDto> {

    @Override
    public Direction fromDto(DirectionReqDto d) {
        Direction direction = new Direction();
        direction.setName(d.name());
        return direction;
    }

    @Override
    public DirectionResDto toDto(Direction entity) {
        return new DirectionResDto(entity.getDirectionId(), entity.getName());
    }
}