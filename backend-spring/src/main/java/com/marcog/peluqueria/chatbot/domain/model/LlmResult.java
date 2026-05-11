package com.marcog.peluqueria.chatbot.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resultado de una invocacion al modelo de lenguaje.
 *
 * @param text          texto generado por el modelo (null si decidio invocar una funcion)
 * @param functionName  nombre de la funcion que el modelo quiere ejecutar
 * @param functionArgs  argumentos de la funcion como JSON
 * @param modelUsed     identificador del modelo que respondio (para coherencia entre vueltas)
 */
public record LlmResult(String text,
                        String functionName,
                        JsonNode functionArgs,
                        String modelUsed) {

    public boolean isFunctionCall() {
        return functionName != null;
    }
}
