package com.marcog.peluqueria.ofertas.infrastructure.in.web;

import com.marcog.peluqueria.ofertas.domain.model.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import com.marcog.peluqueria.ofertas.domain.port.in.GestionarOfertaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.UUID;

@RestController @RequiredArgsConstructor
public class OfertaController {
    private final GestionarOfertaUseCase useCase;

    @GetMapping("/api/v1/ofertas")
    public ResponseEntity<List<Oferta>> listar() { return ResponseEntity.ok(useCase.listarOfertas()); }

    @GetMapping("/api/v1/ofertas/activas")
    public ResponseEntity<List<Oferta>> activas() { return ResponseEntity.ok(useCase.listarOfertasActivas()); }

    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/api/v1/ofertas")
    public ResponseEntity<Oferta> crear(@RequestBody Oferta o) { return ResponseEntity.ok(useCase.crearOferta(o)); }

    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Oferta> actualizar(@PathVariable UUID id, @RequestBody Oferta o) { return ResponseEntity.ok(useCase.actualizarOferta(id, o)); }

    @PreAuthorize("hasRole('ADMIN')") @PatchMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Oferta> toggle(@PathVariable UUID id) { return ResponseEntity.ok(useCase.toggleActiva(id)); }

    @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/api/v1/ofertas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) { useCase.eliminarOferta(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/api/v1/dias-especiales")
    public ResponseEntity<List<DiaEspecial>> listarDias() { return ResponseEntity.ok(useCase.listarDias()); }

    @PreAuthorize("hasRole('ADMIN')") @PostMapping("/api/v1/dias-especiales")
    public ResponseEntity<DiaEspecial> crearDia(@RequestBody DiaEspecial d) { return ResponseEntity.ok(useCase.crearDia(d)); }

    @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/api/v1/dias-especiales/{id}")
    public ResponseEntity<Void> eliminarDia(@PathVariable UUID id) { useCase.eliminarDia(id); return ResponseEntity.noContent().build(); }
}
