package com.marcog.peluqueria.finanzas.domain.port.out;

import com.marcog.peluqueria.finanzas.domain.model.Gasto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GastoRepositoryPort {
    List<Gasto> findAll();

    List<Gasto> findByMesAndAnio(int mes, int anio);

    Optional<Gasto> findById(UUID id);

    Gasto save(Gasto gasto);

    void deleteById(UUID id);
}
