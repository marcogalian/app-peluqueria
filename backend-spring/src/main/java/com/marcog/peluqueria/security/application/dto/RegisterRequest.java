package com.marcog.peluqueria.security.application.dto;

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
    private String username;
    private String email;
    private String password;
}
