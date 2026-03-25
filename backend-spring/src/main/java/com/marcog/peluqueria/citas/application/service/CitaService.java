package com.marcog.peluqueria.citas.application.service;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.model.EstadoCita;
import com.marcog.peluqueria.citas.domain.model.exception.CitaSuperpuestaException;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepositoryPort repositoryPort;

    public List<Cita> getAllCitas() {
        return repositoryPort.findAll();
    }

    public List<Cita> getCitas(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, UUID peluqueroId) {
        if (startDate == null && endDate == null && peluqueroId == null) {
            return getAllCitas();
        }
        return repositoryPort.findByCriteria(startDate, endDate, peluqueroId);
    }

    public List<Cita> getCitasByCliente(UUID clienteId) {
        return repositoryPort.findByClienteId(clienteId);
    }

    public Cita getCitaById(UUID id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita not found"));
    }

    public Cita createCita(Cita cita) {
        validateAvailability(cita);
        cita.setEstado(EstadoCita.PENDIENTE);
        return repositoryPort.save(cita);
    }

    public Cita updateCita(UUID id, Cita citaDetails) {
        Cita existing = getCitaById(id);

        existing.setFechaHora(citaDetails.getFechaHora());
        existing.setDuracionTotal(citaDetails.getDuracionTotal());
        existing.setPeluquero(citaDetails.getPeluquero());

        validateAvailability(existing);

        existing.setEstado(citaDetails.getEstado());
        existing.setCliente(citaDetails.getCliente());
        existing.setServicios(citaDetails.getServicios());

        return repositoryPort.save(existing);
    }

    public void deleteCita(UUID id) {
        repositoryPort.deleteById(id);
    }

    /**
     * Algoritmo de Disponibilidad (HU08)
     * Verifica que el peluquero asignado no tenga otra cita que se solape en
     * tiempo.
     */
    protected void validateAvailability(Cita cita) {
        if (cita.getPeluquero() == null || cita.getPeluquero().getId() == null)
            return;
        if (cita.getFechaHora() == null || cita.getDuracionTotal() == null)
            return;

        UUID peluqueroId = cita.getPeluquero().getId();
        LocalDateTime nuevaStart = cita.getFechaHora();
        LocalDateTime nuevaEnd = nuevaStart.plusMinutes(cita.getDuracionTotal());

        // Optimizamos obteniendo solo las citas del mismo día del peluquero
        LocalDateTime startOfDay = nuevaStart.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Cita> citasDelDia = repositoryPort.findByCriteria(startOfDay, endOfDay, peluqueroId);

        for (Cita existente : citasDelDia) {
            // Ignoramos la misma cita al estar actualizando y las canceladas
            if (existente.getId() != null && existente.getId().equals(cita.getId()))
                continue;
            if (EstadoCita.CANCELADO.equals(existente.getEstado()))
                continue;

            LocalDateTime existenteStart = existente.getFechaHora();
            LocalDateTime existenteEnd = existenteStart.plusMinutes(existente.getDuracionTotal());

            // Lógica principal de solapamiento: (StartA < EndB) y (EndA > StartB)
            if (nuevaStart.isBefore(existenteEnd) && nuevaEnd.isAfter(existenteStart)) {
                throw new CitaSuperpuestaException(
                        String.format(
                                "Solapamiento de citas: El peluquero ya tiene un compromiso agendado entre %s y %s",
                                existenteStart.toLocalTime(), existenteEnd.toLocalTime()));
            }
        }
    }
}
