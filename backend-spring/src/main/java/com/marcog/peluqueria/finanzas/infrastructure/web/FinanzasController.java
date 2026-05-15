package com.marcog.peluqueria.finanzas.infrastructure.web;

import com.marcog.peluqueria.finanzas.application.ConsultarPanelFinanciero;
import com.marcog.peluqueria.finanzas.application.GestionarGastos;
import com.marcog.peluqueria.finanzas.domain.DashboardStats;
import com.marcog.peluqueria.finanzas.domain.Gasto;
import com.marcog.peluqueria.finanzas.domain.ResultadosDTO;
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

    private final GestionarGastos gestionarGastos;
    private final ConsultarPanelFinanciero dashboardService;

    // ----- Endpoints de Gastos -----

    @Operation(summary = "Listar gastos", description = "Retorna gastos con filtro opcional por mes y anio")
    @ApiResponse(responseCode = "200", description = "Lista de gastos")
    @GetMapping("/gastos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Gasto>> getGastos(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        if (mes != null && anio != null) {
            return ResponseEntity.ok(gestionarGastos.getGastosByMesAndAnio(mes, anio));
        }
        return ResponseEntity.ok(gestionarGastos.getAllGastos());
    }

    @Operation(summary = "Registrar gasto", description = "Crea un nuevo gasto")
    @ApiResponse(responseCode = "200", description = "Gasto creado")
    @PostMapping("/gastos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Gasto> addGasto(@RequestBody Gasto gasto) {
        return ResponseEntity.ok(gestionarGastos.createGasto(gasto));
    }

    @Operation(summary = "Eliminar gasto", description = "Elimina un gasto")
    @ApiResponse(responseCode = "204", description = "Gasto eliminado")
    @DeleteMapping("/gastos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteGasto(@PathVariable UUID id) {
        gestionarGastos.deleteGasto(id);
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
