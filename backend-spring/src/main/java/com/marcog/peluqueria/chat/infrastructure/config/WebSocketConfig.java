package com.marcog.peluqueria.chat.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion del servidor WebSocket (chat interno multihilo).
 *
 * Implementa STOMP sobre WebSockets. Spring gestiona los thread pools y
 * sesiones abiertas (no hay loops manuales).
 *
 * Seguridad: StompJwtInterceptor valida el JWT en cada CONNECT.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtInterceptor stompJwtInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria para mensajes servidor -> clientes suscritos a /topic
        config.enableSimpleBroker("/topic");
        // Prefijo para mensajes cliente -> servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-websocket")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "http://localhost:4000"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Validar JWT en cada frame CONNECT antes de aceptar la conexion
        registration.interceptors(stompJwtInterceptor);
    }
}
