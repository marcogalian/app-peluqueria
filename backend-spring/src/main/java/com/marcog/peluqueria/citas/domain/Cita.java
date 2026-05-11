package com.marcog.peluqueria.citas.domain;

import com.marcog.peluqueria.clientes.domain.Cliente;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.servicios.domain.Servicio;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Cita {
    private UUID id;
    private Peluquero peluquero;
    private Cliente cliente;
    private LocalDateTime fechaHora;
    private Integer duracionTotal;
    private EstadoCita estado;
    private List<Servicio> servicios;
    private String comentarios;
    private String motivoCancelacion;
}
