package com.marcog.peluqueria.fotos.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "fotos_clientes")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class FotoEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID) @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "cliente_id", columnDefinition = "UUID", nullable = false)
    private UUID clienteId;

    @Column(name = "ruta_archivo", nullable = false, length = 500) private String rutaArchivo;
    @Column(name = "nombre_original", length = 255) private String nombreOriginal;
    @Column(columnDefinition = "TEXT") private String descripcion;

    @JdbcTypeCode(SqlTypes.UUID) @Column(name = "subida_por_peluquero_id", columnDefinition = "UUID")
    private UUID subidaPorPeluqueroId;

    @CreationTimestamp @Column(name = "creada_en", updatable = false) private LocalDateTime creadaEn;
}
