package com.marcog.peluqueria.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambiarPasswordEmpleadoRequest {
    @NotBlank
    @Size(min = 8)
    private String nuevaPassword;

    @NotBlank
    private String repetirPassword;
}
