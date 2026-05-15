package com.marcog.peluqueria.citas.application;

import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.EstadoCita;
import com.marcog.peluqueria.citas.domain.exception.CitaSuperpuestaException;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso principal para gestionar la agenda del salon.
 *
 * Centraliza altas, modificaciones, cancelaciones y finalizacion de citas.
 * Tambien protege la regla de negocio que impide solapar citas activas del
 * mismo empleado en el mismo tramo horario.
 */
@Service
@RequiredArgsConstructor
public class GestionarAgenda {

    private final CitaRepository repository;
    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<Cita> getAllCitas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cita> getCitas(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, UUID peluqueroId) {
        if (startDate == null && endDate == null && peluqueroId == null) {
            return getAllCitas();
        }
        return repository.findByCriteria(startDate, endDate, peluqueroId);
    }

    @Transactional(readOnly = true)
    public List<Cita> getCitasByCliente(UUID clienteId) {
        return repository.findByClienteId(clienteId);
    }

    @Transactional(readOnly = true)
    public Cita getCitaById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Cita no encontrada"));
    }

    public Cita createCita(Cita cita) {
        validateAvailability(cita);
        cita.setEstado(EstadoCita.PENDIENTE);
        return repository.save(cita);
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
        existing.setComentarios(citaDetails.getComentarios());
        existing.setMotivoCancelacion(citaDetails.getMotivoCancelacion());

        return repository.save(existing);
    }

    public Cita cancelar(UUID id, String motivo) {
        Cita cita = getCitaById(id);
        cita.setEstado(EstadoCita.CANCELADO);
        cita.setMotivoCancelacion(motivo);

        // Solo penalizar si la cita aun no ha ocurrido. Sin esta guarda, cancelar
        // una cita pasada anadiria una penalizacion injustificada al cliente.
        LocalDateTime ahora = LocalDateTime.now();
        boolean citaEnFuturo = ahora.isBefore(cita.getFechaHora());
        boolean cancelacionTardia = citaEnFuturo
                && ahora.isAfter(cita.getFechaHora().minusHours(24));
        if (cancelacionTardia && cita.getCliente() != null) {
            clienteRepository.findById(cita.getCliente().getId()).ifPresent(cliente -> {
                String penalizacion = String.format("[Penalización %s] Canceló con menos de 24h de antelación.",
                        LocalDateTime.now().toLocalDate());
                String notasActuales = cliente.getNotas() != null ? cliente.getNotas() : "";
                cliente.setNotas(notasActuales.isEmpty() ? penalizacion : notasActuales + "\n" + penalizacion);
                clienteRepository.save(cliente);
            });
        }

        return repository.save(cita);
    }

    public void deleteCita(UUID id) {
        repository.deleteById(id);
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

        List<Cita> citasDelDia = repository.findByCriteria(startOfDay, endOfDay, peluqueroId);

        for (Cita existente : citasDelDia) {
            // Ignoramos la misma cita al estar actualizando y las canceladas
            if (existente.getId() != null && existente.getId().equals(cita.getId()))
                continue;
            if (EstadoCita.CANCELADO.equals(existente.getEstado()))
                continue;

            LocalDateTime existenteStart = existente.getFechaHora();
            // Guarda contra duracionTotal null en datos antiguos para evitar NPE.
            int duracionExistente = existente.getDuracionTotal() != null
                    ? existente.getDuracionTotal()
                    : 30;
            LocalDateTime existenteEnd = existenteStart.plusMinutes(duracionExistente);

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
