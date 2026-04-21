package com.marcog.peluqueria.productos.application.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaProductoRequestDTO {
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    private int cantidad;
}
