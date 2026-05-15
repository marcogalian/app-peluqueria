package com.marcog.peluqueria.chatbot.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    @NotBlank
    @Size(max = 600, message = "El mensaje no puede superar 600 caracteres")
    private String message;

    @Valid
    @Size(max = 12, message = "El historial no puede superar 12 mensajes")
    private List<ChatMessageDto> history;
}
