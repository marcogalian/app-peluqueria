package com.marcog.peluqueria.security.application;

import com.marcog.peluqueria.peluqueros.infrastructure.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaPasswordResetTokenRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.PasswordResetTokenEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.shared.notification.Notificaciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GestionarCredenciales {

    private static final Duration RESET_EXPIRATION = Duration.ofMinutes(30);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final JpaUserRepository userRepository;
    private final JpaPeluqueroRepository peluqueroRepository;
    private final JpaPasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Notificaciones notificaciones;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * No revela si el email existe: para el login siempre se procesa igual y,
     * si no es un admin activo, simplemente no se genera token.
     */
    @Transactional
    public void solicitarResetAdmin(String email) {
        String emailNormalizado = normalizarEmail(email);
        userRepository.findByEmail(emailNormalizado)
                .filter(user -> user.isActive() && user.getRole() == Role.ROLE_ADMIN)
                .ifPresent(this::enviarResetAdmin);
    }

    @Transactional
    public void resetearPasswordAdmin(String token, String nuevaPassword) {
        validarPassword(nuevaPassword);

        PasswordResetTokenEntity resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperación inválido."));

        if (resetToken.getUsedAt() != null || resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperación caducado.");
        }

        UserEntity user = resetToken.getUser();
        if (!user.isActive() || user.getRole() != Role.ROLE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperación inválido.");
        }

        user.setPassword(passwordEncoder.encode(nuevaPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        resetTokenRepository.save(resetToken);

        log.info("Password del admin {} restablecida mediante token.", user.getUsername());
    }

    @Transactional
    public void cambiarPasswordEmpleado(UUID peluqueroId, String nuevaPassword, String repetirPassword) {
        validarPassword(nuevaPassword);
        if (!nuevaPassword.equals(repetirPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden.");
        }

        var peluquero = peluqueroRepository.findById(peluqueroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado."));

        UserEntity user = peluquero.getUser();
        if (user == null || user.getRole() != Role.ROLE_HAIRDRESSER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede cambiar la contraseña de empleados.");
        }

        user.setPassword(passwordEncoder.encode(nuevaPassword));
        userRepository.save(user);

        log.info("Password de empleado {} cambiada por administrador.", user.getUsername());
    }

    /** Invalida tokens previos y emite un único enlace temporal para el admin. */
    private void enviarResetAdmin(UserEntity admin) {
        Instant ahora = Instant.now();
        resetTokenRepository.findByUserAndUsedAtIsNull(admin).forEach(token -> {
            token.setUsedAt(ahora);
            resetTokenRepository.save(token);
        });

        String token = generarTokenSeguro();
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .user(admin)
                .token(token)
                .createdAt(ahora)
                .expiryDate(ahora.plus(RESET_EXPIRATION))
                .build();
        resetTokenRepository.save(resetToken);

        String enlace = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        notificaciones.enviarEmailConBoton(
                admin.getEmail(),
                "Recuperación de contraseña - Peluquería Isabella",
                "Hemos recibido una solicitud para restablecer la contraseña de administrador.<br/>" +
                "Pulsa el botón para establecer una nueva contraseña. El enlace es válido durante <strong>30 minutos</strong>.",
                "Restablecer contraseña",
                enlace
        );
    }

    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe contener al menos una mayúscula.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe contener al menos una letra minúscula.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe contener al menos un número.");
        }
    }
}
