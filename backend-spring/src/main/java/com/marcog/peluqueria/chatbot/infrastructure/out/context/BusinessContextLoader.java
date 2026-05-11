package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class BusinessContextLoader {

    private String cachedContext = "";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("chatbot/business-context.json");
            cachedContext = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Contexto de negocio cargado desde business-context.json ({} chars)", cachedContext.length());
        } catch (Exception e) {
            log.error("No se pudo cargar chatbot/business-context.json: {}", e.getMessage());
        }
    }

    public void regenerar() {
        init();
    }

    public String getContext() {
        return cachedContext;
    }
}
