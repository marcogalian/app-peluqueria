package com.marcog.peluqueria.ausencias.infrastructure.web;

import com.marcog.peluqueria.ausencias.application.GestionarDiasBloqueados;
import com.marcog.peluqueria.ausencias.domain.DiaBloqueado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Días bloqueados", description = "Días en los que no se permite solicitar vacaciones")
@RestController
@RequestMapping("/api/v1/dias-bloqueados")
@RequiredArgsConstructor
public class DiaBloqueadoController {

    private final GestionarDiasBloqueados service;

    @Operation(summary = "Listar días bloqueados",
            description = "Cualquier usuario autenticado puede ver los días bloqueados (los empleados los necesitan al solicitar vacaciones)")
    @GetMapping
    public ResponseEntity<List<DiaBloqueado>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear día bloqueado", description = "Solo admin")
    @PostMapping
    public ResponseEntity<DiaBloqueado> crear(@Valid @RequestBody DiaBloqueadoRequest request) {
        DiaBloqueado nuevo = DiaBloqueado.builder()
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin() != null ? request.getFechaFin() : request.getFechaInicio())
                .motivo(request.getMotivo())
                .build();
        return ResponseEntity.ok(service.crear(nuevo));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar día bloqueado", description = "Solo admin")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class DiaBloqueadoRequest {
        @NotNull(message = "La fecha de inicio es obligatoria")
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private String motivo;
    }
}
