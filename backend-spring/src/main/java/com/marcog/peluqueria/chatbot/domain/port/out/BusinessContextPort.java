package com.marcog.peluqueria.chatbot.domain.port.out;

/**
 * Puerto de salida que expone el contexto estatico del negocio en formato JSON.
 *
 * El contexto se cachea en memoria y se regenera periodicamente.
 * El servicio de aplicacion solo lee, no le importa de donde sale la cadena
 * (BD, archivo estatico, servicio externo, etc.).
 */
public interface BusinessContextPort {

    /** Devuelve el contexto JSON serializado, listo para inyectarse al system prompt. */
    String getContext();

    /** Fuerza la regeneracion inmediata (lo invoca el endpoint admin). */
    void regenerar();
}
