package com.marcog.peluqueria.productos.infrastructure.persistence;
import com.marcog.peluqueria.productos.domain.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface JpaProductoRepository extends JpaRepository<ProductoEntity, UUID> {
    List<ProductoEntity> findByCategoria(CategoriaProducto categoria);
    List<ProductoEntity> findByActivoTrue();
}
