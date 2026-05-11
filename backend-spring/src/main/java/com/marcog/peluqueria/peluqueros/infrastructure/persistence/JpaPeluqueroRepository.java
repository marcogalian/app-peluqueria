package com.marcog.peluqueria.peluqueros.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaPeluqueroRepository extends JpaRepository<PeluqueroEntity, UUID> {
    Optional<PeluqueroEntity> findByUserId(UUID userId);
}
