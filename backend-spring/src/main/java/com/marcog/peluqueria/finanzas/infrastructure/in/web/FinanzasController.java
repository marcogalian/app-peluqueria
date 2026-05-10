package com.marcog.peluqueria.finanzas.infrastructure.in.web;

import com.marcog.peluqueria.finanzas.application.service.FinanzasDashboardService;
import com.marcog.peluqueria.finanzas.application.service.GastoService;
import com.marcog.peluqueria.finanzas.domain.model.DashboardStats;
import com.marcog.peluqueria.finanzas.domain.model.Gasto;
import com.marcog.peluqueria.finanzas.domain.model.ResultadosDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Finanzas", description = "Dashboard financiero, gastos y resultados")
@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final GastoService gastoService;
    private final FinanzasDashboardService dashboardService;

    // ----- Endpoints de Gastos -----

    @Operation(summary = "Listar gastos", description = "Retorna gastos con filtro opcional por mes y anio")
    @ApiResponse(responseCode = "200", description = "Lista de gastos")
    @GetMapping("/gastos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Gasto>> getGastos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes != null && anio != null) {
            return ResponseEntity.ok(gastoService.getGastosByMesAndAnio(mes, anio));
        }
        return ResponseEntity.ok(gastoService.getAllGastos());
    }

    @Operation(summary = "Registrar gasto", description = "Crea un nuevo gasto")
    @ApiResponse(responseCode = "200", description = "Gasto creado")
    @PostMapping("/gastos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Gasto> addGasto(@RequestBody Gasto gasto) {
        return ResponseEntity.ok(gastoService.createGasto(gasto));
    }

    @Operation(summary = "Eliminar gasto", description = "Elimina un gasto")
    @ApiResponse(responseCode = "204", description = "Gasto eliminado")
    @DeleteMapping("/gastos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteGasto(@PathVariable UUID id) {
        gastoService.deleteGasto(id);
        return ResponseEntity.noContent().build();
    }

    // ----- Endpoints del Dashboard Financiero -----

    @Operation(summary = "Dashboard financiero", description = "Estadisticas del mes: ingresos, gastos, beneficio, citas")
    @ApiResponse(responseCode = "200", description = "Stats generadas")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DashboardStats> getDashboardStats(
            @RequestParam int mes,
            @RequestParam int anio) {
        return ResponseEntity.ok(dashboardService.getStatsByMesAndAnio(mes, anio));
    }

    @Operation(summary = "Resultados por periodo", description = "Ingresos, gastos y beneficio agrupados por periodo")
    @ApiResponse(responseCode = "200", description = "Resultados generados")
    @GetMapping("/resultados")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResultadosDTO> getResultados(
            @RequestParam(defaultValue = "mes") String periodo) {
        return ResponseEntity.ok(dashboardService.getResultados(periodo));
    }
}
