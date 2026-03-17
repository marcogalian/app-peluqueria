package com.marcog.peluqueria.clientes.domain.model;

import com.marcog.peluqueria.citas.domain.model.Cita;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistorialClinicoDTO {
    private Cliente cliente;
    private List<Cita> citasList;
    private String notasMedicas;
    private String formulasTinte;
    // Esto expande la funcionalidad de notas básicas para la HU11
}
