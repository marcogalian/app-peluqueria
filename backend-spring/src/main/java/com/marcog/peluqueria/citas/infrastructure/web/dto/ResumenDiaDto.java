package com.marcog.peluqueria.citas.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumenDiaDto {
    private String fecha; // "2025-04-02"
    private Integer totalCitas;
    private List<CitaResumenDto> citas;
}
