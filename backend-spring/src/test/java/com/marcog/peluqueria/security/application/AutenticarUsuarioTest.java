package com.marcog.peluqueria.security.application;

import com.marcog.peluqueria.security.application.dto.RegisterRequest;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.config.JwtService;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.RefreshTokenEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica el endurecimiento de seguridad en register():
 * el rol del usuario nunca debe leerse del request publico.
 */
@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioTest {

    @Mock private JpaUserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private GestionarSesion GestionarSesion;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AutenticarUsuario AutenticarUsuario;

    @Test
    @DisplayName("register() asigna SIEMPRE ROLE_HAIRDRESSER, ignorando lo que venga del request")
    void register_siempreAsignaHairdresser() {
        RegisterRequest request = RegisterRequest.builder()
                .username("nuevoUsuario")
                .email("nuevo@test.com")
                .password("contrasena123")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-fake");
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setToken("refresh-fake");
        when(GestionarSesion.createRefreshToken(any())).thenReturn(refreshToken);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        AutenticarUsuario.register(request);

        UserEntity usuarioGuardado = userCaptor.getValue();
        assertEquals(Role.ROLE_HAIRDRESSER, usuarioGuardado.getRole(),
                "El registro publico debe asignar siempre ROLE_HAIRDRESSER");
    }

    @Test
    @DisplayName("register() hashea el password (no lo guarda en plano)")
    void register_passwordHasheado() {
        RegisterRequest request = RegisterRequest.builder()
                .username("usuario")
                .email("u@test.com")
                .password("plainPassword")
                .build();

        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$hashed");
        when(jwtService.generateToken(any(), any())).thenReturn("jwt");
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setToken("refresh");
        when(GestionarSesion.createRefreshToken(any())).thenReturn(refreshToken);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> {
            UserEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        AutenticarUsuario.register(request);

        UserEntity guardado = userCaptor.getValue();
        assertEquals("$2a$hashed", guardado.getPassword());
        assertNotEquals("plainPassword", guardado.getPassword());
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    @DisplayName("register() devuelve token JWT y refresh token")
    void register_devuelveAmbosTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .username("u")
                .email("u@test.com")
                .password("p")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jwtService.generateToken(any(), any())).thenReturn("access-token-jwt");
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setToken("refresh-token-uuid");
        when(GestionarSesion.createRefreshToken(any())).thenReturn(refreshToken);
        when(userRepository.save(any())).thenAnswer(inv -> {
            UserEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        var respuesta = AutenticarUsuario.register(request);

        assertEquals("access-token-jwt", respuesta.getToken());
        assertEquals("refresh-token-uuid", respuesta.getRefreshToken());
    }
}
