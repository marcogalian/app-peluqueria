package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.ausencias.domain.model.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.port.out.AusenciaRepositoryPort;
import com.marcog.peluqueria.chatbot.domain.port.out.ChatFunctionExecutorPort;
import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.model.EstadoCita;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import com.marcog.peluqueria.finanzas.application.service.FinanzasDashboardService;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.VentaProductoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ejecutor de funciones que el modelo Gemini puede invocar.
 * Cada metodo consulta la BD y devuelve JSON serializado para el modelo.
 *
 * Comprobacion de rol: las funciones solo-admin se filtran aqui como segunda
 * linea de defensa (la primera es ChatbotService.buildToolDeclarations).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatFunctionExecutor implements ChatFunctionExecutorPort {

    // ── Dependencias ────────────────────────────────────────────────
    private final CitaRepositoryPort citaRepository;
    private final AusenciaRepositoryPort ausenciaRepository;
    private final FinanzasDashboardService dashboardService;
    private final JpaVentaProductoRepository ventaProductoRepository;
    private final ProductoRepositoryPort productoRepository;
    private final ClienteRepository clienteRepository;
    // ObjectMapper inyectado de Spring para mantener config consistente con el resto.
    private final ObjectMapper mapper;

    private static final String ERROR_NO_ADMIN_GANANCIAS =
            "{\"error\": \"Solo el administrador puede consultar ganancias\"}";
    private static final String ERROR_NO_ADMIN_CITAS =
            "{\"error\": \"Solo el administrador puede consultar citas de otros empleados\"}";
    private static final String ERROR_NO_ADMIN_STOCK =
            "{\"error\": \"Solo el administrador puede consultar stock\"}";
    private static final String ERROR_NO_ADMIN_VENTAS =
            "{\"error\": \"Solo el administrador puede consultar ventas\"}";
    private static final String ERROR_NO_ADMIN_INVENTARIO =
            "{\"error\": \"Solo el administrador puede consultar inventario\"}";
    private static final String ERROR_NO_ADMIN_CLIENTES =
            "{\"error\": \"Solo el administrador puede consultar clientes\"}";

    // ── Dispatcher publico ──────────────────────────────────────────

    @Override
    public String execute(String functionName, JsonNode args, UUID peluqueroId, boolean isAdmin) {
        try {
            return switch (functionName) {
                case "getCitasEmpleado" -> getCitasEmpleado(args, peluqueroId);
                case "getVacacionesEmpleado" -> getVacacionesEmpleado(peluqueroId);
                case "getGanancias" -> isAdmin ? getGanancias(args) : ERROR_NO_ADMIN_GANANCIAS;
                case "getCitasAtendidas" -> isAdmin ? getCitasAtendidas(args) : ERROR_NO_ADMIN_CITAS;
                case "getProductosStockBajo" -> isAdmin ? getProductosStockBajo() : ERROR_NO_ADMIN_STOCK;
                case "getProductosMasVendidos" -> isAdmin ? getProductosMasVendidos(args) : ERROR_NO_ADMIN_VENTAS;
                case "getInventario" -> isAdmin ? getInventario() : ERROR_NO_ADMIN_INVENTARIO;
                case "getClientesVip" -> isAdmin ? getClientesVip() : ERROR_NO_ADMIN_CLIENTES;
                case "getTotalClientes" -> isAdmin ? getTotalClientes() : ERROR_NO_ADMIN_CLIENTES;
                default -> "{\"error\": \"Funcion no reconocida: " + functionName + "\"}";
            };
        } catch (Exception ex) {
            log.error("Error ejecutando funcion {}: {}", functionName, ex.getMessage());
            return "{\"error\": \"Error al ejecutar la funcion\"}";
        }
    }

    // ── Funciones para todos los roles ──────────────────────────────

    private String getCitasEmpleado(JsonNode args, UUID peluqueroId) {
        String fechaStr = args != null && args.has("fecha")
                ? args.get("fecha").asText()
                : LocalDate.now().toString();
        LocalDate fecha = LocalDate.parse(fechaStr);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Cita> citas = citaRepository.findByCriteria(inicio, fin, peluqueroId);
        List<Map<String, String>> citasResumidas = citas.stream()
                .map(this::resumirCita)
                .collect(Collectors.toList());

        return toJson(Map.of(
                "fecha", fechaStr,
                "totalCitas", citasResumidas.size(),
                "citas", citasResumidas
        ), "Error serializando citas");
    }

    private Map<String, String> resumirCita(Cita cita) {
        return Map.of(
                "hora", cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")),
                "cliente", cita.getCliente().getNombre() + " " + cita.getCliente().getApellidos(),
                "servicio", primerServicioOSinServicio(cita),
                "estado", cita.getEstado().name()
        );
    }

    private String primerServicioOSinServicio(Cita cita) {
        return cita.getServicios() != null && !cita.getServicios().isEmpty()
                ? cita.getServicios().get(0).getNombre()
                : "Sin servicio";
    }

    private String getVacacionesEmpleado(UUID peluqueroId) {
        List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluqueroId);
        // Comparacion contra el enum directo. Antes comparaba String con enum -> siempre 0.
        long aprobadas = contarPorEstado(ausencias, EstadoAusencia.APROBADA);
        long pendientes = contarPorEstado(ausencias, EstadoAusencia.PENDIENTE);

        return toJson(Map.of(
                "totalSolicitudes", ausencias.size(),
                "aprobadas", aprobadas,
                "pendientes", pendientes
        ), "Error serializando ausencias");
    }

    private long contarPorEstado(List<SolicitudAusencia> ausencias, EstadoAusencia estado) {
        return ausencias.stream()
                .filter(ausencia -> estado.equals(ausencia.getEstado()))
                .count();
    }

    // ── Funciones admin ─────────────────────────────────────────────

    private String getGanancias(JsonNode args) {
        String periodoSolicitado = args != null && args.has("periodo")
                ? args.get("periodo").asText()
                : "mes";
        LocalDate hoy = LocalDate.now();

        // El dashboard se calcula a nivel mensual. Devolvemos las cifras del mes
        // y dejamos que el modelo matice si el usuario pidio "hoy" o "semana".
        DashboardStats stats = dashboardService.getStatsByMesAndAnio(hoy.getMonthValue(), hoy.getYear());

        return toJson(Map.of(
                "periodoSolicitado", periodoSolicitado,
                "ingresosMes", stats.getIngresosTotales(),
                "gastosMes", stats.getGastosTotales(),
                "beneficioMes", stats.getBalance(),
                "citasCompletadasMes", stats.getCitasCompletadasMes(),
                "nota", "Cifras del mes en curso. Si piden hoy/semana matizalo en la respuesta."
        ), "Error serializando ganancias");
    }

    private String getCitasAtendidas(JsonNode args) {
        String empleadoIdStr = args != null && args.has("empleadoId") ? args.get("empleadoId").asText() : null;
        String periodo = args != null && args.has("periodo") ? args.get("periodo").asText() : "mes";

        if (empleadoIdStr == null) {
            return "{\"error\": \"empleadoId requerido\"}";
        }

        UUID peluqueroId = UUID.fromString(empleadoIdStr);
        LocalDateTime inicio = calcularInicioPeriodo(periodo);
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);

        List<Cita> citas = citaRepository.findByCriteria(inicio, fin, peluqueroId);
        // Antes comparaba con "COMPLETADA" pero el enum es COMPLETADO -> siempre 0.
        long completadas = citas.stream()
                .filter(cita -> EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .count();

        return toJson(Map.of(
                "empleadoId", empleadoIdStr,
                "periodo", periodo,
                "citasCompletadas", completadas,
                "citasTotales", citas.size()
        ), "Error serializando citas atendidas");
    }

    private LocalDateTime calcularInicioPeriodo(String periodo) {
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case "hoy" -> hoy.atStartOfDay();
            case "semana" -> hoy.with(DayOfWeek.MONDAY).atStartOfDay();
            default -> hoy.withDayOfMonth(1).atStartOfDay();
        };
    }

    private String getProductosStockBajo() {
        LocalDate hoy = LocalDate.now();
        DashboardStats stats = dashboardService.getStatsByMesAndAnio(hoy.getMonthValue(), hoy.getYear());

        List<Map<String, Object>> stockBajo = stats.getProductosStats().getPocoStock().stream()
                .map(producto -> Map.<String, Object>of(
                        "nombre", producto.getNombre(),
                        "stock", producto.getStock(),
                        "stockMinimo", producto.getStockMinimo()
                ))
                .collect(Collectors.toList());

        return toJson(Map.of(
                "productosStockBajo", stockBajo,
                "total", stockBajo.size()
        ), "Error serializando productos");
    }

    private String getProductosMasVendidos(JsonNode args) {
        String periodo = args != null && args.has("periodo") ? args.get("periodo").asText() : "mes";
        LocalDateTime inicio = calcularInicioPeriodo(periodo);
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);

        List<VentaProductoEntity> ventas = ventaProductoRepository.findByFechaVentaBetween(inicio, fin);

        Map<String, Integer> unidadesPorProducto = new HashMap<>();
        for (VentaProductoEntity venta : ventas) {
            int cantidad = venta.getCantidad() != null ? venta.getCantidad() : 0;
            unidadesPorProducto.merge(venta.getProductoNombre(), cantidad, Integer::sum);
        }

        List<Map<String, Object>> ranking = unidadesPorProducto.entrySet().stream()
                .sorted((primero, segundo) -> segundo.getValue() - primero.getValue())
                .limit(10)
                .map(entrada -> Map.<String, Object>of(
                        "producto", entrada.getKey(),
                        "unidadesVendidas", entrada.getValue()))
                .collect(Collectors.toList());

        return toJson(Map.of("periodo", periodo, "ranking", ranking), "Error serializando ranking");
    }

    private String getInventario() {
        List<Producto> productos = productoRepository.findAll().stream()
                .filter(Producto::isActivo)
                .collect(Collectors.toList());

        int totalDistintos = productos.size();
        int totalUnidades = productos.stream()
                .mapToInt(producto -> producto.getStock() != null ? producto.getStock() : 0)
                .sum();

        List<Map<String, Object>> detalle = productos.stream()
                .map(producto -> Map.<String, Object>of(
                        "nombre", producto.getNombre(),
                        "stock", producto.getStock() != null ? producto.getStock() : 0,
                        "stockMinimo", producto.getStockMinimo() != null ? producto.getStockMinimo() : 0
                ))
                .collect(Collectors.toList());

        return toJson(Map.of(
                "totalProductosDistintos", totalDistintos,
                "totalUnidadesEnStock", totalUnidades,
                "detalle", detalle
        ), "Error serializando inventario");
    }

    private String getClientesVip() {
        List<Cliente> clientesVip = clienteRepository.findByFiltros(null, true, false);

        List<Map<String, Object>> detalle = clientesVip.stream()
                .map(cliente -> Map.<String, Object>of(
                        "nombre", cliente.getNombre() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : ""),
                        "telefono", cliente.getTelefono() != null ? cliente.getTelefono() : "",
                        "descuento", cliente.getDescuentoPorcentaje() != null ? cliente.getDescuentoPorcentaje() : 0
                ))
                .collect(Collectors.toList());

        return toJson(Map.of(
                "totalVip", clientesVip.size(),
                "clientes", detalle
        ), "Error serializando VIPs");
    }

    private String getTotalClientes() {
        List<Cliente> activos = clienteRepository.findAllByArchivado(false);
        List<Cliente> archivados = clienteRepository.findAllByArchivado(true);
        long totalVip = activos.stream().filter(Cliente::isEsVip).count();

        return toJson(Map.of(
                "activos", activos.size(),
                "archivados", archivados.size(),
                "vip", totalVip
        ), "Error serializando clientes");
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String toJson(Object payload, String mensajeError) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("Fallo serializando payload del chatbot: {}", ex.getMessage());
            return "{\"error\": \"" + mensajeError + "\"}";
        }
    }
}
