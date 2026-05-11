package com.marcog.peluqueria.ausencias.infrastructure.out.persistence;

import com.marcog.peluqueria.ausencias.domain.model.EstadoAusencia;
import com.marcog.peluqueria.ausencias.domain.model.TipoAusencia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "solicitudes_ausencia")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class AusenciaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID) @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "peluquero_id", columnDefinition = "UUID", nullable = false)
    private UUID peluqueroId;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoAusencia tipo;
    @Column(name = "fecha_inicio", nullable = false) private LocalDate fechaInicio;
    @Column(name = "fecha_fin", nullable = false) private LocalDate fechaFin;
    @Column(columnDefinition = "TEXT") private String motivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default private EstadoAusencia estado = EstadoAusencia.PENDIENTE;
    @CreationTimestamp @Column(name = "solicitada_en", updatable = false) private LocalDateTime solicitadaEn;
    @Column(name = "resuelta_en") private LocalDateTime resueltaEn;
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT") private String motivoRechazo;

    @Column(name = "vista_por_empleado", nullable = false) @Builder.Default private boolean vistaPorEmpleado = true;
}
