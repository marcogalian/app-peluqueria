package com.marcog.peluqueria.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @NotBlank
    @Size(max = 120)
    private String username;

    @NotBlank
    @Size(min = 4, max = 120)
    private String password;

    private boolean rememberMe;
}
