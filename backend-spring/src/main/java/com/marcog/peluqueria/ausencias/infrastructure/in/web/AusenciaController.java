package com.marcog.peluqueria.ausencias.infrastructure.in.web;

import com.marcog.peluqueria.ausencias.domain.model.SolicitudAusencia;
import com.marcog.peluqueria.ausencias.domain.port.in.GestionarAusenciaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map; import java.util.UUID;

@RestController @RequestMapping("/api/v1/ausencias") @RequiredArgsConstructor
public class AusenciaController {
    private final GestionarAusenciaUseCase useCase;

    @GetMapping
    public ResponseEntity<List<SolicitudAusencia>> listar(
            Authentication auth,
            @RequestParam(required = false) UUID peluqueroId) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.ok(useCase.listarTodas());
        }
        return ResponseEntity.ok(useCase.listarPorPeluquero(peluqueroId));
    }

    @PostMapping
    public ResponseEntity<SolicitudAusencia> solicitar(@RequestBody SolicitudAusencia solicitud) {
        return ResponseEntity.ok(useCase.solicitar(solicitud));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudAusencia> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.aprobar(id));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudAusencia> rechazar(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(useCase.rechazar(id, body.get("motivo")));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitudAusencia> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.cancelar(id));
    }
}
