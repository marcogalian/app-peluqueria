package com.marcog.peluqueria.peluqueros.infrastructure.in.web;

import com.marcog.peluqueria.peluqueros.application.service.PeluqueroService;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/peluqueros")
@RequiredArgsConstructor
public class PeluqueroController {

    private final PeluqueroService peluqueroService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<Peluquero>> getAllPeluqueros() {
        return ResponseEntity.ok(peluqueroService.getAllPeluqueros());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Peluquero> getPeluqueroById(@PathVariable UUID id) {
        return ResponseEntity.ok(peluqueroService.getPeluqueroById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Solo Admin puede crear peluqueros
    public ResponseEntity<Peluquero> createPeluquero(@RequestBody Peluquero peluquero) {
        return ResponseEntity.ok(peluqueroService.createPeluquero(peluquero));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> updatePeluquero(
            @PathVariable UUID id,
            @RequestBody Peluquero peluquero) {
        return ResponseEntity.ok(peluqueroService.updatePeluquero(id, peluquero));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePeluquero(@PathVariable UUID id) {
        peluqueroService.deletePeluquero(id);
        return ResponseEntity.noContent().build();
    }
}
