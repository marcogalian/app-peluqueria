package com.marcog.peluqueria.clientes.infrastructure.out.persistence;

import com.marcog.peluqueria.clientes.domain.model.Genero;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @Column(name = "telefono", length = 20, nullable = false, unique = true)
    private String telefono;

    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "notas_medicas", columnDefinition = "TEXT")
    private String notasMedicas;

    @Column(name = "formulas_tinte", columnDefinition = "TEXT")
    private String formulasTinte;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    // ── Nuevos campos ───────────────────────────────────────
    @Column(name = "es_vip", nullable = false)
    @Builder.Default
    private boolean esVip = false;

    @Column(name = "archivado", nullable = false)
    @Builder.Default
    private boolean archivado = false;

    @Column(name = "descuento_porcentaje")
    private Integer descuentoPorcentaje;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "agregado_por_peluquero_id", columnDefinition = "UUID")
    private UUID agregadoPorPeluqueroId;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "consentimiento_fotos", nullable = false)
    @Builder.Default
    private boolean consentimientoFotos = false;
}
