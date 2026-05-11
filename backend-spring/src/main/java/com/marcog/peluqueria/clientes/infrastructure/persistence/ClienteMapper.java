package com.marcog.peluqueria.clientes.infrastructure.persistence;

import com.marcog.peluqueria.clientes.domain.Cliente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    Cliente toDomain(ClienteEntity entity);

    ClienteEntity toEntity(Cliente domain);
}
