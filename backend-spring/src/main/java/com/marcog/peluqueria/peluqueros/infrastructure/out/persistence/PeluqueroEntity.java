package com.marcog.peluqueria.peluqueros.infrastructure.out.persistence;

import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "peluqueros")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class PeluqueroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String especialidad;

    @Column(name = "horario_base", length = 100)
    private String horarioBase;

    // ── Nuevos campos ───────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean disponible = true;

    @Column(name = "en_baja", nullable = false)
    @Builder.Default
    private boolean enBaja = false;

    @Column(name = "en_vacaciones", nullable = false)
    @Builder.Default
    private boolean enVacaciones = false;

    @Column(name = "especialidades", columnDefinition = "TEXT")
    private String especialidades;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;
}
