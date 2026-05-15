package com.marcog.peluqueria.security.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body para POST /api/auth/register.
 *
 * El campo role fue eliminado deliberadamente: el endpoint es publico y
 * permitir asignar rol desde el cliente expondria una via de escalada de
 * privilegios (cualquiera podria registrarse como ROLE_ADMIN).
 * El rol se asigna siempre a ROLE_HAIRDRESSER en autenticarUsuario.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 60)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "solo puede contener letras, numeros, puntos, guiones y guiones bajos")
    private String username;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;
}
