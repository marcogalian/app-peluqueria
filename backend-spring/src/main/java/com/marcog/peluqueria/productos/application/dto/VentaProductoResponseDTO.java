package com.marcog.peluqueria.productos.application.dto;

import com.marcog.peluqueria.productos.domain.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaProductoResponseDTO {
    private Producto producto;
    private Venta venta;
    private ResumenVentasProductosDTO resumen;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Venta {
        private UUID id;
        private int cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal total;
        private LocalDateTime fechaVenta;
    }
}
