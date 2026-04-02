package com.marcog.peluqueria.ofertas.domain.model;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Oferta {
    private UUID id;
    private String nombre;
    private String descripcion;
    private Integer descuentoPorcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private TipoOferta tipo;
    private UUID entidadId;
    private boolean activa;
}
