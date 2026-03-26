package com.marcog.peluqueria.citas.infrastructure.in.web;

import com.marcog.peluqueria.citas.application.service.CitaService;
import com.marcog.peluqueria.citas.domain.model.Cita;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.marcog.peluqueria.citas.application.service.GenerarTicketPdfUseCase;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final GenerarTicketPdfUseCase generarTicketPdfUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<Cita>> getCitas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) UUID peluqueroId) {
        return ResponseEntity.ok(citaService.getCitas(start, end, peluqueroId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> getCitaById(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.getCitaById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> createCita(@RequestBody Cita cita) {
        return ResponseEntity.ok(citaService.createCita(cita));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Cita> updateCita(@PathVariable UUID id, @RequestBody Cita cita) {
        return ResponseEntity.ok(citaService.updateCita(id, cita));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCita(@PathVariable UUID id) {
        citaService.deleteCita(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * HU13: Endpoint para descargar la factura o ticket de la cita en formato PDF.
     */
    @GetMapping("/{id}/ticket")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
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
}
