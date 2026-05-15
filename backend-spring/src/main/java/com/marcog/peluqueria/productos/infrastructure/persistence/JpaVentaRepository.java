package com.marcog.peluqueria.productos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaVentaRepository extends JpaRepository<VentaEntity, UUID> {
    long countByNumeroStartingWith(String prefijo);
}
