package com.marcog.peluqueria.citas.infrastructure.in.web;

import com.marcog.peluqueria.citas.application.service.CitaService;
import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.infrastructure.in.web.dto.CitaAgendaDto;
import com.marcog.peluqueria.citas.infrastructure.in.web.dto.ResumenDiaDto;
import com.marcog.peluqueria.citas.infrastructure.in.web.dto.CitaResumenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.marcog.peluqueria.citas.application.service.GenerarTicketPdfUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Citas", description = "Gestion de citas y agenda diaria")
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final GenerarTicketPdfUseCase generarTicketPdfUseCase;

    @Operation(summary = "Listar citas", description = "Retorna citas filtradas por rango de fechas y/o peluquero")
    @ApiResponse(responseCode = "200", description = "Lista de citas")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<Cita>> getCitas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) UUID peluqueroId) {
        return ResponseEntity.ok(citaService.getCitas(start, end, peluqueroId));
    }

    @Operation(summary = "Obtener cita por ID", description = "Retorna el detalle de una cita")
    @ApiResponse(responseCode = "200", description = "Cita encontrada")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> getCitaById(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.getCitaById(id));
    }

    @Operation(summary = "Crear cita", description = "Crea una nueva cita validando disponibilidad del peluquero")
    @ApiResponse(responseCode = "200", description = "Cita creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o cita superpuesta")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> createCita(@RequestBody Cita cita) {
        return ResponseEntity.ok(citaService.createCita(cita));
    }

    @Operation(summary = "Actualizar cita", description = "Modifica una cita existente")
    @ApiResponse(responseCode = "200", description = "Cita actualizada")
    @ApiResponse(responseCode = "404", description = "Cita no encontrada")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> updateCita(@PathVariable UUID id, @RequestBody Cita cita) {
        return ResponseEntity.ok(citaService.updateCita(id, cita));
    }

    @Operation(summary = "Cancelar cita", description = "Cancela una cita con motivo opcional")
    @ApiResponse(responseCode = "200", description = "Cita cancelada")
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> cancelarCita(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.getOrDefault("motivo", "") : "";
        return ResponseEntity.ok(citaService.cancelar(id, motivo));
    }

    @Operation(summary = "Eliminar cita", description = "Elimina permanentemente una cita (solo admin)")
    @ApiResponse(responseCode = "204", description = "Cita eliminada")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCita(@PathVariable UUID id) {
        citaService.deleteCita(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene las citas del día para la agenda del empleado autenticado.
     */
    @Operation(summary = "Agenda del dia", description = "Obtiene las citas del dia para la vista de agenda")
    @ApiResponse(responseCode = "200", description = "Lista de citas del dia")
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<CitaAgendaDto>> getCitasAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        List<Cita> citas = citaService.getCitas(inicio, fin, null);

        List<CitaAgendaDto> citasDto = citas.stream()
            .map(this::citaToCitaAgendaDto)
            .toList();

        return ResponseEntity.ok(citasDto);
    }

    /**
     * Obtiene el resumen de citas para un mes (para los badges del calendario).
     */
    @Operation(summary = "Resumen mensual", description = "Resumen de citas agrupadas por dia para el calendario")
    @ApiResponse(responseCode = "200", description = "Resumen por dia")
    @GetMapping("/resumen-mes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<ResumenDiaDto>> getResumenMes(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {
        LocalDateTime inicio = LocalDateTime.of(anio, mes, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(anio, mes, java.time.YearMonth.of(anio, mes).lengthOfMonth(), 23, 59, 59);

        List<Cita> citas = citaService.getCitas(inicio, fin, null);

        var resumenPorDia = citas.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getFechaHora().toLocalDate(),
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));

        List<ResumenDiaDto> resultado = resumenPorDia.entrySet().stream()
            .map(entry -> ResumenDiaDto.builder()
                .fecha(entry.getKey().toString())
                .totalCitas(entry.getValue().size())
                .citas(entry.getValue().stream()
                    .map(this::citaToCitaResumenDto)
                    .toList())
                .build())
            .toList();

        return ResponseEntity.ok(resultado);
    }

    /**
     * HU13: Endpoint para descargar la factura o ticket de la cita en formato PDF.
     */
    @Operation(summary = "Descargar ticket PDF", description = "Genera y descarga el ticket/factura de una cita en PDF")
    @ApiResponse(responseCode = "200", description = "PDF generado")
    @ApiResponse(responseCode = "500", description = "Error generando PDF")
    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<byte[]> descargarTicketPdf(@PathVariable UUID id) {
        try {
            byte[] pdfBytes = generarTicketPdfUseCase.generarTicket(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // inline significa que el navegador intentará abrirlo en una pestaña si puede.
            // (attachment fuerza descarga)
            headers.setContentDispositionFormData("filename", "Ticket_Factura_Cita_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private CitaAgendaDto citaToCitaAgendaDto(Cita cita) {
        String horaInicio = cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm"));
        String servicioNombre = cita.getServicios() != null && !cita.getServicios().isEmpty()
            ? cita.getServicios().get(0).getNombre()
            : "Sin servicio";

        return CitaAgendaDto.builder()
            .id(cita.getId().toString())
            .horaInicio(horaInicio)
            .duracionMinutos(cita.getDuracionTotal())
            .estado(cita.getEstado().toString())
            .clienteNombre(cita.getCliente().getNombre())
            .clienteApellidos(cita.getCliente().getApellidos())
            .clienteEsVip(cita.getCliente().isEsVip())
            .clienteDescuentoPorcentaje(cita.getCliente().getDescuentoPorcentaje())
            .servicioNombre(servicioNombre)
            .comentarios(cita.getComentarios() != null ? cita.getComentarios() : "")
            .motivoCancelacion(cita.getMotivoCancelacion())
            .build();
    }

    private CitaResumenDto citaToCitaResumenDto(Cita cita) {
        String horaInicio = cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm"));
        String servicioNombre = cita.getServicios() != null && !cita.getServicios().isEmpty()
            ? cita.getServicios().get(0).getNombre()
            : "Sin servicio";

        return CitaResumenDto.builder()
            .id(cita.getId().toString())
            .horaInicio(horaInicio)
            .clienteNombre(cita.getCliente().getNombre())
            .clienteApellidos(cita.getCliente().getApellidos())
            .servicioNombre(servicioNombre)
            .estado(cita.getEstado().toString())
            .peluqueroNombre(cita.getPeluquero().getNombre())
            .build();
    }
}
