package com.marcog.peluqueria.citas.infrastructure.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaAgendaDto {
    private String id;
    private String horaInicio;
    private Integer duracionMinutos;
    private String estado;

    // Cliente
    private String clienteNombre;
    private String clienteApellidos;
    private Boolean clienteEsVip;
    private Integer clienteDescuentoPorcentaje;

    // Servicio
    private String servicioNombre;

    // Notas
    private String comentarios;
    private String motivoCancelacion;
}
