package com.marcog.peluqueria.chatbot.domain.port.in;

/**
 * Puerto de entrada para forzar la regeneracion del contexto del chatbot.
 *
 * Lo usa el endpoint admin POST /api/chat/regenerar-contexto cuando el catalogo
 * cambia y no se quiere esperar al cron nocturno.
 */
public interface RegenerarContextoUseCase {

    void regenerar();
}
