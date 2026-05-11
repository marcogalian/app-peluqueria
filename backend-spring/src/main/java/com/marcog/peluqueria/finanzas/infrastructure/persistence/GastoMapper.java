package com.marcog.peluqueria.finanzas.infrastructure.persistence;

import com.marcog.peluqueria.finanzas.domain.Gasto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GastoMapper {

    Gasto toDomain(GastoEntity entity);

    GastoEntity toEntity(Gasto domain);
}
