package com.marcog.peluqueria.peluqueros.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaPeluqueroRepository extends JpaRepository<PeluqueroEntity, UUID> {
}
