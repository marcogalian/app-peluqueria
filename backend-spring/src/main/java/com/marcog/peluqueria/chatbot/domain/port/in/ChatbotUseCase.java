package com.marcog.peluqueria.chatbot.domain.port.in;

import com.marcog.peluqueria.chatbot.domain.model.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.model.ChatResponse;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;

/**
 * Puerto de entrada del chatbot.
 *
 * El controller depende de esta interfaz, no de la implementacion concreta.
 */
public interface ChatbotUseCase {

    ChatResponse chat(ChatRequest request, CustomUserDetails userDetails);
}
