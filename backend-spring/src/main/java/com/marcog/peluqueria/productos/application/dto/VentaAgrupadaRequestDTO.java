package com.marcog.peluqueria.productos.application.dto;

import com.marcog.peluqueria.productos.domain.model.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaAgrupadaRequestDTO {
    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Valid
    @NotEmpty(message = "La venta debe tener al menos una línea")
    private List<Linea> lineas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Linea {
        @NotNull(message = "El producto es obligatorio")
        private UUID productoId;

        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        private int cantidad;
    }
}
