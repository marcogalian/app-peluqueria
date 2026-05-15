package com.marcog.peluqueria.servicios.infrastructure.persistence;

import com.marcog.peluqueria.servicios.domain.CategoriaServicio;
import com.marcog.peluqueria.servicios.domain.TipoGenero;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "servicios")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_descuento", precision = 10, scale = 2)
    private BigDecimal precioDescuento;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoGenero genero;

    @Enumerated(EnumType.STRING)
    @Column
    private CategoriaServicio categoria;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
