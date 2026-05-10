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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Productos", description = "Gestion de productos e inventario")
@RestController @RequestMapping("/api/v1/productos") @RequiredArgsConstructor
public class ProductoController {
    private final GestionarProductoUseCase useCase;

    @Operation(summary = "Listar productos", description = "Retorna productos con filtro opcional por categoria")
    @ApiResponse(responseCode = "200", description = "Lista de productos")
    @GetMapping
    public ResponseEntity<List<Producto>> listar(@RequestParam(required = false) CategoriaProducto categoria) {
        return ResponseEntity.ok(useCase.listar(categoria));
    }

    @Operation(summary = "Resumen de ventas", description = "Estadisticas de ventas: ingresos y unidades por semana/mes/anio")
    @ApiResponse(responseCode = "200", description = "Resumen generado")
    @GetMapping("/ventas/resumen")
    public ResponseEntity<ResumenVentasProductosDTO> resumenVentas() {
        return ResponseEntity.ok(useCase.obtenerResumenVentas());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear producto", description = "Registra un nuevo producto en inventario")
    @ApiResponse(responseCode = "200", description = "Producto creado")
    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) { return ResponseEntity.ok(useCase.crear(producto)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Modifica un producto existente")
    @ApiResponse(responseCode = "200", description = "Producto actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable UUID id, @RequestBody Producto producto) { return ResponseEntity.ok(useCase.actualizar(id, producto)); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar producto", description = "Desactiva un producto (soft delete)")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) { useCase.eliminar(id); return ResponseEntity.noContent().build(); }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Ajustar stock", description = "Incrementa o decrementa el stock de un producto")
    @ApiResponse(responseCode = "200", description = "Stock ajustado")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Producto> ajustarStock(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(useCase.ajustarStock(id, body.get("cantidad")));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    @Operation(summary = "Venta agrupada", description = "Registra una venta con multiples productos")
    @ApiResponse(responseCode = "200", description = "Venta registrada")
    @ApiResponse(responseCode = "400", description = "Stock insuficiente")
    @PostMapping("/ventas")
    public ResponseEntity<VentaAgrupadaResponseDTO> venderAgrupado(
            @Valid @RequestBody VentaAgrupadaRequestDTO body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(useCase.venderAgrupado(body, userDetails.getUsername()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    @Operation(summary = "Vender producto individual", description = "Registra la venta de un solo producto")
    @ApiResponse(responseCode = "200", description = "Venta registrada")
    @ApiResponse(responseCode = "400", description = "Stock insuficiente")
    @PostMapping("/{id}/vender")
    public ResponseEntity<VentaProductoResponseDTO> vender(
            @PathVariable UUID id,
            @Valid @RequestBody VentaProductoRequestDTO body) {
        return ResponseEntity.ok(useCase.vender(id, body.getCantidad()));
    }
}
