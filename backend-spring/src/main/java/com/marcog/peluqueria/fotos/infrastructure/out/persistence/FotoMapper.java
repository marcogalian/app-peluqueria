package com.marcog.peluqueria.fotos.infrastructure.out.persistence;
import com.marcog.peluqueria.fotos.domain.model.FotoCliente;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface FotoMapper {
    FotoCliente toDomain(FotoEntity entity);
    FotoEntity toEntity(FotoCliente domain);
}
