package com.marcog.peluqueria.ausencias.domain.port.out;

import com.marcog.peluqueria.ausencias.domain.model.DiaBloqueado;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiaBloqueadoRepositoryPort {

    List<DiaBloqueado> findAll();

    DiaBloqueado save(DiaBloqueado diaBloqueado);

    void deleteById(UUID id);

    /** Devuelve los dias bloqueados que solapan con el rango [inicio, fin]. */
    List<DiaBloqueado> findSolapados(LocalDate inicio, LocalDate fin);
}
