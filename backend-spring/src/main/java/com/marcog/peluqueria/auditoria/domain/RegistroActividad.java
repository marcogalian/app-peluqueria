package com.marcog.peluqueria.auditoria.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistroActividad {
    private UUID id;
    private LocalDateTime fechaHora;
    private String usuario;
    private String rol;
    private String accion;
    private String modulo;
    private String metodoHttp;
    private String ruta;
    private Integer estadoHttp;
}
