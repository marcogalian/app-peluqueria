package com.marcog.peluqueria.chat.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensajes_internos")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MensajeInternoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "emisor_user_id", columnDefinition = "UUID", nullable = false)
    private UUID emisorUserId;

    @Column(name = "receptor_user_id", columnDefinition = "UUID", nullable = false)
    private UUID receptorUserId;

    @Column(nullable = false, length = 160, columnDefinition = "varchar(160) default ''")
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @CreationTimestamp
    @Column(name = "enviado_en", nullable = false, updatable = false)
    private LocalDateTime enviadoEn;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean leido = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean archivado = false;
}
