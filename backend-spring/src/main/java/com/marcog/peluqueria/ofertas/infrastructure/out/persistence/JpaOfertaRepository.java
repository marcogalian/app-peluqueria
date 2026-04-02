package com.marcog.peluqueria.ofertas.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaOfertaRepository extends JpaRepository<OfertaEntity, UUID> {
    @Query("SELECT o FROM OfertaEntity o WHERE o.activa = true AND o.fechaInicio <= :hoy AND o.fechaFin >= :hoy")
    List<OfertaEntity> findActivas(@Param("hoy") LocalDate hoy);
}
