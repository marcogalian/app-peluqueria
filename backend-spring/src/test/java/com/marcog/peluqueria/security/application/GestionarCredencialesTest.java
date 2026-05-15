package com.marcog.peluqueria.security.application;

import com.marcog.peluqueria.peluqueros.infrastructure.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.PeluqueroEntity;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaPasswordResetTokenRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.PasswordResetTokenEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.shared.notification.Notificaciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarCredencialesTest {

    @Mock private JpaUserRepository userRepository;
    @Mock private JpaPeluqueroRepository peluqueroRepository;
    @Mock private JpaPasswordResetTokenRepository resetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Notificaciones notificaciones;

    @InjectMocks
    private GestionarCredenciales gestionarCredenciales;

    @Test
    @DisplayName("solicitarResetAdmin() ignora empleados para no exponer recuperación")
    void solicitarResetAdmin_ignoraUsuariosNoAdmin() {
        UserEntity empleado = usuario("lucia", "lucia@email.com", Role.ROLE_HAIRDRESSER);
        when(userRepository.findByEmail("lucia@email.com")).thenReturn(Optional.of(empleado));

        gestionarCredenciales.solicitarResetAdmin("  LUCIA@email.com ");

        verify(resetTokenRepository, never()).save(any());
        verify(notificaciones, never()).enviarEmailConBoton(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("solicitarResetAdmin() invalida tokens previos y envía enlace temporal")
    void solicitarResetAdmin_invalidaTokensPreviosYEnviaEmail() {
        ReflectionTestUtils.setField(gestionarCredenciales, "frontendBaseUrl", "http://localhost:3000/");
        UserEntity admin = usuario("admin", "admin@email.com", Role.ROLE_ADMIN);
        PasswordResetTokenEntity tokenPrevio = PasswordResetTokenEntity.builder()
                .user(admin)
                .token("token-previo")
                .createdAt(Instant.now().minusSeconds(120))
                .expiryDate(Instant.now().plusSeconds(120))
                .build();

        when(userRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(admin));
        when(resetTokenRepository.findByUserAndUsedAtIsNull(admin)).thenReturn(List.of(tokenPrevio));

        gestionarCredenciales.solicitarResetAdmin("admin@email.com");

        assertNotNull(tokenPrevio.getUsedAt(), "Los tokens anteriores quedan invalidados");
        verify(resetTokenRepository, atLeast(2)).save(any(PasswordResetTokenEntity.class));
        verify(notificaciones).enviarEmailConBoton(
                eq("admin@email.com"),
                contains("Recuperación"),
                contains("30 minutos"),
                eq("Restablecer contraseña"),
                startsWith("http://localhost:3000/reset-password?token=")
        );
    }

    @Test
    @DisplayName("resetearPasswordAdmin() rechaza tokens caducados")
    void resetearPasswordAdmin_rechazaTokenCaducado() {
        UserEntity admin = usuario("admin", "admin@email.com", Role.ROLE_ADMIN);
        PasswordResetTokenEntity caducado = PasswordResetTokenEntity.builder()
                .user(admin)
                .token("caducado")
                .createdAt(Instant.now().minusSeconds(3600))
                .expiryDate(Instant.now().minusSeconds(60))
                .build();

        when(resetTokenRepository.findByToken("caducado")).thenReturn(Optional.of(caducado));

        assertThrows(ResponseStatusException.class,
                () -> gestionarCredenciales.resetearPasswordAdmin("caducado", "Nueva123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarPasswordEmpleado() exige que ambas contraseñas coincidan")
    void cambiarPasswordEmpleado_rechazaPasswordRepetidoDistinto() {
        UUID peluqueroId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class,
                () -> gestionarCredenciales.cambiarPasswordEmpleado(peluqueroId, "Nueva123", "Otra1234"));

        verify(peluqueroRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiarPasswordEmpleado() hashea y guarda solo usuarios empleados")
    void cambiarPasswordEmpleado_hasheaPasswordEmpleado() {
        UUID peluqueroId = UUID.randomUUID();
        UserEntity user = usuario("carmen", "carmen@email.com", Role.ROLE_HAIRDRESSER);
        PeluqueroEntity peluquero = PeluqueroEntity.builder()
                .id(peluqueroId)
                .user(user)
                .nombre("Carmen López")
                .build();

        when(peluqueroRepository.findById(peluqueroId)).thenReturn(Optional.of(peluquero));
        when(passwordEncoder.encode("Nueva123")).thenReturn("$2a$hash");

        gestionarCredenciales.cambiarPasswordEmpleado(peluqueroId, "Nueva123", "Nueva123");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("$2a$hash", userCaptor.getValue().getPassword());
    }

    private UserEntity usuario(String username, String email, Role role) {
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password("$2a$old")
                .role(role)
                .active(true)
                .build();
    }
}
