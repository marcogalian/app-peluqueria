package com.marcog.peluqueria.chatbot.domain.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcog.peluqueria.chatbot.domain.model.ChatMessageDto;
import com.marcog.peluqueria.chatbot.domain.model.LlmResult;

import java.util.List;
import java.util.Map;

/**
 * Puerto de salida hacia un proveedor de LLM (Gemini, OpenAI, etc.).
 *
 * El servicio de aplicacion depende de esta interfaz, no del cliente HTTP concreto.
 * Esto permite cambiar de proveedor sin tocar la logica de negocio.
 */
public interface LlmClientPort {

    /**
     * Primera llamada al modelo. Devuelve texto generado o un functionCall si el modelo
     * decide pedir la ejecucion de una funcion declarada en {@code tools}.
     */
    LlmResult generateContent(String systemInstruction,
                               List<ChatMessageDto> history,
                               String userMessage,
                               List<Map<String, Object>> tools);

    /**
     * Segunda llamada al modelo tras ejecutar la funcion solicitada.
     * Reproduce la conversacion: usuario -> functionCall -> functionResponse.
     *
     * @param modelUsed              modelo que respondio en la primera vuelta (coherencia)
     * @param functionArgsOriginal   args reales que devolvio el modelo (no enviar vacio)
     */
    LlmResult sendFunctionResponse(String modelUsed,
                                    String systemInstruction,
                                    List<ChatMessageDto> history,
                                    String userMessage,
                                    String functionName,
                                    JsonNode functionArgsOriginal,
                                    String functionResult,
                                    List<Map<String, Object>> tools);
}
