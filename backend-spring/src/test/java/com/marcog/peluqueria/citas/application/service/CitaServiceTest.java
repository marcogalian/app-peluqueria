package com.marcog.peluqueria.citas.application.service;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.model.EstadoCita;
import com.marcog.peluqueria.citas.domain.model.exception.CitaSuperpuestaException;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepositoryPort repositoryPort;

    @InjectMocks
    private CitaService citaService;

    private Peluquero peluquero;

    @BeforeEach
    void setUp() {
        peluquero = new Peluquero();
        peluquero.setId(UUID.randomUUID());
        peluquero.setNombre("Juan");
    }

    @Test
    void createCita_Success_NoOverlaps() {
        // Arrange
        Cita newCita = new Cita();
        newCita.setPeluquero(peluquero);
        newCita.setFechaHora(LocalDateTime.of(2026, 3, 15, 10, 0));
        newCita.setDuracionTotal(60);

        when(repositoryPort.findByCriteria(any(), any(), eq(peluquero.getId())))
                .thenReturn(List.of());

        when(repositoryPort.save(any(Cita.class))).thenAnswer(invocation -> {
            Cita saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // Act
        Cita result = citaService.createCita(newCita);

        // Assert
        assertNotNull(result.getId());
        assertEquals(EstadoCita.PENDIENTE, result.getEstado());
        verify(repositoryPort, times(1)).save(newCita);
    }

    @Test
    void createCita_ThrowsException_TotalOverlap() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 10, 0);

        Cita existingCita = new Cita();
        existingCita.setId(UUID.randomUUID());
        existingCita.setPeluquero(peluquero);
        existingCita.setFechaHora(start); // Starts at 10:00
        existingCita.setDuracionTotal(60); // Ends at 11:00
        existingCita.setEstado(EstadoCita.PENDIENTE);

        Cita newCita = new Cita();
        newCita.setPeluquero(peluquero);
        newCita.setFechaHora(start.plusMinutes(15)); // Starts at 10:15
        newCita.setDuracionTotal(30); // Ends at 10:45 (Tries to slide in middle)

        when(repositoryPort.findByCriteria(any(), any(), eq(peluquero.getId())))
                .thenReturn(List.of(existingCita));

        // Act & Assert
        CitaSuperpuestaException exception = assertThrows(CitaSuperpuestaException.class, () -> {
            citaService.createCita(newCita);
        });

        assertTrue(exception.getMessage().contains("Solapamiento de citas"));
        verify(repositoryPort, never()).save(any());
    }

    @Test
    void createCita_Success_BackToBack() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 10, 0);

        Cita existingCita = new Cita();
        existingCita.setId(UUID.randomUUID());
        existingCita.setPeluquero(peluquero);
        existingCita.setFechaHora(start); // Starts at 10:00
        existingCita.setDuracionTotal(60); // Ends at 11:00
        existingCita.setEstado(EstadoCita.PENDIENTE);

        Cita newCita = new Cita();
        newCita.setPeluquero(peluquero);
        newCita.setFechaHora(start.plusMinutes(60)); // Starts exactly at 11:00
        newCita.setDuracionTotal(30);

        when(repositoryPort.findByCriteria(any(), any(), eq(peluquero.getId())))
                .thenReturn(List.of(existingCita));

        when(repositoryPort.save(any(Cita.class))).thenReturn(newCita);

        // Act
        assertDoesNotThrow(() -> {
            citaService.createCita(newCita);
        });

        verify(repositoryPort, times(1)).save(newCita);
    }

    @Test
    void updateCita_Success_SelfOverlapIgnored() {
        // Arrange
        UUID citaId = UUID.randomUUID();
        LocalDateTime originalStart = LocalDateTime.of(2026, 3, 15, 10, 0);

        Cita existingCita = new Cita();
        existingCita.setId(citaId);
        existingCita.setPeluquero(peluquero);
        existingCita.setFechaHora(originalStart);
        existingCita.setDuracionTotal(60);
        existingCita.setEstado(EstadoCita.PENDIENTE);

        Cita updateRequest = new Cita();
        updateRequest.setPeluquero(peluquero);
        updateRequest.setFechaHora(originalStart.plusMinutes(15)); // Mover la hora de inicio ligeramente al horario anterior
        updateRequest.setDuracionTotal(60);
        updateRequest.setEstado(EstadoCita.PENDIENTE);

        when(repositoryPort.findById(citaId)).thenReturn(java.util.Optional.of(existingCita));
        // Simular que el repositorio devolverá el registro existente al buscar
        // por criterios
        when(repositoryPort.findByCriteria(any(), any(), eq(peluquero.getId())))
                .thenReturn(List.of(existingCita));
        when(repositoryPort.save(any())).thenReturn(existingCita);

        // Actuar
        // Esto no debería lanzar CitaSuperpuestaException porque la cita superpuesta
        // tiene exactamente el mismo ID.
        Cita updated = citaService.updateCita(citaId, updateRequest);

        // Verificar
        assertEquals(originalStart.plusMinutes(15), updated.getFechaHora());
        verify(repositoryPort, times(1)).save(existingCita);
    }
}
