package com.marcog.peluqueria.productos.infrastructure.in.web;

import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.in.GestionarProductoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map; import java.util.UUID;

@RestController @RequestMapping("/api/v1/productos") @RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProductoController {
    private final GestionarProductoUseCase useCase;

    @GetMapping
    public ResponseEntity<List<Producto>> listar(@RequestParam(required = false) CategoriaProducto categoria) {
        return ResponseEntity.ok(useCase.listar(categoria));
    }

    @PreAuthorize("hasRole('ADMIN')") @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) { return ResponseEntity.ok(useCase.crear(producto)); }

    @PreAuthorize("hasRole('ADMIN')") @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable UUID id, @RequestBody Producto producto) { return ResponseEntity.ok(useCase.actualizar(id, producto)); }

    @PreAuthorize("hasRole('ADMIN')") @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) { useCase.eliminar(id); return ResponseEntity.noContent().build(); }

    @PreAuthorize("hasRole('ADMIN')") @PatchMapping("/{id}/stock")
    public ResponseEntity<Producto> ajustarStock(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(useCase.ajustarStock(id, body.get("cantidad")));
    }
}
