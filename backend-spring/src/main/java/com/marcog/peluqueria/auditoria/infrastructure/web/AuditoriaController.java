package com.marcog.peluqueria.auditoria.infrastructure.web;

import com.marcog.peluqueria.auditoria.application.RegistrarActividad;
import com.marcog.peluqueria.auditoria.domain.RegistroActividad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Auditoria", description = "Registro de acciones realizadas por usuarios")
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditoriaController {
    private final RegistrarActividad registrarActividad;

    @Operation(summary = "Listar actividad reciente", description = "Devuelve las ultimas acciones registradas en el sistema")
    @GetMapping
    public List<RegistroActividad> listar(@RequestParam(defaultValue = "100") int limite) {
        return registrarActividad.listarRecientes(limite);
    }
}
