package com.marcog.peluqueria.ausencias.domain.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class SolicitudAusencia {
    private UUID id;
    private UUID peluqueroId;
    private String peluqueroNombre;
    private TipoAusencia tipo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
    private EstadoAusencia estado;
    private LocalDateTime solicitadaEn;
    private LocalDateTime resueltaEn;
    private String motivoRechazo;
}
