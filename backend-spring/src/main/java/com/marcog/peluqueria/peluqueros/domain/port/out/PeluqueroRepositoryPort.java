package com.marcog.peluqueria.peluqueros.domain.port.out;

import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeluqueroRepositoryPort {
    List<Peluquero> findAll();

    Optional<Peluquero> findById(UUID id);

    Optional<Peluquero> findByUserId(UUID userId);

    Peluquero save(Peluquero peluquero);

    void deleteById(UUID id);
}
