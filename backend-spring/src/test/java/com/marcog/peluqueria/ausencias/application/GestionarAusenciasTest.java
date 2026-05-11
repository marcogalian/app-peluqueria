package com.marcog.peluqueria.ausencias.application;

import com.marcog.peluqueria.ausencias.domain.AusenciaRepository;
import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.TipoAusencia;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.shared.notification.Notificaciones;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarAusenciasTest {

    @Test
    void solicitarVacaciones_SerializaSolicitudesConcurrentes() throws Exception {
        AusenciaRepository repository = mock(AusenciaRepository.class);
        PeluqueroRepository peluqueroRepository = mock(PeluqueroRepository.class);
        Notificaciones notificaciones = mock(Notificaciones.class);
        GestionarDiasBloqueados diasBloqueados = mock(GestionarDiasBloqueados.class);

        AtomicInteger operacionesSimultaneas = new AtomicInteger();
        AtomicInteger maximoSimultaneo = new AtomicInteger();

        when(diasBloqueados.findSolapados(any(), any())).thenReturn(List.of());
        when(repository.guardar(any(SolicitudAusencia.class))).thenAnswer(invocation -> {
            int actual = operacionesSimultaneas.incrementAndGet();
            maximoSimultaneo.updateAndGet(maximo -> Math.max(maximo, actual));
            Thread.sleep(120);
            operacionesSimultaneas.decrementAndGet();
            return invocation.getArgument(0);
        });

        GestionarAusencias gestionarAusencias = new GestionarAusencias(
                repository,
                peluqueroRepository,
                notificaciones,
                diasBloqueados
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);

        Future<SolicitudAusencia> primera = executor.submit(() -> {
            salida.await();
            return gestionarAusencias.solicitar(nuevaSolicitudVacaciones());
        });
        Future<SolicitudAusencia> segunda = executor.submit(() -> {
            salida.await();
            return gestionarAusencias.solicitar(nuevaSolicitudVacaciones());
        });

        salida.countDown();
        primera.get(2, TimeUnit.SECONDS);
        segunda.get(2, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(1, maximoSimultaneo.get());
    }

    private SolicitudAusencia nuevaSolicitudVacaciones() {
        SolicitudAusencia solicitud = new SolicitudAusencia();
        solicitud.setPeluqueroId(UUID.randomUUID());
        solicitud.setTipo(TipoAusencia.VACACIONES);
        solicitud.setFechaInicio(LocalDate.now().plusDays(10));
        solicitud.setFechaFin(LocalDate.now().plusDays(12));
        return solicitud;
    }
}
