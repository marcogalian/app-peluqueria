package com.marcog.peluqueria.citas.infrastructure.out.persistence;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.clientes.infrastructure.out.persistence.ClienteMapper;
import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.PeluqueroMapper;
import com.marcog.peluqueria.servicios.infrastructure.out.persistence.ServicioMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = { PeluqueroMapper.class, ClienteMapper.class, ServicioMapper.class })
public interface CitaMapper {
    Cita toDomain(CitaEntity entity);

    CitaEntity toEntity(Cita domain);
}
