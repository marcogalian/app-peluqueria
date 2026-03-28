package com.marcog.peluqueria.finanzas.domain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStats {
    private BigDecimal ingresosTotales;
    private BigDecimal gastosTotales;
    private BigDecimal balance;

    // Gastos agrupados por categoria
    private Map<CategoriaGasto, BigDecimal> desgloseGastos;

    // Ingresos agrupados para las gráficas mensuales o diarias (opcional expandir
    // luengo)
    // Para simplificar, enviaremos la lista reducida o un chart breakdown
}
