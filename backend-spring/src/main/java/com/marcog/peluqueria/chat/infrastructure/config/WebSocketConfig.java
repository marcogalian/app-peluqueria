package com.marcog.peluqueria.chat.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de Servidor de WebSockets (Chat Interno Multihilo)
 * 
 * Implementación "Enterprise" utilizando STOMP (Simple Text Oriented Message
 * Protocol)
 * sobre WebSockets. Esto delega la gestión de hilos (Thread Pools) y sesiones
 * abiertas
 * a Spring Boot automáticamente en lugar de gestionar ciclos "while"
 * rudimentarios.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar un 'broker' de memoria simple para llevar los mensajes del servidor
        // a los clientes subscritos a un canal
        config.enableSimpleBroker("/topic");

        // Prefijo para enviar mensajes DESDE los clientes HACIA el servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Exponemos el túnel oficial, la conexión base en
        // ws://localhost:8080/chat-websocket
        // Se permite SockJS como un Fallback brutal si el navegador del cliente no
        // soporta WebSockets estándar
        registry.addEndpoint("/chat-websocket")
                .setAllowedOrigins("http://localhost:5173") // Permitimos conectar a React (Vite)
                .withSockJS();
    }
}
