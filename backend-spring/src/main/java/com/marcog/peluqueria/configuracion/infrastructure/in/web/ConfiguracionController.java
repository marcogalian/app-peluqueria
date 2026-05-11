package com.marcog.peluqueria.configuracion.infrastructure.in.web;

import com.marcog.peluqueria.configuracion.application.service.ConfiguracionService;
import com.marcog.peluqueria.configuracion.infrastructure.out.persistence.ConfiguracionEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionService.construirRespuesta());
    }

    @PutMapping("/centro")
    public ResponseEntity<Map<String, Object>> guardarCentro(@RequestBody ConfiguracionCentroRequest request) {
        ConfiguracionEntity cambios = ConfiguracionEntity.builder()
                .nombreNegocio(request.getNombreNegocio())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .direccion(request.getDireccion())
                .politicaFotos(request.getPoliticaFotos())
                .horarioApertura(request.getHorarioApertura())
                .horarioCierre(request.getHorarioCierre())
                .horarioCierreSabado(request.getHorarioCierreSabado())
                .abreSabado(Boolean.TRUE.equals(request.getAbreSabado()))
                .abreDomingo(Boolean.TRUE.equals(request.getAbreDomingo()))
                .build();
        configuracionService.guardarCentro(cambios);
        return ResponseEntity.ok(configuracionService.construirRespuesta());
    }

    @PutMapping("/comunicacion")
    public ResponseEntity<Map<String, Object>> guardarComunicacion(@RequestBody ConfiguracionComunicacionRequest request) {
        configuracionService.guardarComunicacion(
                request.getEmailRecordatorio(),
                request.getHorasAntelacionRecordatorio()
        );
        return ResponseEntity.ok(configuracionService.construirRespuesta());
    }

    @Data
    public static class ConfiguracionCentroRequest {
        private String nombreNegocio;
        private String telefono;
        private String email;
        private String direccion;
        private String politicaFotos;
        // Horario laboral del salon
        private String horarioApertura;
        private String horarioCierre;
        private String horarioCierreSabado;
        private Boolean abreSabado;
        private Boolean abreDomingo;
    }

    @Data
    public static class ConfiguracionComunicacionRequest {
        private Boolean emailRecordatorio;
        private Integer horasAntelacionRecordatorio;
    }
}
