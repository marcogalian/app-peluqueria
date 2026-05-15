package com.marcog.peluqueria.chatbot.infrastructure.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.ausencias.domain.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.AusenciaRepository;
import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.EstadoCita;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import com.marcog.peluqueria.finanzas.application.ConsultarPanelFinanciero;
import com.marcog.peluqueria.finanzas.domain.ResultadosDTO;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.productos.domain.ProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.servicios.domain.Servicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Tests del executor de funciones del chatbot.
 * Verifica filtrado por rol y conteo correcto de estados (regresion del bug enum vs String).
 */
@ExtendWith(MockitoExtension.class)
class ChatFunctionExecutorTest {

    @Mock private CitaRepository citaRepository;
    @Mock private AusenciaRepository ausenciaRepository;
    @Mock private ConsultarPanelFinanciero dashboardService;
    @Mock private JpaVentaProductoRepository ventaProductoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private PeluqueroRepository peluqueroRepository;

    private ConsultasGestionPeluqueriaPostgres executor;

    @BeforeEach
    void setUp() {
        executor = new ConsultasGestionPeluqueriaPostgres(
                citaRepository, ausenciaRepository, dashboardService,
                ventaProductoRepository, productoRepository, clienteRepository,
                peluqueroRepository,
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
        String resultadoEmpleados = executor.execute("getEmpleados", null, peluqueroId, false);
        String resultadoVacacionesOtros = executor.execute("getVacacionesEmpleadoPorNombre", null, peluqueroId, false);

        assertTrue(resultadoGanancias.contains("Solo el administrador"));
        assertTrue(resultadoStock.contains("Solo el administrador"));
        assertTrue(resultadoVentas.contains("Solo el administrador"));
        assertTrue(resultadoInventario.contains("Solo el administrador"));
        assertTrue(resultadoVip.contains("Solo el administrador"));
        assertTrue(resultadoEmpleados.contains("Solo el administrador"));
        assertTrue(resultadoVacacionesOtros.contains("Solo el administrador"));
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
    @DisplayName("Admin consulta vacaciones por nombre de empleado")
    void getVacacionesEmpleadoPorNombre_devuelveResumen() throws Exception {
        UUID peluqueroId = UUID.randomUUID();
        Peluquero sofia = Peluquero.builder()
                .id(peluqueroId)
                .nombre("Sofía Martínez")
                .build();
        SolicitudAusencia aprobada = SolicitudAusencia.builder()
                .estado(EstadoAusencia.APROBADA)
                .fechaInicio(LocalDate.of(2026, 8, 3))
                .fechaFin(LocalDate.of(2026, 8, 9))
                .build();

        when(peluqueroRepository.findAll()).thenReturn(List.of(sofia));
        when(ausenciaRepository.findByPeluqueroId(peluqueroId)).thenReturn(List.of(aprobada));

        JsonNode args = new ObjectMapper().createObjectNode().put("nombre", "sofia");
        String resultadoJson = executor.execute("getVacacionesEmpleadoPorNombre", args, UUID.randomUUID(), true);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals("Sofía Martínez", resultado.get("empleado").asText());
        assertEquals(1, resultado.get("totalSolicitudes").asInt());
        assertEquals(1, resultado.get("aprobadas").asInt());
        assertEquals("2026-08-03", resultado.get("detalle").get(0).get("fechaInicio").asText());
    }

    @Test
    @DisplayName("getEmpleados devuelve estado operativo del equipo")
    void getEmpleados_devuelveEquipo() throws Exception {
        when(peluqueroRepository.findAll()).thenReturn(List.of(
                Peluquero.builder()
                        .id(UUID.randomUUID())
                        .nombre("Sofía Martínez")
                        .especialidad("Colorimetría")
                        .horarioBase("L-V 09:00-17:00")
                        .disponible(true)
                        .build()
        ));

        String resultadoJson = executor.execute("getEmpleados", null, UUID.randomUUID(), true);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals(1, resultado.get("totalEmpleados").asInt());
        assertEquals(1, resultado.get("disponibles").asInt());
        assertEquals("Sofía Martínez", resultado.get("empleados").get(0).get("nombre").asText());
    }

    @Test
    @DisplayName("getResultados devuelve KPIs para admin")
    void getResultados_adminDevuelveKpis() throws Exception {
        ResultadosDTO resultados = ResultadosDTO.builder()
                .kpis(ResultadosDTO.Kpis.builder()
                        .ingresosPeriodo(1200.0)
                        .ingresosDia(100.0)
                        .ingresosSemana(400.0)
                        .ingresosMes(1200.0)
                        .ingresosAnio(5000.0)
                        .ticketMedio(40.0)
                        .citasCompletadas(30)
                        .tasaCancelacion(5.0)
                        .variacionMes(10.0)
                        .build())
                .topServicios(List.of(ResultadosDTO.TopServicio.builder()
                        .nombre("Corte")
                        .ingresos(600.0)
                        .citas(15)
                        .build()))
                .topEmpleados(List.of(ResultadosDTO.TopEmpleado.builder()
                        .nombre("Sofía Martínez")
                        .citas(12)
                        .ingresos(480.0)
                        .comision(48.0)
                        .build()))
                .build();
        when(dashboardService.getResultados("mes")).thenReturn(resultados);

        JsonNode args = new ObjectMapper().createObjectNode().put("periodo", "mes");
        String resultadoJson = executor.execute("getResultados", args, UUID.randomUUID(), true);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals(1200.0, resultado.get("ingresosPeriodo").asDouble());
        assertEquals("Corte", resultado.get("topServicios").get(0).get("nombre").asText());
        assertEquals("Sofía Martínez", resultado.get("topEmpleados").get(0).get("nombre").asText());
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
    @DisplayName("getCitasProgramadas devuelve citas pendientes ordenadas para admin")
    void getCitasProgramadas_adminDevuelveCitasPendientes() throws Exception {
        Cita cita = Cita.builder()
                .fechaHora(LocalDate.now().with(java.time.DayOfWeek.WEDNESDAY).atTime(10, 30))
                .estado(EstadoCita.PENDIENTE)
                .cliente(Cliente.builder().nombre("Sofia").apellidos("Martinez").build())
                .peluquero(Peluquero.builder().nombre("Laura").build())
                .servicios(List.of(Servicio.builder().nombre("Corte").build()))
                .build();

        when(citaRepository.findByCriteria(any(), any(), isNull()))
                .thenReturn(List.of(cita));

        JsonNode args = new ObjectMapper().createObjectNode().put("periodo", "semana");
        String resultadoJson = executor.execute("getCitasProgramadas", args, UUID.randomUUID(), true);

        JsonNode resultado = new ObjectMapper().readTree(resultadoJson);
        assertEquals(1, resultado.get("totalCitas").asInt());
        assertEquals("Sofia Martinez", resultado.get("citas").get(0).get("cliente").asText());
        assertEquals("Laura", resultado.get("citas").get(0).get("peluquera").asText());
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
