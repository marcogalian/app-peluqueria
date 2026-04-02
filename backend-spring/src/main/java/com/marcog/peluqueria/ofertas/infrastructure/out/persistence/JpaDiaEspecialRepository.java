package com.marcog.peluqueria.ofertas.infrastructure.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface JpaDiaEspecialRepository extends JpaRepository<DiaEspecialEntity, UUID> {}
