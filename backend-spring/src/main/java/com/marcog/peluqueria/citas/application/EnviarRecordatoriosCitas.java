package com.marcog.peluqueria.citas.application;

import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.EstadoCita;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import com.marcog.peluqueria.citas.domain.NotificadorCitas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnviarRecordatoriosCitas {

    private final CitaRepository CitaRepository;
    private final List<NotificadorCitas> notificadores;

    /**
     * Se ejecuta cada hora ("0 0 * * * *").
     * Busca las citas pendientes entre 24 y 25 horas a partir de ahora, y envía
     * notificaciones.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sendReminders() {
        log.info("Iniciando tarea programada: Envío de recordatorios (24h)");

        LocalDateTime start = LocalDateTime.now().plusHours(24);
        LocalDateTime end = start.plusHours(1);

        // Obtenemos las citas de mañana a esta misma hora
        List<Cita> citasDeManana = CitaRepository.findByCriteria(start, end, null);

        // Filtramos solo las que estén PENDIENTES
        List<Cita> citasPendientes = citasDeManana.stream()
                .filter(cita -> EstadoCita.PENDIENTE.equals(cita.getEstado()))
                .collect(Collectors.toList());

        if (citasPendientes.isEmpty()) {
            log.info("No hay citas pendientes para enviar recordatorios en el rango {} a {}", start, end);
            return;
        }

        log.info("Se encontraron {} citas para enviar recordatorio.", citasPendientes.size());

        for (Cita cita : citasPendientes) {
            for (NotificadorCitas notificador : notificadores) {
                try {
                    notificador.enviarRecordatorio(cita);
                } catch (Exception e) {
                    log.error("Error al enviar notificación con el puerto {}: {}", notificador.getClass().getSimpleName(),
                            e.getMessage());
                }
            }
        }

        log.info("Tarea programada de recordatorios finalizada con éxito.");
    }
}
