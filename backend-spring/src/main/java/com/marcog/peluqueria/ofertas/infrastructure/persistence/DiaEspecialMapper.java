package com.marcog.peluqueria.ofertas.infrastructure.persistence;
import com.marcog.peluqueria.ofertas.domain.DiaEspecial;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface DiaEspecialMapper {
    DiaEspecial toDomain(DiaEspecialEntity entity);
    DiaEspecialEntity toEntity(DiaEspecial domain);
}
