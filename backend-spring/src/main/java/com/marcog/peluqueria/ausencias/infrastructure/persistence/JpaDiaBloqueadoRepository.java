package com.marcog.peluqueria.ausencias.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaDiaBloqueadoRepository extends JpaRepository<DiaBloqueadoEntity, UUID> {

    /**
     * Solapamiento de rangos: dia.fechaInicio <= fin AND dia.fechaFin >= inicio.
     */
    @Query("""
            SELECT d FROM DiaBloqueadoEntity d
            WHERE d.fechaInicio <= :fin
              AND d.fechaFin >= :inicio
            """)
    List<DiaBloqueadoEntity> findSolapados(LocalDate inicio, LocalDate fin);
}
