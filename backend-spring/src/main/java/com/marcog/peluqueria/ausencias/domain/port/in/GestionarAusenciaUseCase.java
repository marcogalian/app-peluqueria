package com.marcog.peluqueria.ausencias.domain.port.in;

import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import java.util.List; import java.util.UUID;

public interface GestionarAusenciaUseCase {
    SolicitudAusencia solicitar(SolicitudAusencia solicitud);
    SolicitudAusencia aprobar(UUID id);
    SolicitudAusencia rechazar(UUID id, String motivo);
    List<SolicitudAusencia> listarTodas();
    List<SolicitudAusencia> listarPorPeluquero(UUID peluqueroId);
    SolicitudAusencia cancelar(UUID id);
}
