package com.marcog.peluqueria.clientes.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Cliente {
    private UUID id;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String email;
    private String notas;
    private String notasMedicas;
    private String formulasTinte;
    private Genero genero;
    private boolean esVip;
    private Integer descuentoPorcentaje;
    private UUID agregadoPorPeluqueroId;
    private LocalDateTime fechaRegistro;
    private boolean consentimientoFotos;
}
