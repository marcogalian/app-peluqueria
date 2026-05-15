package com.marcog.peluqueria.security.infrastructure.web;

import com.marcog.peluqueria.security.application.dto.AuthRequest;
import com.marcog.peluqueria.security.application.dto.AuthResponse;
import com.marcog.peluqueria.security.application.dto.ForgotPasswordRequest;
import com.marcog.peluqueria.security.application.dto.RefreshTokenRequest;
import com.marcog.peluqueria.security.application.dto.RegisterRequest;
import com.marcog.peluqueria.security.application.dto.ResetPasswordRequest;
import com.marcog.peluqueria.security.application.AutenticarUsuario;
import com.marcog.peluqueria.security.application.GestionarCredenciales;
import com.marcog.peluqueria.security.application.GestionarSesion;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import com.marcog.peluqueria.security.infrastructure.config.JwtService;
import com.marcog.peluqueria.security.infrastructure.persistence.RefreshTokenEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticacion", description = "Registro, login y renovacion de tokens JWT")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AutenticarUsuario autenticarUsuario;
    private final GestionarSesion gestionarSesion;
    private final GestionarCredenciales gestionarCredenciales;
    private final JwtService jwtService;

    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponse(responseCode = "200", description = "Usuario registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(autenticarUsuario.register(request));
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica con credenciales y devuelve token JWT")
    @ApiResponse(responseCode = "200", description = "Token generado")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(autenticarUsuario.authenticate(request));
    }

    @Operation(summary = "Solicitar recuperación admin", description = "Envia un enlace de recuperación solo si el email pertenece al administrador")
    @ApiResponse(responseCode = "204", description = "Solicitud procesada")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        gestionarCredenciales.solicitarResetAdmin(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restablecer contraseña admin", description = "Cambia la contraseña de administrador usando un token temporal")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        gestionarCredenciales.resetearPasswordAdmin(request.getToken(), request.getNuevaPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Renovar token", description = "Genera nuevo access token a partir del refresh token")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @ApiResponse(responseCode = "401", description = "Refresh token invalido")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return gestionarSesion.findByToken(request.getRefreshToken())
                .map(gestionarSesion::verifyExpiration)
                .map(RefreshTokenEntity::getUser)
                .map(user -> {
                    // Incluir el rol como claim igual que en login. Sin esto, el access token
                    // refrescado pierde el rol y los @PreAuthorize fallarian.
                    java.util.Map<String, Object> claims = new java.util.HashMap<>();
                    claims.put("role", user.getRole().name());
                    String accessToken = jwtService.generateToken(claims, new CustomUserDetails(user));
                    return ResponseEntity.ok(AuthResponse.builder()
                            .token(accessToken)
                            .refreshToken(request.getRefreshToken())
                            .build());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
