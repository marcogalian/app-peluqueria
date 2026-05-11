package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.port.out.AusenciaRepositoryPort;
import com.marcog.peluqueria.finanzas.application.service.FinanzasDashboardService;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatFunctionExecutor {

    private final CitaRepositoryPort citaRepository;
    private final AusenciaRepositoryPort ausenciaRepository;
    private final FinanzasDashboardService dashboardService;
    private final ProductoRepositoryPort productoRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public String execute(String functionName, JsonNode args, UUID peluqueroId, boolean isAdmin) {
        try {
            return switch (functionName) {
                case "getCitasEmpleado" -> getCitasEmpleado(args, peluqueroId);
                case "getVacacionesEmpleado" -> getVacacionesEmpleado(peluqueroId);
                case "getGanancias" -> {
                    if (!isAdmin) yield "{\"error\": \"Solo el administrador puede consultar ganancias\"}";
                    yield getGanancias(args);
                }
                case "getCitasAtendidas" -> {
                    if (!isAdmin) yield "{\"error\": \"Solo el administrador puede consultar citas de otros empleados\"}";
                    yield getCitasAtendidas(args);
                }
                case "getProductosStockBajo" -> {
                    if (!isAdmin) yield "{\"error\": \"Solo el administrador puede consultar stock\"}";
                    yield getProductosStockBajo();
                }
                default -> "{\"error\": \"Funcion no reconocida: " + functionName + "\"}";
            };
        } catch (Exception e) {
            log.error("Error ejecutando funcion {}: {}", functionName, e.getMessage());
            return "{\"error\": \"Error al ejecutar la funcion\"}";
        }
    }

    private String getCitasEmpleado(JsonNode args, UUID peluqueroId) {
        String fechaStr = args != null && args.has("fecha") ? args.get("fecha").asText() : LocalDate.now().toString();
        LocalDate fecha = LocalDate.parse(fechaStr);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Cita> citas = citaRepository.findByCriteria(inicio, fin, peluqueroId);
        List<Map<String, String>> resultado = citas.stream().map(c -> Map.of(
                "hora", c.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")),
                "cliente", c.getCliente().getNombre() + " " + c.getCliente().getApellidos(),
                "servicio", c.getServicios() != null && !c.getServicios().isEmpty()
                        ? c.getServicios().get(0).getNombre() : "Sin servicio",
                "estado", c.getEstado().name()
        )).collect(Collectors.toList());

        try {
            return mapper.writeValueAsString(Map.of("fecha", fechaStr, "totalCitas", resultado.size(), "citas", resultado));
        } catch (Exception e) {
            return "{\"error\": \"Error serializando citas\"}";
        }
    }

    private String getVacacionesEmpleado(UUID peluqueroId) {
        List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluqueroId);
        long aprobadas = ausencias.stream().filter(a -> "APROBADA".equals(a.getEstado())).count();
        long pendientes = ausencias.stream().filter(a -> "PENDIENTE".equals(a.getEstado())).count();

        try {
            return mapper.writeValueAsString(Map.of(
                    "totalSolicitudes", ausencias.size(),
                    "aprobadas", aprobadas,
                    "pendientes", pendientes
            ));
        } catch (Exception e) {
            return "{\"error\": \"Error serializando ausencias\"}";
        }
    }

    private String getGanancias(JsonNode args) {
        String periodo = args != null && args.has("periodo") ? args.get("periodo").asText() : "mes";
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();

        if ("semana".equals(periodo)) {
            LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
            mes = inicioSemana.getMonthValue();
            anio = inicioSemana.getYear();
        }

        DashboardStats stats = dashboardService.getStatsByMesAndAnio(mes, anio);
        try {
            return mapper.writeValueAsString(Map.of(
                    "periodo", periodo,
                    "ingresos", stats.getIngresos(),
                    "gastos", stats.getGastos(),
                    "beneficio", stats.getBeneficio(),
                    "totalCitas", stats.getTotalCitas()
            ));
        } catch (Exception e) {
            return "{\"error\": \"Error serializando ganancias\"}";
        }
    }

    private String getCitasAtendidas(JsonNode args) {
        String empleadoId = args != null && args.has("empleadoId") ? args.get("empleadoId").asText() : null;
        String periodo = args != null && args.has("periodo") ? args.get("periodo").asText() : "mes";

        if (empleadoId == null) return "{\"error\": \"empleadoId requerido\"}";

        UUID peluqueroId = UUID.fromString(empleadoId);
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio;
        LocalDateTime fin = hoy.atTime(23, 59, 59);

        inicio = switch (periodo) {
            case "hoy" -> hoy.atStartOfDay();
            case "semana" -> hoy.with(DayOfWeek.MONDAY).atStartOfDay();
            default -> hoy.withDayOfMonth(1).atStartOfDay();
        };

        List<Cita> citas = citaRepository.findByCriteria(inicio, fin, peluqueroId);
        long completadas = citas.stream().filter(c -> "COMPLETADA".equals(c.getEstado().name())).count();

        try {
            return mapper.writeValueAsString(Map.of(
                    "empleadoId", empleadoId,
                    "periodo", periodo,
                    "citasCompletadas", completadas,
                    "citasTotales", citas.size()
            ));
        } catch (Exception e) {
            return "{\"error\": \"Error serializando citas atendidas\"}";
        }
    }

    private String getProductosStockBajo() {
        List<Producto> productos = productoRepository.findAll();
        List<Map<String, Object>> stockBajo = productos.stream()
                .filter(p -> p.getActivo() != null && p.getActivo())
                .filter(p -> p.getStock() != null && p.getStockMinimo() != null && p.getStock() <= p.getStockMinimo())
                .map(p -> Map.<String, Object>of(
                        "nombre", p.getNombre(),
                        "stock", p.getStock(),
                        "stockMinimo", p.getStockMinimo()
                ))
                .collect(Collectors.toList());

        try {
            return mapper.writeValueAsString(Map.of("productosStockBajo", stockBajo, "total", stockBajo.size()));
        } catch (Exception e) {
            return "{\"error\": \"Error serializando productos\"}";
        }
    }
}
