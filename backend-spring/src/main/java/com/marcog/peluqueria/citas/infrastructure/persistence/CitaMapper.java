package com.marcog.peluqueria.citas.infrastructure.persistence;

import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.clientes.infrastructure.persistence.ClienteMapper;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.PeluqueroMapper;
import com.marcog.peluqueria.servicios.infrastructure.persistence.ServicioMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = { PeluqueroMapper.class, ClienteMapper.class, ServicioMapper.class })
public interface CitaMapper {
    Cita toDomain(CitaEntity entity);

    CitaEntity toEntity(Cita domain);
}
