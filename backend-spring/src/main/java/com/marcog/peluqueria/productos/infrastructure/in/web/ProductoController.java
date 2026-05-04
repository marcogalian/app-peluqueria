package com.marcog.peluqueria.productos.infrastructure.in.web;

import com.marcog.peluqueria.productos.application.dto.ResumenVentasProductosDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaRequestDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaResponseDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoRequestDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoResponseDTO;
import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.in.GestionarProductoUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/productos") @RequiredArgsConstructor
public class ProductoController {
    private final GestionarProductoUseCase useCase;

    @GetMapping
    public ResponseEntity<List<Producto>> listar(@RequestParam(required = false) CategoriaProducto categoria) {
        return ResponseEntity.ok(useCase.listar(categoria));
    }

    @GetMapping("/ventas/resumen")
    public ResponseEntity<ResumenVentasProductosDTO> resumenVentas() {
        return ResponseEntity.ok(useCase.obtenerResumenVentas());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) { return ResponseEntity.ok(useCase.crear(producto)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable UUID id, @RequestBody Producto producto) { return ResponseEntity.ok(useCase.actualizar(id, producto)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) { useCase.eliminar(id); return ResponseEntity.noContent().build(); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Producto> ajustarStock(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(useCase.ajustarStock(id, body.get("cantidad")));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    @PostMapping("/ventas")
    public ResponseEntity<VentaAgrupadaResponseDTO> venderAgrupado(
            @Valid @RequestBody VentaAgrupadaRequestDTO body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(useCase.venderAgrupado(body, userDetails.getUsername()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    @PostMapping("/{id}/vender")
    public ResponseEntity<VentaProductoResponseDTO> vender(
            @PathVariable UUID id,
            @Valid @RequestBody VentaProductoRequestDTO body) {
        return ResponseEntity.ok(useCase.vender(id, body.getCantidad()));
    }
}
