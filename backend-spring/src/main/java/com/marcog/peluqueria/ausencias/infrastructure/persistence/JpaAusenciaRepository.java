package com.marcog.peluqueria.ausencias.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface JpaAusenciaRepository extends JpaRepository<AusenciaEntity, UUID> {
    List<AusenciaEntity> findByPeluqueroId(UUID peluqueroId);
}
