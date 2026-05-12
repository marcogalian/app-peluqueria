package com.marcog.peluqueria.auditoria.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaRegistroActividadRepository extends JpaRepository<RegistroActividadEntity, UUID> {
    List<RegistroActividadEntity> findTop200ByOrderByFechaHoraDesc();
}
