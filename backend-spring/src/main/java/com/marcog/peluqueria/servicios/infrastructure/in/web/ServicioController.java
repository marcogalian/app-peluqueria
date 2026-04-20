package com.marcog.peluqueria.servicios.infrastructure.in.web;

import com.marcog.peluqueria.servicios.application.ServicioService;
import com.marcog.peluqueria.servicios.domain.model.Servicio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    public ResponseEntity<List<Servicio>> getAllServicios() {
        return ResponseEntity.ok(servicioService.getAllServicios());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Servicio> crear(@RequestBody Servicio servicio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.ejecutar(servicio));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Servicio> actualizar(@PathVariable UUID id, @RequestBody Servicio servicio) {
        return ResponseEntity.ok(servicioService.updateServicio(id, servicio));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
