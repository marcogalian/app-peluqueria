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

    private String emisor;
    private String contenidoAES;
    private LocalDateTime fecha;
    private MessageType tipo;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}
