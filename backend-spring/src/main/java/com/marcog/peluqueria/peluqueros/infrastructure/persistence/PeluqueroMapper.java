package com.marcog.peluqueria.peluqueros.infrastructure.persistence;

import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PeluqueroMapper {
    Peluquero toDomain(PeluqueroEntity entity);

    PeluqueroEntity toEntity(Peluquero domain);
}
