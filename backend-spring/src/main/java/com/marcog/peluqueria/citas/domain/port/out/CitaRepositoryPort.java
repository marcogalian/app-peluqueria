package com.marcog.peluqueria.citas.domain.port.out;

import com.marcog.peluqueria.citas.domain.model.Cita;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CitaRepositoryPort {
    List<Cita> findAll();

    List<Cita> findByCriteria(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, UUID peluqueroId);

    List<Cita> findByClienteId(UUID clienteId);

    Optional<Cita> findById(UUID id);

    Cita save(Cita cita);

    void deleteById(UUID id);
}
