package com.marcog.peluqueria.chat.infrastructure.web;

import com.marcog.peluqueria.chat.domain.ChatMessage;
import com.marcog.peluqueria.security.application.util.AESCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * Controlador de WebSockets para el Chat de la Peluquería.
 * Intercepta los mensajes que los clientes mandan por rutas "/app/...".
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final AESCryptoUtil aesCryptoUtil;

    /**
     * Las aplicaciones cliente enviarán mensajes aquí usando la ruta
     * "/app/chat.sendMessage"
     * Se redirige broadcast a todos los subscritos en la "sala" principal
     * "/topic/public"
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage interceptarAESYEnviar(@Payload ChatMessage chatMessage) {
        log.info("Mensaje encriptado recibido de: {} -> [{}]",
                chatMessage.getEmisor(),
                chatMessage.getContenidoAES());

        // El servidor no necesita saber el contenido si solo hace de puente (Pasarela
        // Segura),
        // pero vamos a descifrarlo únicamente por motivos de registro/auditoría (Log)
        // en consola:
        String textoReal = aesCryptoUtil.decrypt(chatMessage.getContenidoAES());
        log.info("El texto original en memoria interna fue: \"{}\"", textoReal);

        // Volvemos a enviarlo cifrado (Tal como venía).
        chatMessage.setFecha(LocalDateTime.now());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage registrarNuevoUsuarioConectado(@Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        // Agregar username a los atributos de sesión del socket
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getEmisor());
            log.info("{} se ha UNIDO a los WebSockets multihilo.", chatMessage.getEmisor());
        }

        chatMessage.setFecha(LocalDateTime.now());
        return chatMessage;
    }
}
