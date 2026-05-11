package com.marcog.peluqueria.ausencias.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dia (o rango) que el admin bloquea para que NINGUN empleado pueda
 * solicitar vacaciones esa fecha. Util para eventos especiales, dias
 * con mucha demanda esperada, etc.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiaBloqueado {
    private UUID id;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivo;
}
