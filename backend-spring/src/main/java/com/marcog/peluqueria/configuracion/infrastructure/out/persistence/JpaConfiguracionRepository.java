package com.marcog.peluqueria.configuracion.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConfiguracionRepository extends JpaRepository<ConfiguracionEntity, Long> {
}
