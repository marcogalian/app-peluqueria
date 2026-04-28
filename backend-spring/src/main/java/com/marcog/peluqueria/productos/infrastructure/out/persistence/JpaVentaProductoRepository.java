package com.marcog.peluqueria.productos.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaVentaProductoRepository extends JpaRepository<VentaProductoEntity, UUID> {
    List<VentaProductoEntity> findByFechaVentaBetween(LocalDateTime inicio, LocalDateTime fin);
}
