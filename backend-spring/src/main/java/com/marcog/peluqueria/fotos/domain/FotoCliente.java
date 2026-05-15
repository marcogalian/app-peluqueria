package com.marcog.peluqueria.fotos.domain;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class FotoCliente {
    private UUID id;
    private UUID clienteId;
    private String rutaArchivo;
    private String nombreOriginal;
    private String descripcion;
    private UUID subidaPorPeluqueroId;
    private LocalDateTime creadaEn;
}
