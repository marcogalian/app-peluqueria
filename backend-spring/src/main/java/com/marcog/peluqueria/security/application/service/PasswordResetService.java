package com.marcog.peluqueria.security.application.service;

import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaPasswordResetTokenRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.PasswordResetTokenEntity;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import com.marcog.peluqueria.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final JpaUserRepository userRepo;
    private final JpaPasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public void solicitar(String email) {
        var userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) return;
        UserEntity user = userOpt.get();
        if (user.getRole() != Role.ROLE_ADMIN) return;

        tokenRepo.deleteByEmail(email);

        String token = UUID.randomUUID().toString();
        tokenRepo.save(PasswordResetTokenEntity.builder()
                .token(token)
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build());

        String enlace = frontendBaseUrl + "/reset-password?token=" + token;
        notificationService.enviarEmail(
                email,
                "Restablecer contraseña — Peluquería Isabella",
                "Hola,\n\nUsa este enlace para restablecer tu contraseña (válido 1 hora):\n\n"
                        + enlace + "\n\nSi no lo solicitaste, ignora este email.\n\nPeluquería Isabella");
        log.info("Token de reset enviado a {}", email);
    }

    @Transactional
    public void resetear(String token, String nuevaPassword) {
        PasswordResetTokenEntity entity = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (entity.isUsed()) throw new IllegalArgumentException("Token ya utilizado");
        if (entity.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Token expirado");

        UserEntity user = userRepo.findByEmail(entity.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(nuevaPassword));
        userRepo.save(user);

        entity.setUsed(true);
        tokenRepo.save(entity);
        log.info("Contraseña restablecida para {}", entity.getEmail());
    }
}
