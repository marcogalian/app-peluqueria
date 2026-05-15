package com.marcog.peluqueria.ausencias.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaDiaBloqueadoRepository extends JpaRepository<DiaBloqueadoEntity, UUID> {

    /**
     * Solapamiento de rangos: fecha de inicio del bloqueo anterior o igual al
     * fin solicitado, y fecha de fin del bloqueo posterior o igual al inicio
     * solicitado.
     */
    @Query("""
            SELECT d FROM DiaBloqueadoEntity d
            WHERE d.fechaInicio <= :fin
              AND d.fechaFin >= :inicio
            """)
    List<DiaBloqueadoEntity> findSolapados(LocalDate inicio, LocalDate fin);
}
