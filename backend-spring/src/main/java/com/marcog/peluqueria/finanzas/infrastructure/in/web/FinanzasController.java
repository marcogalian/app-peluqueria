package com.marcog.peluqueria.finanzas.infrastructure.in.web;

import com.marcog.peluqueria.finanzas.application.service.FinanzasDashboardService;
import com.marcog.peluqueria.finanzas.application.service.GastoService;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.finanzas.domain.model.Gasto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final GastoService gastoService;
    private final FinanzasDashboardService dashboardService;

    // ----- Endpoints de Gastos -----

    @GetMapping("/gastos")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<Gasto>> getGastos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes != null && anio != null) {
            return ResponseEntity.ok(gastoService.getGastosByMesAndAnio(mes, anio));
        }
        return ResponseEntity.ok(gastoService.getAllGastos());
    }

    @PostMapping("/gastos")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Gasto> addGasto(@RequestBody Gasto gasto) {
        return ResponseEntity.ok(gastoService.createGasto(gasto));
    }

    @DeleteMapping("/gastos/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteGasto(@PathVariable UUID id) {
        gastoService.deleteGasto(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Endpoints del Dashboard Financiero -----

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DashboardStats> getDashboardStats(
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(dashboardService.getStatsByMesAndAnio(mes, anio));
    }
}
