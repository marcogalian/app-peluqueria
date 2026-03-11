package com.marcog.peluqueria.security.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private UUID id;
    private User user;
    private String token;
    private Instant expiryDate;
}
