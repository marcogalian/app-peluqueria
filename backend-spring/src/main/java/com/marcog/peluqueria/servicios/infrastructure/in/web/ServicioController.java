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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Servicios", description = "Catalogo de servicios de peluqueria")
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @Operation(summary = "Listar servicios", description = "Retorna todos los servicios disponibles")
    @ApiResponse(responseCode = "200", description = "Lista de servicios")
    @GetMapping
    public ResponseEntity<List<Servicio>> getAllServicios() {
        return ResponseEntity.ok(servicioService.getAllServicios());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear servicio", description = "Crea un nuevo servicio (solo admin)")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    @PostMapping
    public ResponseEntity<Servicio> crear(@RequestBody Servicio servicio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.ejecutar(servicio));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar servicio", description = "Modifica un servicio existente")
    @ApiResponse(responseCode = "200", description = "Servicio actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<Servicio> actualizar(@PathVariable UUID id, @RequestBody Servicio servicio) {
        return ResponseEntity.ok(servicioService.updateServicio(id, servicio));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar servicio", description = "Elimina un servicio del catalogo")
    @ApiResponse(responseCode = "204", description = "Servicio eliminado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
