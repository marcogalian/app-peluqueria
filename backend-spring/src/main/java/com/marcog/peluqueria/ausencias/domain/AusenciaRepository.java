package com.marcog.peluqueria.ausencias.domain;

import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import java.util.List; import java.util.Optional; import java.util.UUID;

public interface AusenciaRepository {
    SolicitudAusencia guardar(SolicitudAusencia solicitud);
    Optional<SolicitudAusencia> findById(UUID id);
    List<SolicitudAusencia> findAll();
    List<SolicitudAusencia> findByPeluqueroId(UUID peluqueroId);
}
