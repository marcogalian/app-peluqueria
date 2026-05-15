package com.marcog.peluqueria.peluqueros.infrastructure.web;

import com.marcog.peluqueria.fotos.infrastructure.config.FileStorageConfig;
import com.marcog.peluqueria.peluqueros.application.GestionarEmpleados;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.security.application.GestionarCredenciales;
import com.marcog.peluqueria.security.application.dto.CambiarPasswordEmpleadoRequest;
import jakarta.validation.Valid;
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
import java.util.Set;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Empleados", description = "Gestion de peluqueros/empleados")
@RestController
@RequestMapping("/api/peluqueros")
@RequiredArgsConstructor
public class PeluqueroController {

    private final GestionarEmpleados gestionarEmpleados;
    private final GestionarCredenciales gestionarCredenciales;
    private final FileStorageConfig  storageConfig;

    @Operation(summary = "Listar empleados", description = "Retorna todos los peluqueros del centro")
    @ApiResponse(responseCode = "200", description = "Lista de peluqueros")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<List<Peluquero>> getAllPeluqueros() {
        return ResponseEntity.ok(gestionarEmpleados.getAllPeluqueros());
    }

    @Operation(summary = "Obtener empleado", description = "Retorna un peluquero por su ID")
    @ApiResponse(responseCode = "200", description = "Peluquero encontrado")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<Peluquero> getPeluqueroById(@PathVariable UUID id) {
        return ResponseEntity.ok(gestionarEmpleados.getPeluqueroById(id));
    }

    @Operation(summary = "Crear empleado", description = "Registra un nuevo peluquero (solo admin)")
    @ApiResponse(responseCode = "200", description = "Peluquero creado")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Solo Admin puede crear peluqueros
    public ResponseEntity<Peluquero> createPeluquero(@RequestBody Peluquero peluquero) {
        return ResponseEntity.ok(gestionarEmpleados.createPeluquero(peluquero));
    }

    @Operation(summary = "Actualizar empleado", description = "Modifica los datos de un peluquero")
    @ApiResponse(responseCode = "200", description = "Peluquero actualizado")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> updatePeluquero(
            @PathVariable UUID id,
            @RequestBody Peluquero peluquero) {
        return ResponseEntity.ok(gestionarEmpleados.updatePeluquero(id, peluquero));
    }

    @Operation(summary = "Eliminar empleado", description = "Elimina un peluquero permanentemente")
    @ApiResponse(responseCode = "204", description = "Peluquero eliminado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deletePeluquero(@PathVariable UUID id) {
        gestionarEmpleados.deletePeluquero(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registrar baja", description = "Marca un peluquero como dado de baja")
    @ApiResponse(responseCode = "200", description = "Baja registrada")
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> registrarBaja(@PathVariable UUID id) {
        return ResponseEntity.ok(gestionarEmpleados.registrarBaja(id));
    }

    @Operation(summary = "Reactivar empleado", description = "Reactiva un peluquero dado de baja")
    @ApiResponse(responseCode = "200", description = "Peluquero reactivado")
    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Peluquero> reactivar(@PathVariable UUID id) {
        return ResponseEntity.ok(gestionarEmpleados.reactivar(id));
    }

    @Operation(summary = "Cambiar contraseña de empleado", description = "Permite al admin definir una nueva contraseña para un empleado")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @PostMapping("/clave-empleado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> cambiarPasswordEmpleado(
            @RequestParam UUID peluqueroId,
            @Valid @RequestBody CambiarPasswordEmpleadoRequest request) {
        gestionarCredenciales.cambiarPasswordEmpleado(
                peluqueroId,
                request.getNuevaPassword(),
                request.getRepetirPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir foto", description = "Sube la foto de perfil de un peluquero")
    @ApiResponse(responseCode = "200", description = "Foto subida")
    @PostMapping("/{id}/foto")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> subirFoto(
            @PathVariable UUID id,
            @RequestParam MultipartFile file) throws IOException {

        Path dir = Paths.get(storageConfig.getUploadsDir(), "fotos-peluqueros", id.toString());
        Files.createDirectories(dir);

        String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')).toLowerCase()
                : ".jpg";
        // Solo se permiten imágenes — se rechaza cualquier otro tipo de archivo
        if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de archivo no permitido. Use JPG, PNG o WEBP."));
        }
        String nombre = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(nombre));

        String rutaRelativa = "fotos-peluqueros/" + id + "/" + nombre;
        gestionarEmpleados.actualizarFoto(id, rutaRelativa);

        return ResponseEntity.ok(Map.of("fotoUrl", rutaRelativa));
    }
}
