package com.marcog.peluqueria.productos.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVentasProductosDTO {
    @Builder.Default
    private BigDecimal ingresosSemana = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ingresosMes = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ingresosAnio = BigDecimal.ZERO;
    @Builder.Default
    private int unidadesSemana = 0;
    @Builder.Default
    private int unidadesMes = 0;
    @Builder.Default
    private int unidadesAnio = 0;
}
