package com.marcog.peluqueria.security.infrastructure.in.web;

import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.security.application.dto.CambiarPasswordEmpleadoRequest;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Tag(name = "Credenciales", description = "Gestion de contraseñas de empleados")
@RestController
@RequestMapping("/api/credenciales")
@RequiredArgsConstructor
public class CredencialesController {

    private final JpaPeluqueroRepository peluqueroRepo;
    private final JpaUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Cambiar contraseña de empleado", description = "Solo admin. Actualiza la contraseña del usuario asociado al peluquero.")
    @PostMapping("/empleado/{peluqueroId}/contrasena")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable UUID peluqueroId,
            @RequestBody CambiarPasswordEmpleadoRequest request) {

        if (request.nuevaPassword() == null || request.nuevaPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres");
        }

        var peluquero = peluqueroRepo.findById(peluqueroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));

        var user = peluquero.getUser();
        user.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        userRepo.save(user);

        return ResponseEntity.noContent().build();
    }
}
