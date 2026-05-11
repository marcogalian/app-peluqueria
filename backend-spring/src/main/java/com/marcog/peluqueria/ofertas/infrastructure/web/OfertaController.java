package com.marcog.peluqueria.ofertas.infrastructure.web;

import com.marcog.peluqueria.ofertas.domain.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import com.marcog.peluqueria.ofertas.application.GestionarOferta;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ofertas", description = "Gestion de ofertas y dias especiales")
@RestController @RequiredArgsConstructor
public class OfertaController {
    private final GestionarOferta useCase;

    @Operation(summary = "Listar ofertas", description = "Retorna todas las ofertas")
    @ApiResponse(responseCode = "200", description = "Lista de ofertas")
    @GetMapping("/api/v1/ofertas")
    public ResponseEntity<List<Oferta>> listar() { return ResponseEntity.ok(useCase.listarOfertas()); }

    @Operation(summary = "Ofertas activas", description = "Retorna solo las ofertas actualmente activas")
    @ApiResponse(responseCode = "200", description = "Lista de ofertas activas")
    @GetMapping("/api/v1/ofertas/activas")
    public ResponseEntity<List<Oferta>> activas() { return ResponseEntity.ok(useCase.listarOfertasActivas()); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear oferta", description = "Crea una nueva oferta promocional")
    @ApiResponse(responseCode = "200", description = "Oferta creada")
    @PostMapping("/api/v1/ofertas")
    public ResponseEntity<Oferta> crear(@RequestBody Oferta oferta) { return ResponseEntity.ok(useCase.crearOferta(oferta)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar oferta", description = "Modifica una oferta existente")
    @ApiResponse(responseCode = "200", description = "Oferta actualizada")
    @PutMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Oferta> actualizar(@PathVariable UUID id, @RequestBody Oferta oferta) { return ResponseEntity.ok(useCase.actualizarOferta(id, oferta)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Toggle oferta", description = "Activa o desactiva una oferta")
    @ApiResponse(responseCode = "200", description = "Estado cambiado")
    @PatchMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Oferta> toggle(@PathVariable UUID id) { return ResponseEntity.ok(useCase.toggleActiva(id)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar oferta", description = "Elimina una oferta")
    @ApiResponse(responseCode = "204", description = "Oferta eliminada")
    @DeleteMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) { useCase.eliminarOferta(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Listar dias especiales", description = "Retorna los dias especiales configurados")
    @ApiResponse(responseCode = "200", description = "Lista de dias especiales")
    @GetMapping("/api/v1/dias-especiales")
    public ResponseEntity<List<DiaEspecial>> listarDias() { return ResponseEntity.ok(useCase.listarDias()); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear dia especial", description = "Registra un nuevo dia especial")
    @ApiResponse(responseCode = "200", description = "Dia creado")
    @PostMapping("/api/v1/dias-especiales")
    public ResponseEntity<DiaEspecial> crearDia(@RequestBody DiaEspecial diaEspecial) { return ResponseEntity.ok(useCase.crearDia(diaEspecial)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar dia especial", description = "Modifica un dia especial")
    @ApiResponse(responseCode = "200", description = "Dia actualizado")
    @PutMapping("/api/v1/dias-especiales/{id}")
    public ResponseEntity<DiaEspecial> actualizarDia(@PathVariable UUID id, @RequestBody DiaEspecial diaEspecial) {
        diaEspecial.setId(id);
        return ResponseEntity.ok(useCase.crearDia(diaEspecial));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar dia especial", description = "Elimina un dia especial")
    @ApiResponse(responseCode = "204", description = "Dia eliminado")
    @DeleteMapping("/api/v1/dias-especiales/{id}")
    public ResponseEntity<Void> eliminarDia(@PathVariable UUID id) { useCase.eliminarDia(id); return ResponseEntity.noContent().build(); }
}
