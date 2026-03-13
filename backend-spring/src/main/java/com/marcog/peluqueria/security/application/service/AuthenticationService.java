package com.marcog.peluqueria.security.application.service;

import com.marcog.peluqueria.security.application.dto.AuthRequest;
import com.marcog.peluqueria.security.application.dto.AuthResponse;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import com.marcog.peluqueria.security.infrastructure.config.JwtService;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.RefreshTokenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.marcog.peluqueria.security.application.dto.RegisterRequest;
import com.marcog.peluqueria.security.domain.model.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

        private final JpaUserRepository repository;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final RefreshTokenService refreshTokenService;
        private final PasswordEncoder passwordEncoder;

        public AuthResponse register(RegisterRequest request) {
                var user = UserEntity.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(request.getRole() != null ? request.getRole() : Role.ROLE_HAIRDRESSER)
                                .active(true)
                                .build();

                repository.save(user);

                var customUserDetails = new CustomUserDetails(user);

                java.util.Map<String, Object> claims = new java.util.HashMap<>();
                claims.put("role", user.getRole().name());
                var jwtToken = jwtService.generateToken(claims, customUserDetails);

                RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user.getId());

                return AuthResponse.builder()
                                .token(jwtToken)
                                .refreshToken(refreshToken.getToken())
                                .build();
        }

        public AuthResponse authenticate(AuthRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                var userEntity = repository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                var customUserDetails = new CustomUserDetails(userEntity);

                java.util.Map<String, Object> claims = new java.util.HashMap<>();
                claims.put("role", userEntity.getRole().name());
                var jwtToken = jwtService.generateToken(claims, customUserDetails);

                String refreshTokenString = null;
                if (request.isRememberMe()) {
                        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(userEntity.getId());
                        refreshTokenString = refreshToken.getToken();
                }

                return AuthResponse.builder()
                                .token(jwtToken)
                                .refreshToken(refreshTokenString)
                                .build();
        }
}
