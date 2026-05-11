package com.marcog.peluqueria.security.application;

import com.marcog.peluqueria.security.infrastructure.persistence.RefreshTokenEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaRefreshTokenRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarSesion {

    @Value("${application.security.jwt.refresh-token.expiration:604800000}") // 7 days default
    private long refreshTokenDurationMs;

    private final JpaRefreshTokenRepository refreshTokenRepository;
    private final JpaUserRepository userRepository;

    @Transactional
    public RefreshTokenEntity createRefreshToken(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Disable old token and flush immediately to avoid OneToOne unique constraint
        // violation
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }
}
