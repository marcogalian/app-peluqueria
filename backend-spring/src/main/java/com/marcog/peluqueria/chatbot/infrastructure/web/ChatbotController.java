package com.marcog.peluqueria.chatbot.infrastructure.web;

import com.marcog.peluqueria.chatbot.domain.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.ChatResponse;
import com.marcog.peluqueria.chatbot.application.ConversarConAsistente;
import com.marcog.peluqueria.chatbot.application.RegenerarContextoNegocio;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chatbot", description = "Asistente virtual IA con Gemini")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    // Solo dependencias de puertos de entrada (use cases). Sin acoplamiento a infra.
    private final ConversarConAsistente conversarConAsistente;
    private final RegenerarContextoNegocio regenerarContextoNegocio;

    @Operation(summary = "Enviar mensaje al chatbot",
            description = "Envia un mensaje y recibe respuesta del asistente IA")
    @ApiResponse(responseCode = "200", description = "Respuesta generada")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HAIRDRESSER')")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(conversarConAsistente.chat(request, userDetails));
    }

    @Operation(summary = "Regenerar contexto",
            description = "Regenera el contexto de negocio del chatbot (solo admin)")
    @ApiResponse(responseCode = "200", description = "Contexto regenerado")
    @PostMapping("/regenerar-contexto")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> regenerarContexto() {
        regenerarContextoNegocio.regenerar();
        return ResponseEntity.ok("Contexto regenerado correctamente");
    }
}
