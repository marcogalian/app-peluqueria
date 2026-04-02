package com.marcog.peluqueria.ofertas.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class DiaEspecial {
    private UUID id;
    private LocalDate fecha;
    private String nombre;
    private String descripcion;
    private BigDecimal multiplicadorPrecio;
}
