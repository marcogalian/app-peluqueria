package com.marcog.peluqueria.chatbot.infrastructure.out.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.chatbot.domain.model.ChatMessageDto;
import com.marcog.peluqueria.chatbot.domain.model.LlmResult;
import com.marcog.peluqueria.chatbot.domain.port.out.LlmClientPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP contra Gemini API (Google AI Studio).
 *
 * Responsabilidades:
 *  - Construir payload JSON segun protocolo de generateContent
 *  - Manejar function calling de ida y vuelta (modelo -> funcion -> modelo)
 *  - Aplicar fallback automatico entre modelos cuando uno falla (429, 404, etc.)
 */
@Component
@Slf4j
public class GeminiClient implements LlmClientPort {

    // ── Configuracion ───────────────────────────────────────────────
    private final RestTemplate restTemplate;
    // ObjectMapper inyectado de Spring (no new) para reusar el bean autoconfigurado.
    private final ObjectMapper mapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
    );

    private static final String ERROR_RESPONSE =
            "Lo siento, no puedo responder ahora. Inténtalo de nuevo.";

    public GeminiClient(@Qualifier("geminiRestTemplate") RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    // ── API publica ─────────────────────────────────────────────────

    /**
     * Primera llamada al modelo. Devuelve el texto generado o un functionCall.
     * Itera por la lista de modelos fallback hasta que uno responda OK.
     */
    @Override
    public LlmResult generateContent(String systemInstruction,
                                        List<ChatMessageDto> history,
                                        String userMessage,
                                        List<Map<String, Object>> tools) {
        List<String> modelosOrdenados = construirOrdenDeModelos();

        for (String modeloActual : modelosOrdenados) {
            try {
                ObjectNode body = construirCuerpoBase(systemInstruction, history, userMessage, tools);
                LlmResult resultado = enviarPeticion(modeloActual, body);
                if (!modeloActual.equals(model)) {
                    log.info("Usando modelo fallback: {}", modeloActual);
                }
                return resultado;
            } catch (Exception ex) {
                log.warn("Error con {} ({}), probando siguiente modelo",
                        modeloActual, abreviarMensajeError(ex));
            }
        }

        return new LlmResult(ERROR_RESPONSE, null, null, null);
    }

    /**
     * Segunda llamada al modelo tras ejecutar la funcion solicitada.
     * Reproduce la conversacion completa: usuario → functionCall del modelo → functionResponse.
     * Usa el mismo modelo que respondio la primera vez para mantener coherencia.
     */
    @Override
    public LlmResult sendFunctionResponse(String modeloUtilizado,
                                              String systemInstruction,
                                              List<ChatMessageDto> history,
                                              String userMessage,
                                              String functionName,
                                              JsonNode functionArgsOriginales,
                                              String functionResult,
                                              List<Map<String, Object>> tools) {
        try {
            ObjectNode body = construirCuerpoBase(systemInstruction, history, userMessage, tools);
            ArrayNode contents = (ArrayNode) body.get("contents");

            // Turno modelo: replica el functionCall original (con args reales, no vacios)
            contents.add(construirTurnoFunctionCall(functionName, functionArgsOriginales));

            // Turno funcion: respuesta de la funcion ejecutada localmente
            contents.add(construirTurnoFunctionResponse(functionName, functionResult));

            return enviarPeticion(modeloUtilizado, body);

        } catch (Exception ex) {
            log.error("Error en function response con {}: {}", modeloUtilizado, ex.getMessage());
            return new LlmResult("No pude procesar la información. Inténtalo de nuevo.",
                    null, null, null);
        }
    }

    // ── Construccion del payload ────────────────────────────────────

    /**
     * Construye el cuerpo JSON comun: system_instruction + contents (historial + mensaje usuario) + tools.
     * El contents queda mutable para que el llamador pueda anadir turnos adicionales.
     */
    private ObjectNode construirCuerpoBase(String systemInstruction,
                                            List<ChatMessageDto> history,
                                            String userMessage,
                                            List<Map<String, Object>> tools) {
        ObjectNode body = mapper.createObjectNode();

        // System instruction
        ObjectNode systemNode = mapper.createObjectNode();
        ArrayNode systemParts = mapper.createArrayNode();
        systemParts.add(mapper.createObjectNode().put("text", systemInstruction));
        systemNode.set("parts", systemParts);
        body.set("system_instruction", systemNode);

        // Contents: historial + mensaje del usuario actual
        ArrayNode contents = mapper.createArrayNode();
        if (history != null) {
            for (ChatMessageDto mensaje : history) {
                contents.add(construirTurnoTexto(
                        "model".equals(mensaje.getRole()) ? "model" : "user",
                        mensaje.getContent()));
            }
        }
        contents.add(construirTurnoTexto("user", userMessage));
        body.set("contents", contents);

        // Tools (function declarations)
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsNode = mapper.createArrayNode();
            ObjectNode toolWrapper = mapper.createObjectNode();
            toolWrapper.set("function_declarations", mapper.valueToTree(tools));
            toolsNode.add(toolWrapper);
            body.set("tools", toolsNode);
        }

        return body;
    }

    private ObjectNode construirTurnoTexto(String role, String texto) {
        ObjectNode contenido = mapper.createObjectNode();
        contenido.put("role", role);
        ArrayNode partes = mapper.createArrayNode();
        partes.add(mapper.createObjectNode().put("text", texto));
        contenido.set("parts", partes);
        return contenido;
    }

    private ObjectNode construirTurnoFunctionCall(String functionName, JsonNode argsOriginales) {
        ObjectNode contenido = mapper.createObjectNode();
        contenido.put("role", "model");
        ArrayNode partes = mapper.createArrayNode();
        ObjectNode parteCall = mapper.createObjectNode();
        ObjectNode functionCall = mapper.createObjectNode();
        functionCall.put("name", functionName);
        // Importante: re-enviar los args originales que devolvio el modelo, no un objeto vacio
        functionCall.set("args", argsOriginales != null ? argsOriginales : mapper.createObjectNode());
        parteCall.set("functionCall", functionCall);
        partes.add(parteCall);
        contenido.set("parts", partes);
        return contenido;
    }

    private ObjectNode construirTurnoFunctionResponse(String functionName, String functionResult) {
        ObjectNode contenido = mapper.createObjectNode();
        contenido.put("role", "function");
        ArrayNode partes = mapper.createArrayNode();
        ObjectNode parteResponse = mapper.createObjectNode();
        ObjectNode functionResponse = mapper.createObjectNode();
        functionResponse.put("name", functionName);
        ObjectNode responseObj = mapper.createObjectNode();
        responseObj.put("result", functionResult);
        functionResponse.set("response", responseObj);
        parteResponse.set("functionResponse", functionResponse);
        partes.add(parteResponse);
        contenido.set("parts", partes);
        return contenido;
    }

    // ── Envio HTTP y parseo ─────────────────────────────────────────

    private LlmResult enviarPeticion(String modelo, ObjectNode body) throws Exception {
        String url = String.format(GEMINI_URL_TEMPLATE, modelo, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode candidate = root.path("candidates").path(0).path("content").path("parts").path(0);

        if (candidate.has("functionCall")) {
            JsonNode functionCall = candidate.get("functionCall");
            return new LlmResult(
                    null,
                    functionCall.get("name").asText(),
                    functionCall.get("args"),
                    modelo
            );
        }

        return new LlmResult(candidate.path("text").asText(""), null, null, modelo);
    }

    // ── Utilidades ──────────────────────────────────────────────────

    private List<String> construirOrdenDeModelos() {
        List<String> orden = new ArrayList<>();
        orden.add(model);
        for (String modeloFallback : FALLBACK_MODELS) {
            if (!modeloFallback.equals(model)) orden.add(modeloFallback);
        }
        return orden;
    }

    private String abreviarMensajeError(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null) return "unknown";
        return mensaje.substring(0, Math.min(80, mensaje.length()));
    }
}
