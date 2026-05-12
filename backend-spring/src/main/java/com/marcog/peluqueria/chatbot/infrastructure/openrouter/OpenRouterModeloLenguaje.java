package com.marcog.peluqueria.chatbot.infrastructure.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.chatbot.domain.ChatMessageDto;
import com.marcog.peluqueria.chatbot.domain.LlmResult;
import com.marcog.peluqueria.chatbot.domain.ModeloLenguaje;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP compatible con OpenAI Chat Completions contra OpenRouter.
 *
 * Permite probar el router gratuito openrouter/free sin cambiar el caso de uso
 * del asistente ni el frontend.
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter")
@Slf4j
public class OpenRouterModeloLenguaje implements ModeloLenguaje {

    private static final String OPENROUTER_CHAT_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String ERROR_RESPONSE = "Lo siento, no puedo responder ahora. Inténtalo de nuevo.";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model:openrouter/free}")
    private String model;

    @Value("${openrouter.site.url:http://localhost:3000}")
    private String siteUrl;

    @Value("${openrouter.app.name:Peluqueria Isabella}")
    private String appName;

    public OpenRouterModeloLenguaje(@Qualifier("openRouterRestTemplate") RestTemplate restTemplate,
                                    ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    @Override
    public LlmResult generateContent(String systemInstruction,
                                     List<ChatMessageDto> history,
                                     String userMessage,
                                     List<Map<String, Object>> tools) {
        try {
            ObjectNode body = construirCuerpoBase(systemInstruction, history, userMessage, tools);
            return enviarPeticion(model, body);
        } catch (Exception ex) {
            log.error("Error llamando a OpenRouter: {}", abreviarMensajeError(ex));
            return new LlmResult(ERROR_RESPONSE, null, null, model);
        }
    }

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
            String modelo = modeloUtilizado == null || modeloUtilizado.isBlank() ? model : modeloUtilizado;
            ObjectNode body = construirCuerpoBase(systemInstruction, history, userMessage, tools);
            ArrayNode messages = (ArrayNode) body.get("messages");

            String toolCallId = "call_" + functionName;
            messages.add(construirMensajeToolCall(toolCallId, functionName, functionArgsOriginales));
            messages.add(construirMensajeToolResponse(toolCallId, functionName, functionResult));

            return enviarPeticion(modelo, body);
        } catch (Exception ex) {
            log.error("Error procesando function response con OpenRouter: {}", abreviarMensajeError(ex));
            return new LlmResult("No pude procesar la información. Inténtalo de nuevo.", null, null, model);
        }
    }

    private ObjectNode construirCuerpoBase(String systemInstruction,
                                           List<ChatMessageDto> history,
                                           String userMessage,
                                           List<Map<String, Object>> tools) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(construirMensajeTexto("system", systemInstruction));
        if (history != null) {
            for (ChatMessageDto mensaje : history) {
                String role = "model".equals(mensaje.getRole()) || "assistant".equals(mensaje.getRole())
                        ? "assistant"
                        : "user";
                messages.add(construirMensajeTexto(role, mensaje.getContent()));
            }
        }
        messages.add(construirMensajeTexto("user", userMessage));
        body.set("messages", messages);

        if (tools != null && !tools.isEmpty()) {
            body.set("tools", convertirTools(tools));
            body.put("tool_choice", "auto");
        }

        return body;
    }

    private ObjectNode construirMensajeTexto(String role, String content) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private ObjectNode construirMensajeToolCall(String toolCallId, String functionName, JsonNode argsOriginales) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "assistant");
        message.putNull("content");

        ArrayNode toolCalls = mapper.createArrayNode();
        ObjectNode toolCall = mapper.createObjectNode();
        toolCall.put("id", toolCallId);
        toolCall.put("type", "function");

        ObjectNode function = mapper.createObjectNode();
        function.put("name", functionName);
        function.put("arguments", argsOriginales == null ? "{}" : argsOriginales.toString());
        toolCall.set("function", function);
        toolCalls.add(toolCall);

        message.set("tool_calls", toolCalls);
        return message;
    }

    private ObjectNode construirMensajeToolResponse(String toolCallId, String functionName, String functionResult) {
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("name", functionName);
        message.put("content", functionResult == null ? "" : functionResult);
        return message;
    }

    private ArrayNode convertirTools(List<Map<String, Object>> tools) {
        ArrayNode toolsNode = mapper.createArrayNode();
        for (Map<String, Object> tool : tools) {
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.put("type", "function");

            ObjectNode function = mapper.createObjectNode();
            function.put("name", String.valueOf(tool.get("name")));
            function.put("description", String.valueOf(tool.getOrDefault("description", "")));
            function.set("parameters", mapper.valueToTree(tool.get("parameters")));

            wrapper.set("function", function);
            toolsNode.add(wrapper);
        }
        return toolsNode;
    }

    private LlmResult enviarPeticion(String modelo, ObjectNode body) throws Exception {
        body.put("model", modelo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", siteUrl);
        headers.set("X-Title", appName);

        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(
                OPENROUTER_CHAT_URL, HttpMethod.POST, entity, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");

        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray() && !toolCalls.isEmpty()) {
            JsonNode function = toolCalls.path(0).path("function");
            return new LlmResult(
                    null,
                    function.path("name").asText(),
                    leerArgumentos(function.path("arguments")),
                    root.path("model").asText(modelo)
            );
        }

        return new LlmResult(
                message.path("content").asText(""),
                null,
                null,
                root.path("model").asText(modelo)
        );
    }

    private JsonNode leerArgumentos(JsonNode argumentsNode) {
        if (argumentsNode == null || argumentsNode.isMissingNode() || argumentsNode.isNull()) {
            return mapper.createObjectNode();
        }
        if (argumentsNode.isObject()) {
            return argumentsNode;
        }
        String raw = argumentsNode.asText("{}");
        try {
            return mapper.readTree(raw);
        } catch (Exception ex) {
            log.warn("OpenRouter devolvio argumentos no JSON: {}", raw);
            return mapper.createObjectNode();
        }
    }

    private String abreviarMensajeError(Exception ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null) return "unknown";
        return mensaje.substring(0, Math.min(120, mensaje.length()));
    }
}
