package com.marcog.peluqueria.finanzas.application.service;

import com.marcog.peluqueria.ausencias.domain.model.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.model.TipoAusencia;
import com.marcog.peluqueria.ausencias.domain.port.out.AusenciaRepositoryPort;
import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.model.EstadoCita;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.finanzas.domain.model.CategoriaGasto;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.finanzas.domain.model.Gasto;
import com.marcog.peluqueria.finanzas.domain.port.out.GastoRepositoryPort;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.port.out.PeluqueroRepositoryPort;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.VentaProductoEntity;
import com.marcog.peluqueria.servicios.domain.model.Servicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.marcog.peluqueria.finanzas.domain.model.ResultadosDTO;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanzasDashboardService {

    private final GastoRepositoryPort gastoRepository;
    private final CitaRepositoryPort citaRepository;
    private final PeluqueroRepositoryPort peluqueroRepository;
    private final AusenciaRepositoryPort ausenciaRepository;
    private final ProductoRepositoryPort productoRepository;
    private final JpaVentaProductoRepository ventaProductoRepository;

    public DashboardStats getStatsByMesAndAnio(int mes, int anio) {
        // 1. Obtener Gastos
        List<Gasto> gastosDelMes = gastoRepository.findByMesAndAnio(mes, anio);
        BigDecimal totalGastos = gastosDelMes.stream()
                .map(Gasto::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<CategoriaGasto, BigDecimal> desglose = gastosDelMes.stream()
                .collect(Collectors.groupingBy(
                        Gasto::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getImporte, BigDecimal::add)));

        Map<Integer, BigDecimal> gastosPorDia = gastosDelMes.stream()
                .collect(Collectors.groupingBy(
                        gasto -> gasto.getFecha().getDayOfMonth(),
                        Collectors.reducing(BigDecimal.ZERO, Gasto::getImporte, BigDecimal::add)));

        // 2. Obtener Ingresos a través de Citas Completadas
        LocalDateTime inicioMes = LocalDateTime.of(anio, mes, 1, 0, 0);
        LocalDateTime finMes = inicioMes.plusMonths(1).minusNanos(1);

        List<Cita> citasDelMes = citaRepository.findByCriteria(inicioMes, finMes, null);
        List<VentaProductoEntity> ventasProductosDelMes = ventaProductoRepository.findByFechaVentaBetween(inicioMes, finMes);

        BigDecimal ingresosServicios = citasDelMes.stream()
                .filter(cita -> EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .filter(cita -> cita.getServicios() != null)
                .flatMap(cita -> cita.getServicios().stream())
                .map(Servicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ingresosProductos = sumarIngresosVentas(ventasProductosDelMes);
        BigDecimal totalIngresos = ingresosServicios.add(ingresosProductos);

        Map<Integer, BigDecimal> ingresosServiciosPorDia = citasDelMes.stream()
                .filter(cita -> EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .filter(cita -> cita.getServicios() != null)
                .collect(Collectors.groupingBy(
                        cita -> cita.getFechaHora().getDayOfMonth(),
                        Collectors.mapping(
                            cita -> cita.getServicios().stream()
                                .map(Servicio::getPrecio)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                            Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));
        Map<Integer, BigDecimal> ingresosProductosPorDia = ventasProductosDelMes.stream()
                .collect(Collectors.groupingBy(
                        venta -> venta.getFechaVenta().getDayOfMonth(),
                        Collectors.reducing(BigDecimal.ZERO, VentaProductoEntity::getTotal, BigDecimal::add)
                ));
        Map<Integer, BigDecimal> ingresosPorDia = new HashMap<>(ingresosServiciosPorDia);
        ingresosProductosPorDia.forEach((dia, total) ->
                ingresosPorDia.merge(dia, total, BigDecimal::add));

        // 3. Métricas globales de citas
        List<Cita> citasCompletadas = citasDelMes.stream()
                .filter(c -> EstadoCita.COMPLETADO.equals(c.getEstado()))
                .collect(Collectors.toList());

        int totalCitasCompletadas = citasCompletadas.size();
        BigDecimal ticketMedio = totalCitasCompletadas == 0 ? BigDecimal.ZERO
                : totalIngresos.divide(java.math.BigDecimal.valueOf(totalCitasCompletadas), 2, java.math.RoundingMode.HALF_UP);

        // 4. Stats de Empleados
        List<Peluquero> peluqueros = peluqueroRepository.findAll();
        List<DashboardStats.EmpleadoStat> empleadosStats = new ArrayList<>();
        List<String> activosAhora = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (Peluquero peluquero : peluqueros) {
            List<SolicitudAusencia> ausencias = ausenciaRepository.findByPeluqueroId(peluquero.getId());

            int vacEsteMes = 0;
            int vacTotales = 0;
            int asunPropios = 0;
            int diasBaja = 0;

            for (SolicitudAusencia aus : ausencias) {
                if (aus.getEstado() != EstadoAusencia.APROBADA) continue;
                int dias = (int) ChronoUnit.DAYS.between(aus.getFechaInicio(), aus.getFechaFin()) + 1;
                if (aus.getTipo() == TipoAusencia.VACACIONES) {
                    vacTotales += dias;
                    if (aus.getFechaInicio().getMonthValue() == mes && aus.getFechaInicio().getYear() == anio) {
                        vacEsteMes += dias;
                    }
                } else if (aus.getTipo() == TipoAusencia.ASUNTO_PROPIO) {
                    asunPropios += dias;
                } else if (aus.getTipo() == TipoAusencia.BAJA) {
                    diasBaja += dias;
                }
            }

            // Citas completadas del mes por este empleado
            int citasMesEmpleado = (int) citasCompletadas.stream()
                    .filter(c -> c.getPeluquero() != null && c.getPeluquero().getId().equals(peluquero.getId()))
                    .count();

            empleadosStats.add(DashboardStats.EmpleadoStat.builder()
                    .nombre(peluquero.getNombre())
                    .citasMes(citasMesEmpleado)
                    .vacacionesEsteMes(vacEsteMes)
                    .vacacionesPendientes(30 - vacTotales)
                    .asuntosPropios(asunPropios)
                    .diasBaja(diasBaja)
                    .build());

            boolean ocupado = citasDelMes.stream()
                    .filter(c -> c.getPeluquero() != null && c.getPeluquero().getId().equals(peluquero.getId()))
                    .filter(c -> c.getEstado() == EstadoCita.PENDIENTE)
                    .anyMatch(c -> c.getFechaHora().isBefore(ahora) &&
                            c.getFechaHora().plusMinutes(c.getDuracionTotal() != null ? c.getDuracionTotal() : 30).isAfter(ahora));

            if (ocupado) activosAhora.add(peluquero.getNombre());
        }

        // 5. Stats de Productos e Inventario
        List<Producto> todosLosProductos = productoRepository.findAll();
        Map<UUID, List<VentaProductoEntity>> ventasPorProducto = ventasProductosDelMes.stream()
                .collect(Collectors.groupingBy(venta -> venta.getProducto().getId()));

        // Convierte cada producto a su DTO de ranking
        List<DashboardStats.ProductoRanking> rankingGeneral = todosLosProductos.stream()
                .map(p -> {
                    List<VentaProductoEntity> ventasProducto = ventasPorProducto.getOrDefault(p.getId(), List.of());
                    int consumidos = ventasProducto.stream().mapToInt(VentaProductoEntity::getCantidad).sum();
                    BigDecimal ganancia = ventasProducto.stream()
                            .map(VentaProductoEntity::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    String genero = p.getGenero() != null ? p.getGenero().name() : "UNISEX";
                    return DashboardStats.ProductoRanking.builder()
                            .nombre(p.getNombre())
                            .categoria(p.getCategoria() != null ? p.getCategoria().name() : "OTRO")
                            .genero(genero)
                            .consumidos(consumidos)
                            .gananciaEstimada(ganancia)
                            .stock(p.getStock())
                            .stockMinimo(p.getStockMinimo())
                            .bajoMinimo(p.getStock() <= p.getStockMinimo())
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getConsumidos(), a.getConsumidos()))
                .collect(Collectors.toList());

        // Rankings por género (MASCULINO o UNISEX aparece en hombres; FEMENINO o UNISEX en mujeres)
        List<DashboardStats.ProductoRanking> rankingHombres = rankingGeneral.stream()
                .filter(r -> "MASCULINO".equals(r.getGenero()) || "UNISEX".equals(r.getGenero()))
                .collect(Collectors.toList());

        List<DashboardStats.ProductoRanking> rankingMujeres = rankingGeneral.stream()
                .filter(r -> "FEMENINO".equals(r.getGenero()) || "UNISEX".equals(r.getGenero()))
                .collect(Collectors.toList());

        BigDecimal gananciasHombres = rankingHombres.stream()
                .map(DashboardStats.ProductoRanking::getGananciaEstimada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciasMujeres = rankingMujeres.stream()
                .map(DashboardStats.ProductoRanking::getGananciaEstimada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Guardas null antes de comparar: stock o stockMinimo podrian no estar seteados
        // y dispararian un NullPointerException en la comparacion sin auto-unboxing seguro.
        List<DashboardStats.ProductoStock> pocoStock = todosLosProductos.stream()
                .filter(producto -> producto.getStock() != null
                        && producto.getStockMinimo() != null
                        && producto.getStock() <= producto.getStockMinimo())
                .map(producto -> DashboardStats.ProductoStock.builder()
                        .nombre(producto.getNombre())
                        .stock(producto.getStock())
                        .stockMinimo(producto.getStockMinimo())
                        .build())
                .collect(Collectors.toList());

        String masVendido = rankingGeneral.isEmpty() ? "—" : rankingGeneral.get(0).getNombre();
        int ventasMas = rankingGeneral.isEmpty() ? 0 : rankingGeneral.get(0).getConsumidos();
        String menosVendido = rankingGeneral.isEmpty() ? "—" : rankingGeneral.get(rankingGeneral.size() - 1).getNombre();
        int ventasMenos = rankingGeneral.isEmpty() ? 0 : rankingGeneral.get(rankingGeneral.size() - 1).getConsumidos();

        DashboardStats.ProductosStats prodStats = DashboardStats.ProductosStats.builder()
                .rankingProductos(rankingGeneral)
                .rankingHombres(rankingHombres)
                .rankingMujeres(rankingMujeres)
                .gananciasHombres(gananciasHombres)
                .gananciasMujeres(gananciasMujeres)
                .pocoStock(pocoStock)
                .productoMasVendido(masVendido)
                .ventasMasVendido(ventasMas)
                .productoMenosVendido(menosVendido)
                .ventasMenosVendido(ventasMenos)
                .productoTopHombres(rankingHombres.isEmpty() ? "—" : rankingHombres.get(0).getNombre())
                .ventasTopHombres(rankingHombres.isEmpty() ? 0 : rankingHombres.get(0).getConsumidos())
                .productoTopMujeres(rankingMujeres.isEmpty() ? "—" : rankingMujeres.get(0).getNombre())
                .ventasTopMujeres(rankingMujeres.isEmpty() ? 0 : rankingMujeres.get(0).getConsumidos())
                .build();

        // 6. Compilar resultado dashboard
        return DashboardStats.builder()
                .gastosTotales(totalGastos)
                .ingresosTotales(totalIngresos)
                .balance(totalIngresos.subtract(totalGastos))
                .desgloseGastos(desglose)
                .gastosPorDia(gastosPorDia)
                .ingresosPorDia(ingresosPorDia)
                .citasCompletadasMes(totalCitasCompletadas)
                .ticketMedio(ticketMedio)
                .empleadosStats(empleadosStats)
                .peluquerosActivosAhora(activosAhora)
                .productosStats(prodStats)
                .build();
    }

    public ResultadosDTO getResultados(String periodo) {
        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime inicio = switch (periodo) {
            case "semana"    -> ahora.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            case "trimestre" -> ahora.minusMonths(3).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case "anio"      -> LocalDateTime.of(ahora.getYear(), 1, 1, 0, 0);
            default          -> LocalDateTime.of(ahora.getYear(), ahora.getMonthValue(), 1, 0, 0);
        };

        List<Cita> citasPeriodo    = citaRepository.findByCriteria(inicio, ahora, null);
        List<Cita> completadasPeriodo = citasPeriodo.stream()
                .filter(c -> EstadoCita.COMPLETADO.equals(c.getEstado())).collect(Collectors.toList());
        List<Cita> canceladasPeriodo  = citasPeriodo.stream()
                .filter(c -> EstadoCita.CANCELADO.equals(c.getEstado())).collect(Collectors.toList());
        List<VentaProductoEntity> ventasPeriodo = ventaProductoRepository.findByFechaVentaBetween(inicio, ahora);

        double ingresosServiciosPeriodo = sumarIngresos(completadasPeriodo);
        double ingresosPeriodo = ingresosServiciosPeriodo + sumarIngresosVentasDouble(ventasPeriodo);
        int citasCompletadas   = completadasPeriodo.size();
        double ticketMedio     = citasCompletadas > 0 ? ingresosServiciosPeriodo / citasCompletadas : 0;
        double tasaCancelacion = citasPeriodo.isEmpty() ? 0
                : (double) canceladasPeriodo.size() / citasPeriodo.size() * 100;

        // KPIs fijos (día/semana/mes/año) — siempre basados en hoy
        LocalDateTime hoyInicio    = ahora.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime semanaInicio = ahora.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime mesInicio    = LocalDateTime.of(ahora.getYear(), ahora.getMonthValue(), 1, 0, 0);
        LocalDateTime anioInicio   = LocalDateTime.of(ahora.getYear(), 1, 1, 0, 0);

        List<Cita> citasAnio = citaRepository.findByCriteria(anioInicio, ahora, null);
        List<Cita> completadasAnio = citasAnio.stream()
                .filter(c -> EstadoCita.COMPLETADO.equals(c.getEstado())).collect(Collectors.toList());
        List<VentaProductoEntity> ventasAnio = ventaProductoRepository.findByFechaVentaBetween(anioInicio, ahora);

        double ingresosDia    = sumarIngresosDesde(completadasAnio, ventasAnio, hoyInicio);
        double ingresosSemana = sumarIngresosDesde(completadasAnio, ventasAnio, semanaInicio);
        double ingresosMes    = sumarIngresosDesde(completadasAnio, ventasAnio, mesInicio);
        double ingresosAnio   = sumarIngresos(completadasAnio) + sumarIngresosVentasDouble(ventasAnio);

        // Variación vs mes anterior
        LocalDateTime mesAnteriorInicio = mesInicio.minusMonths(1);
        List<Cita> citasMesAnterior = citaRepository.findByCriteria(mesAnteriorInicio, mesInicio.minusNanos(1), null);
        List<VentaProductoEntity> ventasMesAnterior = ventaProductoRepository.findByFechaVentaBetween(mesAnteriorInicio, mesInicio.minusNanos(1));
        double ingresosMesAnterior  = sumarIngresos(citasMesAnterior.stream()
                .filter(c -> EstadoCita.COMPLETADO.equals(c.getEstado())).collect(Collectors.toList()))
                + sumarIngresosVentasDouble(ventasMesAnterior);
        double variacionMes = ingresosMesAnterior == 0 ? 0
                : (ingresosMes - ingresosMesAnterior) / ingresosMesAnterior * 100;

        // Evolución temporal
        ResultadosDTO.Evolucion evolucion = construirEvolucion(completadasPeriodo, ventasPeriodo, periodo, inicio, ahora);

        // Top servicios
        Map<String, double[]> serviciosAcum = new LinkedHashMap<>();
        for (Cita cita : completadasPeriodo) {
            if (cita.getServicios() == null) continue;
            for (Servicio servicio : cita.getServicios()) {
                serviciosAcum.computeIfAbsent(servicio.getNombre(), k -> new double[]{0, 0});
                double[] valores = serviciosAcum.get(servicio.getNombre());
                valores[0] += servicio.getPrecio() != null ? servicio.getPrecio().doubleValue() : 0;
                valores[1]++;
            }
        }
        List<ResultadosDTO.TopServicio> topServicios = serviciosAcum.entrySet().stream()
                .map(e -> ResultadosDTO.TopServicio.builder()
                        .nombre(e.getKey())
                        .ingresos(e.getValue()[0])
                        .citas((int) e.getValue()[1])
                        .build())
                .sorted((a, b) -> Double.compare(b.getIngresos(), a.getIngresos()))
                .limit(5)
                .collect(Collectors.toList());

        // Top empleados
        Map<String, double[]> empleadosAcum = new LinkedHashMap<>();
        for (Cita cita : completadasPeriodo) {
            if (cita.getPeluquero() == null) continue;
            String nombre = cita.getPeluquero().getNombre();
            empleadosAcum.computeIfAbsent(nombre, k -> new double[]{0, 0});
            double[] valores = empleadosAcum.get(nombre);
            valores[0]++;
            if (cita.getServicios() != null) {
                valores[1] += cita.getServicios().stream()
                        .mapToDouble(s -> s.getPrecio() != null ? s.getPrecio().doubleValue() : 0)
                        .sum();
            }
        }
        Map<String, Double> comisionPorNombre = peluqueroRepository.findAll().stream()
                .collect(Collectors.toMap(Peluquero::getNombre, Peluquero::getPorcentajeComision, (a, b) -> a));
        List<ResultadosDTO.TopEmpleado> topEmpleados = empleadosAcum.entrySet().stream()
                .map(e -> {
                    double pct = comisionPorNombre.getOrDefault(e.getKey(), 0.0);
                    return ResultadosDTO.TopEmpleado.builder()
                            .nombre(e.getKey())
                            .citas((int) e.getValue()[0])
                            .ingresos(e.getValue()[1])
                            .comision(e.getValue()[1] * pct / 100)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getCitas(), a.getCitas()))
                .limit(5)
                .collect(Collectors.toList());

        return ResultadosDTO.builder()
                .kpis(ResultadosDTO.Kpis.builder()
                        .ingresosPeriodo(ingresosPeriodo)
                        .ingresosDia(ingresosDia)
                        .ingresosSemana(ingresosSemana)
                        .ingresosMes(ingresosMes)
                        .ingresosAnio(ingresosAnio)
                        .ticketMedio(ticketMedio)
                        .citasCompletadas(citasCompletadas)
                        .tasaCancelacion(tasaCancelacion)
                        .variacionMes(variacionMes)
                        .build())
                .evolucion(evolucion)
                .topServicios(topServicios)
                .topEmpleados(topEmpleados)
                .build();
    }

    private double sumarIngresos(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getServicios() != null)
                .flatMap(c -> c.getServicios().stream())
                .mapToDouble(s -> s.getPrecio() != null ? s.getPrecio().doubleValue() : 0)
                .sum();
    }

    private BigDecimal sumarIngresosVentas(List<VentaProductoEntity> ventas) {
        return ventas.stream()
                .map(VentaProductoEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double sumarIngresosVentasDouble(List<VentaProductoEntity> ventas) {
        return ventas.stream()
                .mapToDouble(venta -> venta.getTotal() != null ? venta.getTotal().doubleValue() : 0)
                .sum();
    }

    private List<VentaProductoEntity> filtrarVentasDesde(List<VentaProductoEntity> ventas, LocalDateTime inicio) {
        return ventas.stream()
                .filter(venta -> !venta.getFechaVenta().isBefore(inicio))
                .collect(Collectors.toList());
    }

    /**
     * Suma ingresos (servicios + productos) desde una fecha hasta ahora.
     * Extraido para evitar repetir el patron 4 veces (dia/semana/mes/anio).
     */
    private double sumarIngresosDesde(List<Cita> completadas,
                                       List<VentaProductoEntity> ventas,
                                       LocalDateTime inicio) {
        List<Cita> citasDesde = completadas.stream()
                .filter(cita -> !cita.getFechaHora().isBefore(inicio))
                .collect(Collectors.toList());
        return sumarIngresos(citasDesde) + sumarIngresosVentasDouble(filtrarVentasDesde(ventas, inicio));
    }

    private ResultadosDTO.Evolucion construirEvolucion(List<Cita> completadas, List<VentaProductoEntity> ventas,
                                                        String periodo,
                                                        LocalDateTime inicio, LocalDateTime fin) {
        List<String> labels = new ArrayList<>();
        List<Double> valores = new ArrayList<>();

        if ("semana".equals(periodo)) {
            for (int dia = 0; dia < 7; dia++) {
                LocalDateTime diaInicio = inicio.plusDays(dia);
                LocalDateTime diaFin    = diaInicio.plusDays(1);
                labels.add(diaInicio.getDayOfWeek().getDisplayName(TextStyle.SHORT, new java.util.Locale("es")));
                double ingresosServicios = sumarIngresos(completadas.stream()
                        .filter(c -> !c.getFechaHora().isBefore(diaInicio) && c.getFechaHora().isBefore(diaFin))
                        .collect(Collectors.toList()));
                double ingresosProductos = sumarIngresosVentasDouble(ventas.stream()
                        .filter(v -> !v.getFechaVenta().isBefore(diaInicio) && v.getFechaVenta().isBefore(diaFin))
                        .collect(Collectors.toList()));
                valores.add(ingresosServicios + ingresosProductos);
            }
        } else if ("mes".equals(periodo)) {
            int diasMes = fin.getMonth().length(fin.toLocalDate().isLeapYear());
            for (int dia = 1; dia <= diasMes; dia++) {
                int diaFinal = dia;
                labels.add(String.valueOf(dia));
                double ingresosServicios = sumarIngresos(completadas.stream()
                        .filter(c -> c.getFechaHora().getDayOfMonth() == diaFinal)
                        .collect(Collectors.toList()));
                double ingresosProductos = sumarIngresosVentasDouble(ventas.stream()
                        .filter(v -> v.getFechaVenta().getDayOfMonth() == diaFinal
                                  && v.getFechaVenta().getMonthValue() == fin.getMonthValue()
                                  && v.getFechaVenta().getYear() == fin.getYear())
                        .collect(Collectors.toList()));
                valores.add(ingresosServicios + ingresosProductos);
            }
        } else if ("trimestre".equals(periodo)) {
            for (int mes = 0; mes < 3; mes++) {
                LocalDateTime mesInicio = inicio.plusMonths(mes);
                LocalDateTime mesFin    = mesInicio.plusMonths(1);
                labels.add(mesInicio.getMonth().getDisplayName(TextStyle.SHORT, new java.util.Locale("es")));
                double ingresosServicios = sumarIngresos(completadas.stream()
                        .filter(c -> !c.getFechaHora().isBefore(mesInicio) && c.getFechaHora().isBefore(mesFin))
                        .collect(Collectors.toList()));
                double ingresosProductos = sumarIngresosVentasDouble(ventas.stream()
                        .filter(v -> !v.getFechaVenta().isBefore(mesInicio) && v.getFechaVenta().isBefore(mesFin))
                        .collect(Collectors.toList()));
                valores.add(ingresosServicios + ingresosProductos);
            }
        } else {
            for (int mes = 1; mes <= 12; mes++) {
                int mesFinal = mes;
                labels.add(LocalDateTime.of(fin.getYear(), mes, 1, 0, 0)
                        .getMonth().getDisplayName(TextStyle.SHORT, new java.util.Locale("es")));
                double ingresosServicios = sumarIngresos(completadas.stream()
                        .filter(c -> c.getFechaHora().getMonthValue() == mesFinal
                                  && c.getFechaHora().getYear() == fin.getYear())
                        .collect(Collectors.toList()));
                double ingresosProductos = sumarIngresosVentasDouble(ventas.stream()
                        .filter(v -> v.getFechaVenta().getMonthValue() == mesFinal
                                  && v.getFechaVenta().getYear() == fin.getYear())
                        .collect(Collectors.toList()));
                valores.add(ingresosServicios + ingresosProductos);
            }
        }

        return ResultadosDTO.Evolucion.builder().labels(labels).valores(valores).build();
    }
}
