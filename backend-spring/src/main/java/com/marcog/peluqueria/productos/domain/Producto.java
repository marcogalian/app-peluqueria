package com.marcog.peluqueria.productos.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Producto {
    private UUID id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal precioDescuento;
    private Integer stock;
    private Integer stockMinimo;
    private CategoriaProducto categoria;
    private GeneroProducto genero;
    private String imageUrl;
    private boolean activo;
    private LocalDateTime creadoEn;
}
