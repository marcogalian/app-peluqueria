package com.marcog.peluqueria.ofertas.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "dias_especiales")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class DiaEspecialEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID) @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(name = "multiplicador_precio", nullable = false, precision = 4, scale = 2) private BigDecimal multiplicadorPrecio;
}
