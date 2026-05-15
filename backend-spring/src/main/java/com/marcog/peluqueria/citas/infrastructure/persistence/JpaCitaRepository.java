package com.marcog.peluqueria.citas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaCitaRepository extends JpaRepository<CitaEntity, UUID> {
    List<CitaEntity> findByFechaHoraBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<CitaEntity> findByFechaHoraBetweenAndPeluqueroId(LocalDateTime startDate, LocalDateTime endDate,
            UUID peluqueroId);

    List<CitaEntity> findByClienteIdOrderByFechaHoraDesc(UUID clienteId);
}
