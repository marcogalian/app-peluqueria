package com.marcog.peluqueria.citas.infrastructure.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CitaResumenDto {
    private String id;
    private String horaInicio;
    private String clienteNombre;
    private String clienteApellidos;
    private String servicioNombre;
    private String estado;
    private String peluqueroNombre;
}
