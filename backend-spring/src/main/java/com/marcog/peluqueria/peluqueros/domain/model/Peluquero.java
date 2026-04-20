package com.marcog.peluqueria.peluqueros.domain.model;

import com.marcog.peluqueria.security.domain.model.User;
import lombok.*;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Peluquero {
    private UUID id;
    private User user;
    private String nombre;
    private String especialidad;
    private String horarioBase;
    private boolean disponible;
    private boolean enBaja;
    private boolean enVacaciones;
    private String especialidades;
    private String telefono;
    private String fotoUrl;
}
