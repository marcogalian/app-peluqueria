package com.marcog.peluqueria.servicios.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaServicioRepository extends JpaRepository<ServicioEntity, UUID> {
    Optional<ServicioEntity> findByNombreIgnoreCase(String nombre);
}
