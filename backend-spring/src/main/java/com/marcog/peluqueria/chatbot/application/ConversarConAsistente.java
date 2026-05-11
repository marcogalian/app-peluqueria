package com.marcog.peluqueria.chatbot.application;

import com.marcog.peluqueria.chatbot.domain.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.ChatResponse;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;

/**
 * Puerto de entrada del chatbot.
 *
 * El controller depende de esta interfaz, no de la implementacion concreta.
 */
public interface ConversarConAsistente {

    ChatResponse chat(ChatRequest request, CustomUserDetails userDetails);
}
