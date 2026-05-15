package com.marcog.peluqueria.ausencias.infrastructure.persistence;
import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface AusenciaMapper {
    @Mapping(target = "peluqueroNombre", ignore = true)
    SolicitudAusencia toDomain(AusenciaEntity entity);
    AusenciaEntity toEntity(SolicitudAusencia domain);
}
