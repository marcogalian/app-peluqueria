package com.marcog.peluqueria.finanzas.application.service;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.model.EstadoCita;
import com.marcog.peluqueria.citas.domain.port.out.CitaRepositoryPort;
import com.marcog.peluqueria.finanzas.domain.model.CategoriaGasto;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.finanzas.domain.model.Gasto;
import com.marcog.peluqueria.finanzas.domain.port.out.GastoRepositoryPort;
import com.marcog.peluqueria.servicios.domain.model.Servicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanzasDashboardService {

    private final GastoRepositoryPort gastoRepository;
    private final CitaRepositoryPort citaRepository;

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

        // 2. Obtener Ingresos a través de Citas Completadas
        LocalDateTime inicioMes = LocalDateTime.of(anio, mes, 1, 0, 0);
        LocalDateTime finMes = inicioMes.plusMonths(1).minusNanos(1);

        // Asumiendo que podemos filtrar las citas de todo el mes sin necesidad del
        // peluqueroId
        List<Cita> citasDelMes = citaRepository.findByCriteria(inicioMes, finMes, null);

        BigDecimal totalIngresos = citasDelMes.stream()
                .filter(cita -> EstadoCita.COMPLETADO.equals(cita.getEstado()))
                .filter(cita -> cita.getServicios() != null)
                .flatMap(cita -> cita.getServicios().stream())
                .map(Servicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Compilar los resultados en el DTO (Balance)
        return DashboardStats.builder()
                .gastosTotales(totalGastos)
                .ingresosTotales(totalIngresos)
                .balance(totalIngresos.subtract(totalGastos))
                .desgloseGastos(desglose)
                .build();
    }
}
