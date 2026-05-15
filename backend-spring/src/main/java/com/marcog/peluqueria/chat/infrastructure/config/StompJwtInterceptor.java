package com.marcog.peluqueria.chat.infrastructure.config;

import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import com.marcog.peluqueria.security.infrastructure.config.JwtService;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Valida el JWT cuando un cliente STOMP envia el frame CONNECT.
 *
 * Sin este interceptor, /chat-websocket aceptaria conexiones anonimas
 * (CWE-306). El JWT viaja en el header "Authorization: Bearer ..." del frame
 * CONNECT (los WebSockets nativos no permiten cabeceras HTTP, pero STOMP si).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtInterceptor implements ChannelInterceptor {

    private static final String CABECERA_AUTH = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final JpaUserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader(CABECERA_AUTH);
        if (authHeader == null || !authHeader.startsWith(PREFIJO_BEARER)) {
            log.warn("WebSocket CONNECT rechazado: falta header Authorization");
            throw new SecurityException("Falta token JWT en el handshake STOMP");
        }

        String token = authHeader.substring(PREFIJO_BEARER.length());
        try {
            String username = jwtService.extractUsername(token);
            Optional<UserEntity> usuarioOpt = userRepository.findByUsername(username);
            if (usuarioOpt.isEmpty()) {
                throw new SecurityException("Usuario no encontrado");
            }

            CustomUserDetails userDetails = new CustomUserDetails(usuarioOpt.get());
            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new SecurityException("Token JWT invalido o caducado");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("WebSocket CONNECT rechazado: {}", ex.getMessage());
            throw new SecurityException("Token JWT invalido");
        }

        return message;
    }
}
