package com.marcog.peluqueria.chatbot.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDto {
    @NotBlank
    @Pattern(regexp = "^(user|assistant)$", message = "rol de mensaje no valido")
    private String role;

    @NotBlank
    @Size(max = 800, message = "Cada mensaje del historial no puede superar 800 caracteres")
    private String content;
}
