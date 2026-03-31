package com.marcog.peluqueria.productos.domain.port.out;
import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface ProductoRepositoryPort {
    Producto guardar(Producto producto);
    Optional<Producto> findById(UUID id);
    List<Producto> findAll();
    List<Producto> findByCategoria(CategoriaProducto categoria);
}
