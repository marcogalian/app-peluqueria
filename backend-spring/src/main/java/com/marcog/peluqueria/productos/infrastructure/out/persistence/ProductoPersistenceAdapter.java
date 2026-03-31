package com.marcog.peluqueria.productos.infrastructure.out.persistence;

import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List; import java.util.Optional; import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoRepositoryPort {
    private final JpaProductoRepository repository;
    private final ProductoMapper mapper;

    @Override public Producto guardar(Producto p) { return mapper.toDomain(repository.save(mapper.toEntity(p))); }
    @Override public Optional<Producto> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    @Override public List<Producto> findAll() { return repository.findByActivoTrue().stream().map(mapper::toDomain).collect(Collectors.toList()); }
    @Override public List<Producto> findByCategoria(CategoriaProducto cat) { return repository.findByCategoria(cat).stream().map(mapper::toDomain).collect(Collectors.toList()); }
}
