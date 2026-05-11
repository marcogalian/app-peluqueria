package com.marcog.peluqueria.fotos.infrastructure.persistence;
import com.marcog.peluqueria.fotos.domain.FotoCliente;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface FotoMapper {
    FotoCliente toDomain(FotoEntity entity);
    FotoEntity toEntity(FotoCliente domain);
}
