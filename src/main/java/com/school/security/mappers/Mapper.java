package com.school.security.mappers;

public interface Mapper<QD, E, RD> {

    E fromDto(QD d);

    RD toDto(E entity);
}
