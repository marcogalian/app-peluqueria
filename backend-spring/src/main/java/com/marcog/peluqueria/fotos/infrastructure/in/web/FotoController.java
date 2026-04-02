package com.marcog.peluqueria.fotos.infrastructure.in.web;

import com.marcog.peluqueria.fotos.domain.model.FotoCliente;
import com.marcog.peluqueria.fotos.domain.port.in.GestionarFotoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; import java.util.UUID;

@RestController @RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FotoController {
    private final GestionarFotoUseCase useCase;

    @GetMapping("/api/v1/clientes/{clienteId}/fotos")
    public ResponseEntity<List<FotoCliente>> listar(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(useCase.listarPorCliente(clienteId));
    }

    @PostMapping("/api/v1/clientes/{clienteId}/fotos")
    public ResponseEntity<FotoCliente> subir(
            @PathVariable UUID clienteId,
            @RequestParam UUID peluqueroId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String descripcion) {
        return ResponseEntity.ok(useCase.subir(clienteId, peluqueroId, file, descripcion));
    }

    @DeleteMapping("/api/v1/fotos/{fotoId}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID fotoId) {
        useCase.eliminar(fotoId);
        return ResponseEntity.noContent().build();
    }
}
