package com.marcog.peluqueria;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcog.peluqueria.chatbot.domain.ChatMessageDto;
import com.marcog.peluqueria.chatbot.domain.LlmResult;
import com.marcog.peluqueria.chatbot.domain.ModeloLenguaje;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootTest
class PeluqueriaApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestModeloLenguajeConfig {

        @Bean
        ModeloLenguaje modeloLenguaje() {
            return new ModeloLenguaje() {
                @Override
                public LlmResult generateContent(String systemInstruction,
                                                 List<ChatMessageDto> history,
                                                 String userMessage,
                                                 List<Map<String, Object>> tools) {
                    return new LlmResult("IA deshabilitada en tests", null, null, "test");
                }

                @Override
                public LlmResult sendFunctionResponse(String modelUsed,
                                                      String systemInstruction,
                                                      List<ChatMessageDto> history,
                                                      String userMessage,
                                                      String functionName,
                                                      JsonNode functionArgsOriginal,
                                                      String functionResult,
                                                      List<Map<String, Object>> tools) {
                    return new LlmResult("IA deshabilitada en tests", null, null, "test");
                }
            };
        }
    }
}
