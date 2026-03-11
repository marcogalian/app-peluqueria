package com.marcog.peluqueria.security.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private UUID id;
    private String username;
    private String password;
    private Role role;
    private String email;
    private boolean active;
}
