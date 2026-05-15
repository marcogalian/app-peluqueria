package com.marcog.peluqueria.ofertas.infrastructure.persistence;

import com.marcog.peluqueria.ofertas.domain.TipoOferta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "ofertas")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class OfertaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID) @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(name = "descuento_porcentaje") private Integer descuentoPorcentaje;
    @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "fecha_fin") private LocalDate fechaFin;
    @Enumerated(EnumType.STRING) private TipoOferta tipo;
    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "entidad_id", columnDefinition = "UUID") private UUID entidadId;
    @Column(nullable = false) @Builder.Default private boolean activa = true;
}
