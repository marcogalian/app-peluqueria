package com.marcog.peluqueria.productos.infrastructure.out.persistence;

import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.GeneroProducto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "productos")
@Builder @Data @AllArgsConstructor @NoArgsConstructor
public class ProductoEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.UUID) @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal precio;
    @Column(name = "precio_descuento", precision = 10, scale = 2) private BigDecimal precioDescuento;
    @Column(nullable = false) @Builder.Default private Integer stock = 0;
    @Column(name = "stock_minimo", nullable = false) @Builder.Default private Integer stockMinimo = 5;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CategoriaProducto categoria;
    @Enumerated(EnumType.STRING) @Column(nullable = false) @Builder.Default private GeneroProducto genero = GeneroProducto.UNISEX;
    @Column(name = "image_url", length = 500) private String imageUrl;
    @Column(nullable = false) @Builder.Default private boolean activo = true;
    @CreationTimestamp @Column(name = "creado_en", updatable = false) private LocalDateTime creadoEn;
}
