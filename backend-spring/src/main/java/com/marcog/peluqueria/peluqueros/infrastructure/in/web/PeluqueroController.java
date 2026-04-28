package com.marcog.peluqueria.peluqueros.infrastructure.in.web;

import com.marcog.peluqueria.fotos.infrastructure.config.FileStorageConfig;
import com.marcog.peluqueria.peluqueros.application.service.PeluqueroService;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/peluqueros")
@RequiredArgsConstructor
public class PeluqueroController {

    private final PeluqueroService   peluqueroService;
    private final FileStorageConfig  storageConfig;

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

    @PostMapping("/{id}/baja")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> registrarBaja(@PathVariable UUID id) {
        return ResponseEntity.ok(peluqueroService.registrarBaja(id));
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(peluqueroService.reactivar(id));
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> subirFoto(
            @PathVariable UUID id,
            @RequestParam MultipartFile file) throws IOException {

        Path dir = Paths.get(storageConfig.getUploadsDir(), "fotos-peluqueros", id.toString());
        Files.createDirectories(dir);

        String ext        = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                            ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                            : ".jpg";
        String nombre     = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(nombre));

        String rutaRelativa = "fotos-peluqueros/" + id + "/" + nombre;
        peluqueroService.actualizarFoto(id, rutaRelativa);

        return ResponseEntity.ok(Map.of("fotoUrl", rutaRelativa));
    }
}
