package com.marcog.peluqueria.chatbot.infrastructure.out.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.chatbot.domain.model.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    public GeminiClient(@Qualifier("geminiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GeminiResult generateContent(String systemInstruction, List<ChatMessageDto> history,
                                         String userMessage, List<Map<String, Object>> tools) {
        try {
            ObjectNode body = mapper.createObjectNode();

            // System instruction
            ObjectNode sysInstr = mapper.createObjectNode();
            ArrayNode sysParts = mapper.createArrayNode();
            sysParts.add(mapper.createObjectNode().put("text", systemInstruction));
            sysInstr.set("parts", sysParts);
            body.set("system_instruction", sysInstr);

            // Contents (history + new message)
            ArrayNode contents = mapper.createArrayNode();
            if (history != null) {
                for (ChatMessageDto msg : history) {
                    ObjectNode content = mapper.createObjectNode();
                    content.put("role", "model".equals(msg.getRole()) ? "model" : "user");
                    ArrayNode parts = mapper.createArrayNode();
                    parts.add(mapper.createObjectNode().put("text", msg.getContent()));
                    content.set("parts", parts);
                    contents.add(content);
                }
            }
            ObjectNode userContent = mapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = mapper.createArrayNode();
            userParts.add(mapper.createObjectNode().put("text", userMessage));
            userContent.set("parts", userParts);
            contents.add(userContent);
            body.set("contents", contents);

            // Tools (function declarations)
            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsNode = mapper.createArrayNode();
                ObjectNode toolObj = mapper.createObjectNode();
                ArrayNode funcDecls = mapper.valueToTree(tools);
                toolObj.set("function_declarations", funcDecls);
                toolsNode.add(toolObj);
                body.set("tools", toolsNode);
            }

            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = mapper.readTree(response.getBody());

            JsonNode candidate = root.path("candidates").path(0).path("content").path("parts").path(0);

            if (candidate.has("functionCall")) {
                JsonNode fc = candidate.get("functionCall");
                String funcName = fc.get("name").asText();
                JsonNode args = fc.get("args");
                return new GeminiResult(null, funcName, args);
            }

            String text = candidate.path("text").asText("");
            return new GeminiResult(text, null, null);

        } catch (Exception e) {
            log.error("Error llamando a Gemini API: {}", e.getMessage());
            return new GeminiResult("Lo siento, no puedo responder ahora. Inténtalo de nuevo.", null, null);
        }
    }

    public GeminiResult sendFunctionResponse(String systemInstruction, List<ChatMessageDto> history,
                                              String userMessage, String functionName, String functionResult,
                                              List<Map<String, Object>> tools) {
        try {
            ObjectNode body = mapper.createObjectNode();

            ObjectNode sysInstr = mapper.createObjectNode();
            ArrayNode sysParts = mapper.createArrayNode();
            sysParts.add(mapper.createObjectNode().put("text", systemInstruction));
            sysInstr.set("parts", sysParts);
            body.set("system_instruction", sysInstr);

            ArrayNode contents = mapper.createArrayNode();
            if (history != null) {
                for (ChatMessageDto msg : history) {
                    ObjectNode content = mapper.createObjectNode();
                    content.put("role", "model".equals(msg.getRole()) ? "model" : "user");
                    ArrayNode parts = mapper.createArrayNode();
                    parts.add(mapper.createObjectNode().put("text", msg.getContent()));
                    content.set("parts", parts);
                    contents.add(content);
                }
            }

            // User message
            ObjectNode userContent = mapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode userParts = mapper.createArrayNode();
            userParts.add(mapper.createObjectNode().put("text", userMessage));
            userContent.set("parts", userParts);
            contents.add(userContent);

            // Model function call
            ObjectNode modelContent = mapper.createObjectNode();
            modelContent.put("role", "model");
            ArrayNode modelParts = mapper.createArrayNode();
            ObjectNode fcPart = mapper.createObjectNode();
            ObjectNode fc = mapper.createObjectNode();
            fc.put("name", functionName);
            fc.set("args", mapper.createObjectNode());
            fcPart.set("functionCall", fc);
            modelParts.add(fcPart);
            modelContent.set("parts", modelParts);
            contents.add(modelContent);

            // Function response
            ObjectNode funcRespContent = mapper.createObjectNode();
            funcRespContent.put("role", "function");
            ArrayNode funcParts = mapper.createArrayNode();
            ObjectNode frPart = mapper.createObjectNode();
            ObjectNode fr = mapper.createObjectNode();
            fr.put("name", functionName);
            ObjectNode responseObj = mapper.createObjectNode();
            responseObj.put("result", functionResult);
            fr.set("response", responseObj);
            frPart.set("functionResponse", fr);
            funcParts.add(frPart);
            funcRespContent.set("parts", funcParts);
            contents.add(funcRespContent);

            body.set("contents", contents);

            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsNode = mapper.createArrayNode();
                ObjectNode toolObj = mapper.createObjectNode();
                ArrayNode funcDecls = mapper.valueToTree(tools);
                toolObj.set("function_declarations", funcDecls);
                toolsNode.add(toolObj);
                body.set("tools", toolsNode);
            }

            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            return new GeminiResult(text, null, null);

        } catch (Exception e) {
            log.error("Error en function response a Gemini: {}", e.getMessage());
            return new GeminiResult("No pude procesar la información. Inténtalo de nuevo.", null, null);
        }
    }

    public record GeminiResult(String text, String functionName, JsonNode functionArgs) {
        public boolean isFunctionCall() { return functionName != null; }
    }
}
