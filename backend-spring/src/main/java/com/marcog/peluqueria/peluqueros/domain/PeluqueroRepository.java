package com.marcog.peluqueria.peluqueros.domain;

import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeluqueroRepository {
    List<Peluquero> findAll();

    Optional<Peluquero> findById(UUID id);

    Optional<Peluquero> findByUserId(UUID userId);

    Peluquero save(Peluquero peluquero);

    void deleteById(UUID id);
}
