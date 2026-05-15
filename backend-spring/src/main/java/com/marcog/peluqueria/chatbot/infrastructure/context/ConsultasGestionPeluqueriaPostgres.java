package com.marcog.peluqueria.chatbot.infrastructure.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.ausencias.domain.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.TipoAusencia;
import com.marcog.peluqueria.ausencias.domain.AusenciaRepository;
import com.marcog.peluqueria.chatbot.domain.ConsultasGestionPeluqueria;
import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.EstadoCita;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import com.marcog.peluqueria.finanzas.application.ConsultarPanelFinanciero;
import com.marcog.peluqueria.finanzas.domain.DashboardStats;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.productos.domain.Producto;
import com.marcog.peluqueria.productos.domain.ProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.VentaProductoEntity;
import com.marcog.peluqueria.servicios.domain.Servicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ejecutor de funciones que el proveedor IA puede invocar.
 * Cada metodo consulta la BD y devuelve JSON serializado para el modelo.
 *
 * Comprobacion de rol: las funciones solo-admin se filtran aqui como segunda
 * linea de defensa (la primera es ResponderConsultasGestion.buildToolDeclarations).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultasGestionPeluqueriaPostgres implements ConsultasGestionPeluqueria {
    private static final int DIAS_VACACIONES_ANUALES = 22;

    // ── Dependencias ────────────────────────────────────────────────
    private final CitaRepository citaRepository;
    private final AusenciaRepository ausenciaRepository;
    private final ConsultarPanelFinanciero dashboardService;
    private final JpaVentaProductoRepository ventaProductoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final PeluqueroRepository peluqueroRepository;
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
                case "getCitasProgramadas" -> getCitasProgramadas(args, peluqueroId, isAdmin);
                case "getVacacionesEmpleado" -> getVacacionesEmpleado(peluqueroId);
                case "getPerfilEmpleado" -> getPerfilEmpleado(peluqueroId);
                case "getRendimientoEmpleado" -> getRendimientoEmpleado(args, peluqueroId);
                case "getVacacionesEmpleadoPorNombre" -> isAdmin ? getVacacionesEmpleadoPorNombre(args) : ERROR_NO_ADMIN_CITAS;
                case "getVacacionesEmpleados" -> isAdmin ? getVacacionesEmpleados() : ERROR_NO_ADMIN_CITAS;
                case "getEmpleados" -> isAdmin ? getEmpleados() : ERROR_NO_ADMIN_CITAS;
                case "getGanancias" -> isAdmin ? getGanancias(args) : ERROR_NO_ADMIN_GANANCIAS;
                case "getResultados" -> isAdmin ? getResultados(args) : ERROR_NO_ADMIN_GANANCIAS;
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

    private String getCitasProgramadas(JsonNode args, UUID peluqueroId, boolean isAdmin) {
        String periodo = args != null && args.has("periodo")
                ? args.get("periodo").asText("semana")
                : "semana";
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = switch (periodo) {
            case "hoy" -> hoy.atStartOfDay();
            case "manana" -> hoy.plusDays(1).atStartOfDay();
            case "mes" -> hoy.withDayOfMonth(1).atStartOfDay();
            default -> hoy.with(DayOfWeek.MONDAY).atStartOfDay();
        };
        LocalDateTime fin = switch (periodo) {
            case "hoy" -> hoy.atTime(23, 59, 59);
            case "manana" -> hoy.plusDays(1).atTime(23, 59, 59);
            case "mes" -> hoy.withDayOfMonth(hoy.lengthOfMonth()).atTime(23, 59, 59);
            default -> hoy.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        };

        UUID filtroPeluquero = isAdmin ? null : peluqueroId;
        List<Cita> citas = citaRepository.findByCriteria(inicio, fin, filtroPeluquero).stream()
                .filter(cita -> !EstadoCita.CANCELADO.equals(cita.getEstado()))
                .filter(cita -> !EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .sorted((primera, segunda) -> primera.getFechaHora().compareTo(segunda.getFechaHora()))
                .collect(Collectors.toList());

        List<Map<String, String>> detalle = citas.stream()
                .map(cita -> resumirCitaProgramada(cita, isAdmin))
                .collect(Collectors.toList());

        return toJson(Map.of(
                "periodo", periodo,
                "desde", inicio.toLocalDate().toString(),
                "hasta", fin.toLocalDate().toString(),
                "totalCitas", detalle.size(),
                "citas", detalle
        ), "Error serializando citas programadas");
    }

    private String getEmpleados() {
        List<Map<String, Object>> empleados = peluqueroRepository.findAll().stream()
                .map(peluquero -> Map.<String, Object>of(
                        "id", peluquero.getId() != null ? peluquero.getId().toString() : "",
                        "nombre", peluquero.getNombre() != null ? peluquero.getNombre() : "",
                        "especialidad", peluquero.getEspecialidad() != null ? peluquero.getEspecialidad() : "",
                        "horario", peluquero.getHorarioBase() != null ? peluquero.getHorarioBase() : "",
                        "disponible", peluquero.isDisponible(),
                        "enBaja", peluquero.isEnBaja(),
                        "enVacaciones", peluquero.isEnVacaciones()
                ))
                .collect(Collectors.toList());

        long disponibles = empleados.stream()
                .filter(empleado -> Boolean.TRUE.equals(empleado.get("disponible"))
                        && Boolean.FALSE.equals(empleado.get("enBaja"))
                        && Boolean.FALSE.equals(empleado.get("enVacaciones")))
                .count();

        return toJson(Map.of(
                "totalEmpleados", empleados.size(),
                "disponibles", disponibles,
                "empleados", empleados
        ), "Error serializando empleados");
    }

    private Map<String, String> resumirCitaProgramada(Cita cita, boolean isAdmin) {
        String cliente = cita.getCliente() != null
                ? cita.getCliente().getNombre() + " " + (cita.getCliente().getApellidos() != null ? cita.getCliente().getApellidos() : "")
                : "Cliente sin asignar";
        String peluquera = cita.getPeluquero() != null ? cita.getPeluquero().getNombre() : "";

        if (!isAdmin) {
            return Map.of(
                    "fecha", cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM")),
                    "hora", cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")),
                    "cliente", cliente.trim(),
                    "servicio", primerServicioOSinServicio(cita),
                    "estado", cita.getEstado() != null ? cita.getEstado().name() : ""
            );
        }

        return Map.of(
                "fecha", cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM")),
                "hora", cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")),
                "cliente", cliente.trim(),
                "servicio", primerServicioOSinServicio(cita),
                "peluquera", peluquera,
                "estado", cita.getEstado() != null ? cita.getEstado().name() : ""
        );
    }

    private String primerServicioOSinServicio(Cita cita) {
        return cita.getServicios() != null && !cita.getServicios().isEmpty()
                ? cita.getServicios().get(0).getNombre()
                : "Sin servicio";
    }

    private String getVacacionesEmpleado(UUID peluqueroId) {
        List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluqueroId);
        return toJson(resumirAusencias("empleado autenticado", ausencias), "Error serializando ausencias");
    }

    private String getPerfilEmpleado(UUID peluqueroId) {
        if (peluqueroId == null) {
            return "{\"error\": \"No hay un perfil de empleado asociado a esta cuenta\"}";
        }

        return peluqueroRepository.findById(peluqueroId)
                .map(peluquero -> toJson(Map.of(
                        "nombre", valorSeguro(peluquero.getNombre()),
                        "horario", valorSeguro(peluquero.getHorarioBase()),
                        "porcentajeComision", peluquero.getPorcentajeComision(),
                        "disponible", peluquero.isDisponible(),
                        "enBaja", peluquero.isEnBaja(),
                        "enVacaciones", peluquero.isEnVacaciones()
                ), "Error serializando perfil del empleado"))
                .orElse("{\"error\": \"Empleado no encontrado\"}");
    }

    private String getRendimientoEmpleado(JsonNode args, UUID peluqueroId) {
        if (peluqueroId == null) {
            return "{\"error\": \"No hay un perfil de empleado asociado a esta cuenta\"}";
        }

        RangoFechas rango = resolverRangoEmpleado(args);
        if (rango.error() != null) {
            return "{\"error\": \"" + rango.error() + "\"}";
        }

        Peluquero peluquero = peluqueroRepository.findById(peluqueroId).orElse(null);
        double porcentajeComision = peluquero != null ? peluquero.getPorcentajeComision() : 0.0;

        List<Cita> completadas = citaRepository.findByCriteria(rango.inicio(), rango.fin(), peluqueroId).stream()
                .filter(cita -> EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .sorted((primera, segunda) -> primera.getFechaHora().compareTo(segunda.getFechaHora()))
                .collect(Collectors.toList());

        int totalServicios = completadas.stream()
                .mapToInt(cita -> cita.getServicios() != null ? cita.getServicios().size() : 0)
                .sum();

        BigDecimal ingresos = completadas.stream()
                .filter(cita -> cita.getServicios() != null)
                .flatMap(cita -> cita.getServicios().stream())
                .map(this::precioVentaServicio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal comisionEstimada = ingresos
                .multiply(BigDecimal.valueOf(porcentajeComision))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Map<String, Long> serviciosPorNombre = completadas.stream()
                .filter(cita -> cita.getServicios() != null)
                .flatMap(cita -> cita.getServicios().stream())
                .collect(Collectors.groupingBy(
                        servicio -> valorSeguro(servicio.getNombre()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return toJson(Map.of(
                "periodo", rango.etiqueta(),
                "desde", rango.inicio().toLocalDate().toString(),
                "hasta", rango.fin().toLocalDate().toString(),
                "totalCitasCompletadas", completadas.size(),
                "totalServiciosRealizados", totalServicios,
                "ingresosGenerados", ingresos,
                "porcentajeComision", porcentajeComision,
                "comisionEstimada", comisionEstimada,
                "serviciosPorNombre", serviciosPorNombre
        ), "Error serializando rendimiento del empleado");
    }

    private String getVacacionesEmpleadoPorNombre(JsonNode args) {
        String nombreSolicitado = args != null && args.has("nombre")
                ? args.get("nombre").asText("")
                : "";
        if (nombreSolicitado.isBlank()) {
            return "{\"error\": \"nombre requerido\"}";
        }

        String nombreNormalizado = normalizar(nombreSolicitado);
        return peluqueroRepository.findAll().stream()
                .filter(peluquero -> normalizar(peluquero.getNombre()).contains(nombreNormalizado)
                        || nombreNormalizado.contains(normalizar(peluquero.getNombre())))
                .findFirst()
                .map(peluquero -> {
                    List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluquero.getId());
                    return toJson(resumirAusencias(peluquero.getNombre(), ausencias), "Error serializando ausencias");
                })
                .orElse("{\"error\": \"Empleado no encontrado\"}");
    }

    private String getVacacionesEmpleados() {
        List<Map<String, Object>> empleados = new ArrayList<>();
        for (Peluquero peluquero : peluqueroRepository.findAll()) {
            List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluquero.getId());
            empleados.add(resumirAusencias(peluquero.getNombre(), ausencias));
        }

        long empleadosConSolicitudes = empleados.stream()
                .filter(empleado -> ((Number) empleado.get("totalSolicitudes")).intValue() > 0)
                .count();

        return toJson(Map.of(
                "totalEmpleados", empleados.size(),
                "empleadosConSolicitudes", empleadosConSolicitudes,
                "empleados", empleados
        ), "Error serializando ausencias del equipo");
    }

    private Map<String, Object> resumirAusencias(String nombre, List<SolicitudAusencia> ausencias) {
        // Comparacion contra el enum directo. Antes comparaba String con enum -> siempre 0.
        long aprobadas = contarPorEstado(ausencias, EstadoAusencia.APROBADA);
        long pendientes = contarPorEstado(ausencias, EstadoAusencia.PENDIENTE);
        long rechazadas = contarPorEstado(ausencias, EstadoAusencia.RECHAZADA);
        long canceladas = contarPorEstado(ausencias, EstadoAusencia.CANCELADA);
        long diasVacacionesAprobadas = contarDiasVacaciones(ausencias, EstadoAusencia.APROBADA);
        long diasVacacionesPendientes = contarDiasVacaciones(ausencias, EstadoAusencia.PENDIENTE);
        long diasVacacionesDisponibles = Math.max(0, DIAS_VACACIONES_ANUALES - diasVacacionesAprobadas);

        List<Map<String, Object>> detalle = ausencias.stream()
                .map(this::resumirAusencia)
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("empleado", nombre);
        payload.put("totalSolicitudes", ausencias.size());
        payload.put("aprobadas", aprobadas);
        payload.put("pendientes", pendientes);
        payload.put("rechazadas", rechazadas);
        payload.put("canceladas", canceladas);
        payload.put("diasVacacionesAnuales", DIAS_VACACIONES_ANUALES);
        payload.put("diasVacacionesAprobadas", diasVacacionesAprobadas);
        payload.put("diasVacacionesPendientes", diasVacacionesPendientes);
        payload.put("diasVacacionesDisponibles", diasVacacionesDisponibles);
        payload.put("detalle", detalle);
        return payload;
    }

    private Map<String, Object> resumirAusencia(SolicitudAusencia ausencia) {
        return Map.of(
                "tipo", ausencia.getTipo() != null ? ausencia.getTipo().name() : "",
                "estado", ausencia.getEstado() != null ? ausencia.getEstado().name() : "",
                "fechaInicio", ausencia.getFechaInicio() != null ? ausencia.getFechaInicio().toString() : "",
                "fechaFin", ausencia.getFechaFin() != null ? ausencia.getFechaFin().toString() : "",
                "motivo", ausencia.getMotivo() != null ? ausencia.getMotivo() : ""
        );
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private long contarPorEstado(List<SolicitudAusencia> ausencias, EstadoAusencia estado) {
        return ausencias.stream()
                .filter(ausencia -> estado.equals(ausencia.getEstado()))
                .count();
    }

    private long contarDiasVacaciones(List<SolicitudAusencia> ausencias, EstadoAusencia estado) {
        int anioActual = LocalDate.now().getYear();
        return ausencias.stream()
                .filter(ausencia -> TipoAusencia.VACACIONES.equals(ausencia.getTipo()))
                .filter(ausencia -> estado.equals(ausencia.getEstado()))
                .filter(ausencia -> ausencia.getFechaInicio() != null && ausencia.getFechaFin() != null)
                .filter(ausencia -> ausencia.getFechaInicio().getYear() == anioActual
                        || ausencia.getFechaFin().getYear() == anioActual)
                .mapToLong(ausencia -> ChronoUnit.DAYS.between(ausencia.getFechaInicio(), ausencia.getFechaFin()) + 1)
                .sum();
    }

    private RangoFechas resolverRangoEmpleado(JsonNode args) {
        LocalDate hoy = LocalDate.now();
        if (args != null && args.hasNonNull("fecha")) {
            LocalDate fecha = LocalDate.parse(args.get("fecha").asText());
            if (fecha.isAfter(hoy)) {
                return new RangoFechas(null, null, null, "No puedes consultar servicios realizados en una fecha futura");
            }
            return new RangoFechas(fecha.atStartOfDay(), fecha.atTime(23, 59, 59), fecha.toString(), null);
        }

        String periodo = args != null && args.hasNonNull("periodo")
                ? normalizarPeriodo(args.get("periodo").asText())
                : "semana";

        return switch (periodo) {
            case "hoy" -> new RangoFechas(hoy.atStartOfDay(), hoy.atTime(23, 59, 59), "hoy", null);
            case "ayer" -> {
                LocalDate ayer = hoy.minusDays(1);
                yield new RangoFechas(ayer.atStartOfDay(), ayer.atTime(23, 59, 59), "ayer", null);
            }
            case "semana_pasada" -> {
                LocalDate inicio = hoy.minusWeeks(1).with(DayOfWeek.MONDAY);
                LocalDate fin = inicio.with(DayOfWeek.SUNDAY);
                yield new RangoFechas(inicio.atStartOfDay(), fin.atTime(23, 59, 59), "semana_pasada", null);
            }
            case "mes_pasado" -> {
                LocalDate inicio = hoy.minusMonths(1).withDayOfMonth(1);
                LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
                yield new RangoFechas(inicio.atStartOfDay(), fin.atTime(23, 59, 59), "mes_pasado", null);
            }
            case "mes" -> {
                LocalDate inicio = hoy.withDayOfMonth(1);
                yield new RangoFechas(inicio.atStartOfDay(), hoy.atTime(23, 59, 59), "mes", null);
            }
            default -> {
                LocalDate inicio = hoy.with(DayOfWeek.MONDAY);
                yield new RangoFechas(inicio.atStartOfDay(), hoy.atTime(23, 59, 59), "semana", null);
            }
        };
    }

    private String normalizarPeriodo(String periodo) {
        return normalizar(periodo).replace(' ', '_');
    }

    private BigDecimal precioVentaServicio(Servicio servicio) {
        if (servicio == null) return BigDecimal.ZERO;
        if (servicio.getPrecioDescuento() != null && servicio.getPrecioDescuento().compareTo(BigDecimal.ZERO) > 0) {
            return servicio.getPrecioDescuento();
        }
        return servicio.getPrecio() != null ? servicio.getPrecio() : BigDecimal.ZERO;
    }

    private String valorSeguro(String valor) {
        return valor != null ? valor : "";
    }

    private record RangoFechas(LocalDateTime inicio, LocalDateTime fin, String etiqueta, String error) {}
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

    private String getResultados(JsonNode args) {
        String periodo = args != null && args.has("periodo")
                ? args.get("periodo").asText("mes")
                : "mes";
        var resultados = dashboardService.getResultados(periodo);

        List<Map<String, Object>> topServicios = resultados.getTopServicios().stream()
                .map(servicio -> Map.<String, Object>of(
                        "nombre", servicio.getNombre(),
                        "ingresos", servicio.getIngresos(),
                        "citas", servicio.getCitas()
                ))
                .collect(Collectors.toList());

        List<Map<String, Object>> topEmpleados = resultados.getTopEmpleados().stream()
                .map(empleado -> Map.<String, Object>of(
                        "nombre", empleado.getNombre(),
                        "citas", empleado.getCitas(),
                        "ingresos", empleado.getIngresos(),
                        "comision", empleado.getComision()
                ))
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("periodo", periodo);
        payload.put("ingresosPeriodo", resultados.getKpis().getIngresosPeriodo());
        payload.put("ingresosDia", resultados.getKpis().getIngresosDia());
        payload.put("ingresosSemana", resultados.getKpis().getIngresosSemana());
        payload.put("ingresosMes", resultados.getKpis().getIngresosMes());
        payload.put("ingresosAnio", resultados.getKpis().getIngresosAnio());
        payload.put("ticketMedio", resultados.getKpis().getTicketMedio());
        payload.put("citasCompletadas", resultados.getKpis().getCitasCompletadas());
        payload.put("tasaCancelacion", resultados.getKpis().getTasaCancelacion());
        payload.put("variacionMes", resultados.getKpis().getVariacionMes());
        payload.put("topServicios", topServicios);
        payload.put("topEmpleados", topEmpleados);
        return toJson(payload, "Error serializando resultados");
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
        List<Cliente> clientesVip = clienteRepository.findAllByArchivado(false).stream()
                .filter(Cliente::isEsVip)
                .collect(Collectors.toList());

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
