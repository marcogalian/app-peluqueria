package com.marcog.peluqueria.servicios.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaServicioRepository extends JpaRepository<ServicioEntity, UUID> {
}
