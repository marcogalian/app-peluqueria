package com.marcog.peluqueria.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO que representa un mensaje en tránsito de la aplicación.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private String emisor; // Quién lo manda ("Admin" o "Peluquero A")
    private String contenidoAES; // El texto cifrado (Ilegible)
    private LocalDateTime fecha; // Cuando se envió
    private MessageType tipo;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}
