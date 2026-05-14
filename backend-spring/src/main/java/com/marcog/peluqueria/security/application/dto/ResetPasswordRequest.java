package com.marcog.peluqueria.security.application.dto;

public record ResetPasswordRequest(String token, String nuevaPassword) {}
