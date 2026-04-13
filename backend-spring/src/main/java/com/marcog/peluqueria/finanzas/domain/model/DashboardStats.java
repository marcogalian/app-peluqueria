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

    // Ingresos agrupados por día del mes (1..31) para gráficas
    private Map<Integer, BigDecimal> ingresosPorDia;
    private Map<Integer, BigDecimal> gastosPorDia;

    // Stats de empleados
    private List<EmpleadoStat> empleadosStats;
    private List<String> peluquerosActivosAhora;

    // Stats de productos e inventario
    private ProductosStats productosStats;

    // Métricas globales de citas
    private int citasCompletadasMes;
    private BigDecimal ticketMedio;

    @Data
    @Builder
    public static class EmpleadoStat {
        private String nombre;
        private int citasMes;          // citas completadas este mes
        private int vacacionesEsteMes;
        private int vacacionesPendientes;
        private int asuntosPropios;
        private int diasBaja;
    }

    @Data
    @Builder
    public static class ProductosStats {
        // Rankings por género
        private List<ProductoRanking> rankingProductos;  // todos
        private List<ProductoRanking> rankingHombres;
        private List<ProductoRanking> rankingMujeres;
        private BigDecimal gananciasHombres;
        private BigDecimal gananciasMujeres;

        // Legacy — se mantienen por compatibilidad
        private String productoMasVendido;
        private int ventasMasVendido;
        private String productoMenosVendido;
        private int ventasMenosVendido;
        private String productoTopHombres;
        private int ventasTopHombres;
        private String productoTopMujeres;
        private int ventasTopMujeres;

        private List<ProductoStock> pocoStock;
    }

    @Data
    @Builder
    public static class ProductoRanking {
        private String nombre;
        private String categoria;
        private String genero;           // MASCULINO | FEMENINO | UNISEX
        private int consumidos;          // stockMinimo - stock (proxy de unidades vendidas)
        private BigDecimal gananciaEstimada; // consumidos * precio
        private int stock;
        private int stockMinimo;
        private boolean bajoMinimo;
    }

    @Data
    @Builder
    public static class ProductoStock {
        private String nombre;
        private int stock;
        private int stockMinimo;
    }
}
