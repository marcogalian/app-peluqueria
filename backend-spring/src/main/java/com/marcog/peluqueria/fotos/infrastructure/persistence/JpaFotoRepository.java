package com.marcog.peluqueria.fotos.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface JpaFotoRepository extends JpaRepository<FotoEntity, UUID> {
    List<FotoEntity> findByClienteId(UUID clienteId);
}
