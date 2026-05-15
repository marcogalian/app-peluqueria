package com.marcog.peluqueria.finanzas.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Gasto {
    private UUID id;
    private String concepto;
    private BigDecimal importe;
    private LocalDate fecha;
    private CategoriaGasto categoria;
}
