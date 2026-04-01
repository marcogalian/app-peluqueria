package com.marcog.peluqueria.ausencias.infrastructure.out.persistence;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface AusenciaMapper {
    SolicitudAusencia toDomain(AusenciaEntity entity);
    AusenciaEntity toEntity(SolicitudAusencia domain);
}
