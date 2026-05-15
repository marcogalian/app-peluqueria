package com.marcog.peluqueria.fotos.infrastructure.web;

import com.marcog.peluqueria.fotos.domain.FotoCliente;
import com.marcog.peluqueria.fotos.application.GestionarFoto;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fotos", description = "Galeria de fotos de clientes")
@RestController @RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_HAIRDRESSER')")
public class FotoController {
    private final GestionarFoto useCase;

    @Operation(summary = "Listar fotos", description = "Retorna las fotos de un cliente")
    @ApiResponse(responseCode = "200", description = "Lista de fotos")
    @GetMapping("/api/v1/clientes/{clienteId}/fotos")
    public ResponseEntity<List<FotoCliente>> listar(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(useCase.listarPorCliente(clienteId));
    }

    @Operation(summary = "Subir foto", description = "Sube una foto asociada a un cliente")
    @ApiResponse(responseCode = "200", description = "Foto subida")
    @PostMapping("/api/v1/clientes/{clienteId}/fotos")
    public ResponseEntity<FotoCliente> subir(
            @PathVariable UUID clienteId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String descripcion,
            @AuthenticationPrincipal CustomUserDetails principal) {
        UUID peluqueroId = principal.getUserEntity().getId();
        return ResponseEntity.ok(useCase.subir(clienteId, peluqueroId, file, descripcion));
    }

    @Operation(summary = "Actualizar descripción", description = "Actualiza la descripción de una foto")
    @ApiResponse(responseCode = "200", description = "Descripción actualizada")
    @PatchMapping("/api/v1/fotos/{fotoId}/descripcion")
    public ResponseEntity<FotoCliente> actualizarDescripcion(
            @PathVariable UUID fotoId,
            @RequestParam String descripcion) {
        return ResponseEntity.ok(useCase.actualizarDescripcion(fotoId, descripcion));
    }

    @Operation(summary = "Eliminar foto", description = "Elimina una foto de la galeria")
    @ApiResponse(responseCode = "204", description = "Foto eliminada")
    @DeleteMapping("/api/v1/fotos/{fotoId}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID fotoId) {
        useCase.eliminar(fotoId);
        return ResponseEntity.noContent().build();
    }
}
