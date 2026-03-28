package com.marcog.peluqueria.finanzas.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaGastoRepository extends JpaRepository<GastoEntity, UUID> {

    @Query("SELECT g FROM GastoEntity g WHERE YEAR(g.fecha) = :anio AND MONTH(g.fecha) = :mes")
    List<GastoEntity> findByAnioAndMes(@Param("anio") int anio, @Param("mes") int mes);
}
