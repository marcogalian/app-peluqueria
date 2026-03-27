package com.marcog.peluqueria.servicios.infrastructure.out.persistence;

import com.marcog.peluqueria.servicios.domain.model.Servicio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServicioMapper {
    Servicio toDomain(ServicioEntity entity);

    ServicioEntity toEntity(Servicio domain);
}
