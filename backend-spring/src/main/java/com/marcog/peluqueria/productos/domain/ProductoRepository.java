package com.marcog.peluqueria.productos.domain;
import com.marcog.peluqueria.productos.domain.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.Producto;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface ProductoRepository {
    Producto guardar(Producto producto);
    Optional<Producto> findById(UUID id);
    List<Producto> findAll();
    List<Producto> findByCategoria(CategoriaProducto categoria);
}
