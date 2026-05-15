package com.marcog.peluqueria.citas.infrastructure.persistence;

import com.marcog.peluqueria.citas.domain.EstadoCita;
import com.marcog.peluqueria.clientes.infrastructure.persistence.ClienteEntity;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.PeluqueroEntity;
import com.marcog.peluqueria.servicios.infrastructure.persistence.ServicioEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "citas")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class CitaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peluquero_id", nullable = false)
    private PeluqueroEntity peluquero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "duracion_total", nullable = false)
    private Integer duracionTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCita estado;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cita_servicio",
        joinColumns = @JoinColumn(name = "cita_id"),
        inverseJoinColumns = @JoinColumn(name = "servicio_id"))
    private List<ServicioEntity> servicios;

    // ── Nuevos campos ───────────────────────────────────────
    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;
}
