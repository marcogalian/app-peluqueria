package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.ausencias.domain.model.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.port.out.AusenciaRepositoryPort;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import com.marcog.peluqueria.finanzas.application.service.FinanzasDashboardService;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.JpaVentaProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests del executor de funciones del chatbot.
 * Verifica filtrado por rol y conteo correcto de estados (regresion del bug enum vs String).
 */
@ExtendWith(MockitoExtension.class)
class ChatFunctionExecutorTest {

    @Mock private CitaRepositoryPort citaRepository;
    @Mock private AusenciaRepositoryPort ausenciaRepository;
    @Mock private FinanzasDashboardService dashboardService;
    @Mock private JpaVentaProductoRepository ventaProductoRepository;
    @Mock private ProductoRepositoryPort productoRepository;
    @Mock private ClienteRepository clienteRepository;

    private ChatFunctionExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ChatFunctionExecutor(
                citaRepository, ausenciaRepository, dashboardService,
                ventaProductoRepository, productoRepository, clienteRepository,
                new ObjectMapper());
    }

    @Test
    @DisplayName("Funciones admin rechazadas para usuarios sin rol admin")
    void execute_empleadoIntentaFuncionAdmin_devuelveError() {
        UUID peluqueroId = UUID.randomUUID();

        String resultadoGanancias = executor.execute("getGanancias", null, peluqueroId, false);
        String resultadoStock = executor.execute("getProductosStockBajo", null, peluqueroId, false);
        String resultadoVentas = executor.execute("getProductosMasVendidos", null, peluqueroId, false);
        String resultadoInventario = executor.execute("getInventario", null, peluqueroId, false);
        String resultadoVip = executor.execute("getClientesVip", null, peluqueroId, false);

        assertTrue(resultadoGanancias.contains("Solo el administrador"));
        assertTrue(resultadoStock.contains("Solo el administrador"));
        assertTrue(resultadoVentas.contains("Solo el administrador"));
        assertTrue(resultadoInventario.contains("Solo el administrador"));
        assertTrue(resultadoVip.contains("Solo el administrador"));
    }

    @Test
    @DisplayName("Funcion no reconocida devuelve error claro")
    void execute_funcionInexistente_devuelveError() {
        String resultado = executor.execute("getNoExiste", null, UUID.randomUUID(), true);

        assertTrue(resultado.contains("Funcion no reconocida"));
    }

    @Test
    @DisplayName("getVacacionesEmpleado cuenta correctamente por enum (regresion B3)")
    void getVacacionesEmpleado_cuentaPorEnum() throws Exception {
        UUID peluqueroId = UUID.randomUUID();

        SolicitudAusencia aprobada1 = new SolicitudAusencia();
        aprobada1.setEstado(EstadoAusencia.APROBADA);
        SolicitudAusencia aprobada2 = new SolicitudAusencia();
        aprobada2.setEstado(EstadoAusencia.APROBADA);
        SolicitudAusencia pendiente = new SolicitudAusencia();
        pendiente.setEstado(EstadoAusencia.PENDIENTE);

        when(ausenciaRepository.findByPeluqueroId(peluqueroId))
                .thenReturn(List.of(aprobada1, aprobada2, pendiente));

        String resultadoJson = executor.execute("getVacacionesEmpleado", null, peluqueroId, false);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals(3, resultado.get("totalSolicitudes").asInt());
        assertEquals(2, resultado.get("aprobadas").asInt(),
                "Antes comparaba String con enum y siempre daba 0");
        assertEquals(1, resultado.get("pendientes").asInt());
    }

    @Test
    @DisplayName("getClientesVip filtra desde activos sin usar query con nombre nulo")
    void getClientesVip_filtraActivosEnMemoria() throws Exception {
        Cliente vip = Cliente.builder()
                .nombre("Sofia")
                .apellidos("Martinez")
                .telefono("600111222")
                .esVip(true)
                .descuentoPorcentaje(10)
                .build();
        Cliente normal = Cliente.builder()
                .nombre("Laura")
                .apellidos("Garcia")
                .telefono("600333444")
                .esVip(false)
                .build();

        when(clienteRepository.findAllByArchivado(false)).thenReturn(List.of(vip, normal));

        String resultadoJson = executor.execute("getClientesVip", null, UUID.randomUUID(), true);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals(1, resultado.get("totalVip").asInt());
        assertEquals("Sofia Martinez", resultado.get("clientes").get(0).get("nombre").asText());
    }

    @Test
    @DisplayName("Excepciones inesperadas se capturan y devuelven JSON de error")
    void execute_excepcion_devuelveJsonError() {
        when(ausenciaRepository.findByPeluqueroId(any()))
                .thenThrow(new RuntimeException("BD caida"));

        String resultado = executor.execute(
                "getVacacionesEmpleado", null, UUID.randomUUID(), false);

        assertTrue(resultado.contains("error"));
    }
}
