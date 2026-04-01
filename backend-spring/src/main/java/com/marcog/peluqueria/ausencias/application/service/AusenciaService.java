package com.marcog.peluqueria.ausencias.application.service;

import com.marcog.peluqueria.ausencias.domain.model.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.model.TipoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.exception.AntelacionInsuficienteException;
import com.marcog.peluqueria.ausencias.domain.port.in.GestionarAusenciaUseCase;
import com.marcog.peluqueria.ausencias.domain.port.out.AusenciaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AusenciaService implements GestionarAusenciaUseCase {

    private final AusenciaRepositoryPort repository;

    @Override
    public SolicitudAusencia solicitar(SolicitudAusencia solicitud) {
        validarAntelacion(solicitud);
        solicitud.setEstado(EstadoAusencia.PENDIENTE);
        solicitud.setSolicitadaEn(LocalDateTime.now());
        return repository.guardar(solicitud);
    }

    private void validarAntelacion(SolicitudAusencia s) {
        if (s.getTipo() == TipoAusencia.BAJA) return; // Sin restriccion
        int diasMinimos = s.getTipo() == TipoAusencia.VACACIONES ? 7 : 5;
        long diasHasta = LocalDate.now().until(s.getFechaInicio()).getDays();
        if (diasHasta < diasMinimos) {
            throw new AntelacionInsuficienteException(
                "Se requieren al menos " + diasMinimos + " dias de antelacion para " + s.getTipo());
        }
    }

    @Override
    public SolicitudAusencia aprobar(UUID id) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setEstado(EstadoAusencia.APROBADA);
        s.setResueltaEn(LocalDateTime.now());
        return repository.guardar(s);
    }

    @Override
    public SolicitudAusencia rechazar(UUID id, String motivo) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setEstado(EstadoAusencia.RECHAZADA);
        s.setMotivoRechazo(motivo);
        s.setResueltaEn(LocalDateTime.now());
        return repository.guardar(s);
    }

    @Override public List<SolicitudAusencia> listarTodas() { return repository.findAll(); }
    @Override public List<SolicitudAusencia> listarPorPeluquero(UUID peluqueroId) { return repository.findByPeluqueroId(peluqueroId); }
}
