package com.marcog.peluqueria.ofertas.infrastructure.out.persistence;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface OfertaMapper {
    Oferta toDomain(OfertaEntity entity);
    OfertaEntity toEntity(Oferta domain);
}
