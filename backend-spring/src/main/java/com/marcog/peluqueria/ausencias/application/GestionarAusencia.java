package com.marcog.peluqueria.ausencias.application;

import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import java.util.List; import java.util.UUID;

public interface GestionarAusencia {
    SolicitudAusencia solicitar(SolicitudAusencia solicitud);
    SolicitudAusencia aprobar(UUID id);
    SolicitudAusencia rechazar(UUID id, String motivo);
    List<SolicitudAusencia> listarTodas();
    List<SolicitudAusencia> listarPorPeluquero(UUID peluqueroId);
    SolicitudAusencia cancelar(UUID id);
    SolicitudAusencia marcarComoVista(UUID id);
}
