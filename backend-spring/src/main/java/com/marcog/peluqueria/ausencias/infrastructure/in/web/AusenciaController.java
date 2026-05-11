package com.marcog.peluqueria.ausencias.infrastructure.in.web;

import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.port.in.GestionarAusenciaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map; import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ausencias", description = "Solicitudes de vacaciones y ausencias")
@RestController @RequestMapping("/api/v1/ausencias") @RequiredArgsConstructor
public class AusenciaController {
    private final GestionarAusenciaUseCase useCase;

    @Operation(summary = "Listar ausencias", description = "Admin ve todas, empleado ve las suyas")
    @ApiResponse(responseCode = "200", description = "Lista de ausencias")
    @GetMapping
    public ResponseEntity<List<SolicitudAusencia>> listar(
            Authentication auth,
            @RequestParam(required = false) UUID peluqueroId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(useCase.listarTodas());
        }
        return ResponseEntity.ok(useCase.listarPorPeluquero(peluqueroId));
    }

    @Operation(summary = "Solicitar ausencia", description = "Crea una nueva solicitud de ausencia/vacaciones")
    @ApiResponse(responseCode = "200", description = "Solicitud creada")
    @PostMapping
    public ResponseEntity<SolicitudAusencia> solicitar(@RequestBody SolicitudAusencia solicitud) {
        return ResponseEntity.ok(useCase.solicitar(solicitud));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Aprobar ausencia", description = "Aprueba una solicitud de ausencia (solo admin)")
    @ApiResponse(responseCode = "200", description = "Solicitud aprobada")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudAusencia> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.aprobar(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Rechazar ausencia", description = "Rechaza una solicitud con motivo (solo admin)")
    @ApiResponse(responseCode = "200", description = "Solicitud rechazada")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudAusencia> rechazar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(useCase.rechazar(id, body.get("motivo")));
    }

    @Operation(summary = "Cancelar ausencia", description = "Cancela una solicitud de ausencia propia")
    @ApiResponse(responseCode = "200", description = "Solicitud cancelada")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitudAusencia> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.cancelar(id));
    }

    @Operation(summary = "Marcar notificacion como vista",
            description = "El empleado confirma que ha leido el card de aprobacion/rechazo")
    @ApiResponse(responseCode = "200", description = "Marcada como vista")
    @PatchMapping("/{id}/marcar-vista")
    public ResponseEntity<SolicitudAusencia> marcarVista(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.marcarComoVista(id));
    }
}
