package com.marcog.peluqueria.productos.application.dto;

import com.marcog.peluqueria.productos.domain.MetodoPago;
import com.marcog.peluqueria.productos.domain.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaAgrupadaResponseDTO {
    private UUID id;
    private String numero;
    private String vendedorNombre;
    private MetodoPago metodoPago;
    private BigDecimal total;
    private LocalDateTime fechaVenta;
    private List<Linea> lineas;
    private List<Producto> productosActualizados;
    private ResumenVentasProductosDTO resumen;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Linea {
        private UUID id;
        private UUID productoId;
        private String productoNombre;
        private int cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal total;
    }
}
