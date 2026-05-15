package com.marcog.peluqueria.finanzas.domain;

import com.marcog.peluqueria.finanzas.domain.Gasto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GastoRepository {
    List<Gasto> findAll();

    List<Gasto> findByMesAndAnio(int mes, int anio);

    Optional<Gasto> findById(UUID id);

    Gasto save(Gasto gasto);

    void deleteById(UUID id);
}
