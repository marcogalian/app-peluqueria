package com.marcog.peluqueria.ausencias.domain.port.out;

import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import java.util.List; import java.util.Optional; import java.util.UUID;

public interface AusenciaRepositoryPort {
    SolicitudAusencia guardar(SolicitudAusencia solicitud);
    Optional<SolicitudAusencia> findById(UUID id);
    List<SolicitudAusencia> findAll();
    List<SolicitudAusencia> findByPeluqueroId(UUID peluqueroId);
}
