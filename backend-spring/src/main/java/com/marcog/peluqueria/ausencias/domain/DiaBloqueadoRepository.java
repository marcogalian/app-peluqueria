package com.marcog.peluqueria.ausencias.domain;

import com.marcog.peluqueria.ausencias.domain.DiaBloqueado;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiaBloqueadoRepository {

    List<DiaBloqueado> findAll();

    DiaBloqueado save(DiaBloqueado diaBloqueado);

    void deleteById(UUID id);

    /** Devuelve los dias bloqueados que solapan con el rango [inicio, fin]. */
    List<DiaBloqueado> findSolapados(LocalDate inicio, LocalDate fin);
}
