package com.marcog.peluqueria.ausencias.infrastructure.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dias_bloqueados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaBloqueadoEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(length = 255)
    private String motivo;
}
