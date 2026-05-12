package com.marcog.peluqueria.auditoria.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_actividad")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistroActividadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "rol", nullable = false, length = 40)
    private String rol;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "modulo", nullable = false, length = 80)
    private String modulo;

    @Column(name = "metodo_http", nullable = false, length = 10)
    private String metodoHttp;

    @Column(name = "ruta", nullable = false, length = 500)
    private String ruta;

    @Column(name = "estado_http", nullable = false)
    private Integer estadoHttp;
}
