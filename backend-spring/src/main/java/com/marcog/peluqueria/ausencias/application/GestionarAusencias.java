package com.marcog.peluqueria.ausencias.application;

import com.marcog.peluqueria.ausencias.domain.DiaBloqueado;
import com.marcog.peluqueria.ausencias.domain.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.TipoAusencia;
import com.marcog.peluqueria.ausencias.domain.exception.AntelacionInsuficienteException;
import com.marcog.peluqueria.ausencias.application.GestionarAusencia;
import com.marcog.peluqueria.ausencias.domain.AusenciaRepository;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.shared.notification.Notificaciones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarAusencias implements GestionarAusencia {

    private final AusenciaRepository repository;
    private final PeluqueroRepository peluqueroRepository;
    private final Notificaciones notificaciones;
    private final GestionarDiasBloqueados GestionarDiasBloqueados;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public SolicitudAusencia solicitar(SolicitudAusencia solicitud) {
        validarAntelacion(solicitud);
        validarDiasBloqueados(solicitud);
        solicitud.setEstado(EstadoAusencia.PENDIENTE);
        solicitud.setSolicitadaEn(LocalDateTime.now());
        // El propio empleado crea la solicitud, no necesita notificacion al verla.
        solicitud.setVistaPorEmpleado(true);
        return repository.guardar(solicitud);
    }

    private void validarDiasBloqueados(SolicitudAusencia s) {
        // Las bajas medicas no se bloquean (son inevitables).
        if (s.getTipo() == TipoAusencia.BAJA) return;
        List<DiaBloqueado> solapados = GestionarDiasBloqueados.findSolapados(s.getFechaInicio(), s.getFechaFin());
        if (!solapados.isEmpty()) {
            DiaBloqueado primero = solapados.get(0);
            String motivo = primero.getMotivo() != null && !primero.getMotivo().isBlank()
                    ? primero.getMotivo()
                    : "no se permiten vacaciones en estas fechas";
            throw new IllegalArgumentException(
                    "El periodo solicitado coincide con un dia bloqueado (" + motivo + ").");
        }
    }

    @Override
    public SolicitudAusencia aprobar(UUID id) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setEstado(EstadoAusencia.APROBADA);
        s.setResueltaEn(LocalDateTime.now());
        // Empleado debe ver el card de notificacion la proxima vez que entre a /vacaciones
        s.setVistaPorEmpleado(false);
        SolicitudAusencia guardada = repository.guardar(s);
        notificarPeluquero(s, true, null);
        return guardada;
    }

    @Override
    public SolicitudAusencia rechazar(UUID id, String motivo) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setEstado(EstadoAusencia.RECHAZADA);
        s.setMotivoRechazo(motivo);
        s.setResueltaEn(LocalDateTime.now());
        s.setVistaPorEmpleado(false);
        SolicitudAusencia guardada = repository.guardar(s);
        notificarPeluquero(s, false, motivo);
        return guardada;
    }

    @Override
    public SolicitudAusencia marcarComoVista(UUID id) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setVistaPorEmpleado(true);
        return repository.guardar(s);
    }

    private void notificarPeluquero(SolicitudAusencia s, boolean aprobada, String motivo) {
        peluqueroRepository.findById(s.getPeluqueroId()).ifPresent(peluquero -> {
            String email = peluquero.getUser() != null ? peluquero.getUser().getEmail() : null;
            if (email == null) return;

            String tipo = s.getTipo().name().toLowerCase();
            String periodo = s.getFechaInicio().format(FMT) + " — " + s.getFechaFin().format(FMT);

            if (aprobada) {
                notificaciones.enviarEmail(
                        email,
                        "Tu solicitud de " + tipo + " ha sido aprobada",
                        "Hola " + peluquero.getNombre() + ",\n\n" +
                        "Tu solicitud de " + tipo + " para el periodo " + periodo + " ha sido APROBADA.\n\n" +
                        "Saludos,\nEl equipo de Peluquería Isabella"
                );
            } else {
                notificaciones.enviarEmail(
                        email,
                        "Tu solicitud de " + tipo + " ha sido rechazada",
                        "Hola " + peluquero.getNombre() + ",\n\n" +
                        "Tu solicitud de " + tipo + " para el periodo " + periodo + " ha sido RECHAZADA.\n" +
                        (motivo != null ? "Motivo: " + motivo + "\n" : "") +
                        "\nSi tienes dudas, habla con el administrador.\n\n" +
                        "Saludos,\nEl equipo de Peluquería Isabella"
                );
            }
        });
    }

    @Override
    public SolicitudAusencia cancelar(UUID id) {
        SolicitudAusencia s = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));
        s.setEstado(EstadoAusencia.CANCELADA);
        s.setResueltaEn(LocalDateTime.now());
        return repository.guardar(s);
    }

    private void validarAntelacion(SolicitudAusencia s) {
        if (s.getTipo() == TipoAusencia.BAJA) return;
        int diasMinimos = s.getTipo() == TipoAusencia.VACACIONES ? 7 : 5;
        long diasHasta = LocalDate.now().until(s.getFechaInicio()).getDays();
        if (diasHasta < diasMinimos) {
            throw new AntelacionInsuficienteException(
                "Se requieren al menos " + diasMinimos + " dias de antelacion para " + s.getTipo());
        }
    }

    @Override public List<SolicitudAusencia> listarTodas() { return repository.findAll(); }
    @Override public List<SolicitudAusencia> listarPorPeluquero(UUID peluqueroId) { return repository.findByPeluqueroId(peluqueroId); }
}
