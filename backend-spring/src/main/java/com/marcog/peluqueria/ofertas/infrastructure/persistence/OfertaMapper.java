package com.marcog.peluqueria.ofertas.infrastructure.persistence;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface OfertaMapper {
    Oferta toDomain(OfertaEntity entity);
    OfertaEntity toEntity(Oferta domain);
}
