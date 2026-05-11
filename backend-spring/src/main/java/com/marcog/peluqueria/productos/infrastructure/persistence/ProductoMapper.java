package com.marcog.peluqueria.productos.infrastructure.persistence;
import com.marcog.peluqueria.productos.domain.Producto;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface ProductoMapper {
    Producto toDomain(ProductoEntity entity);
    ProductoEntity toEntity(Producto domain);
}
