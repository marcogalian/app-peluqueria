package com.marcog.peluqueria.chatbot.application.service;

import com.marcog.peluqueria.chatbot.domain.model.ChatMessageDto;
import com.marcog.peluqueria.chatbot.domain.model.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.model.ChatResponse;
import com.marcog.peluqueria.chatbot.infrastructure.out.context.BusinessContextLoader;
import com.marcog.peluqueria.chatbot.infrastructure.out.context.ChatFunctionExecutor;
import com.marcog.peluqueria.chatbot.infrastructure.out.gemini.GeminiClient;
import com.marcog.peluqueria.chatbot.infrastructure.out.gemini.GeminiClient.GeminiResult;
import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final GeminiClient geminiClient;
    private final BusinessContextLoader contextLoader;
    private final ChatFunctionExecutor functionExecutor;
    private final JpaPeluqueroRepository peluqueroRepository;

    private static final String SYSTEM_PROMPT = """
            Eres el asistente virtual de Peluqueria Isabella. Respondes en espanol, de forma amable y concisa.

            Tu conocimiento incluye:
            - Informacion del negocio (servicios, productos, precios, horarios) que se te proporciona como contexto
            - Acceso a datos en tiempo real mediante funciones (citas, ganancias, stock, etc.)

            Reglas:
            - Responde siempre en espanol
            - Se conciso (2-3 frases maximo para respuestas simples)
            - Si no tienes informacion suficiente, di que no la tienes
            - No inventes datos: usa solo el contexto y las funciones disponibles
            - Si te preguntan algo fuera del ambito de la peluqueria, redirige amablemente
            - Al final de cada respuesta, sugiere 2-3 preguntas relacionadas en formato JSON array con clave "sugerencias"
            - Formato de sugerencias al final: [SUGERENCIAS]: ["pregunta1", "pregunta2", "pregunta3"]

            Contexto del negocio:
            """;

    public ChatResponse chat(ChatRequest request, CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        UUID userId = userDetails.getUserEntity().getId();
        UUID peluqueroId = peluqueroRepository.findByUserId(userId)
                .map(p -> p.getId())
                .orElse(null);

        String fechaHoy = java.time.LocalDate.now().toString();
        String diaSemana = java.time.LocalDate.now().getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES"));
        String systemInstruction = SYSTEM_PROMPT
                + "\nFecha actual: " + fechaHoy + " (" + diaSemana + ")\n"
                + "Cuando el usuario diga 'hoy', usa esta fecha. Para 'mañana' suma un dia.\n\n"
                + contextLoader.getContext();
        List<Map<String, Object>> tools = buildToolDeclarations(isAdmin);

        GeminiResult result = geminiClient.generateContent(
                systemInstruction, request.getHistory(), request.getMessage(), tools);

        // Handle function calling loop (max 3 iterations)
        int maxIterations = 3;
        while (result.isFunctionCall() && maxIterations-- > 0) {
            String funcResult = functionExecutor.execute(
                    result.functionName(), result.functionArgs(), peluqueroId, isAdmin);

            result = geminiClient.sendFunctionResponse(
                    systemInstruction, request.getHistory(), request.getMessage(),
                    result.functionName(), funcResult, tools);
        }

        String reply = result.text() != null ? result.text() : "No pude generar una respuesta.";
        List<String> suggestions = extractSuggestions(reply);
        reply = cleanSuggestions(reply);

        return ChatResponse.builder()
                .reply(reply)
                .suggestedQuestions(suggestions)
                .build();
    }

    private List<Map<String, Object>> buildToolDeclarations(boolean isAdmin) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Functions for all roles
        tools.add(Map.of(
                "name", "getCitasEmpleado",
                "description", "Obtiene las citas del empleado autenticado. Si no se indica fecha, usa la de hoy.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "fecha", Map.of("type", "string", "description", "Fecha YYYY-MM-DD (opcional, default hoy)")
                        )
                )
        ));

        tools.add(Map.of(
                "name", "getVacacionesEmpleado",
                "description", "Obtiene las vacaciones y ausencias del empleado autenticado",
                "parameters", Map.of("type", "object", "properties", Map.of())
        ));

        // Admin-only functions
        if (isAdmin) {
            tools.add(Map.of(
                    "name", "getGanancias",
                    "description", "Obtiene ingresos, gastos y beneficio del negocio",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "periodo", Map.of("type", "string", "description", "Periodo: hoy, semana o mes")
                            ),
                            "required", List.of("periodo")
                    )
            ));

            tools.add(Map.of(
                    "name", "getCitasAtendidas",
                    "description", "Obtiene cuantas citas ha atendido un empleado en un periodo",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "empleadoId", Map.of("type", "string", "description", "UUID del empleado"),
                                    "periodo", Map.of("type", "string", "description", "Periodo: hoy, semana o mes")
                            ),
                            "required", List.of("empleadoId", "periodo")
                    )
            ));

            tools.add(Map.of(
                    "name", "getProductosStockBajo",
                    "description", "Obtiene los productos con stock por debajo del minimo",
                    "parameters", Map.of("type", "object", "properties", Map.of())
            ));

            tools.add(Map.of(
                    "name", "getProductosMasVendidos",
                    "description", "Obtiene el ranking de productos mas vendidos por unidades en un periodo",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "periodo", Map.of("type", "string", "description", "Periodo: hoy, semana o mes")
                            ),
                            "required", List.of("periodo")
                    )
            ));

            tools.add(Map.of(
                    "name", "getInventario",
                    "description", "Obtiene el inventario completo: numero total de productos distintos, unidades totales en stock y detalle por producto",
                    "parameters", Map.of("type", "object", "properties", Map.of())
            ));
        }

        return tools;
    }

    private List<String> extractSuggestions(String reply) {
        List<String> suggestions = new ArrayList<>();
        int idx = reply.indexOf("[SUGERENCIAS]:");
        if (idx >= 0) {
            String sugPart = reply.substring(idx + "[SUGERENCIAS]:".length()).trim();
            sugPart = sugPart.replaceAll("[\\[\\]\"]", "");
            for (String s : sugPart.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) suggestions.add(trimmed);
            }
        }
        return suggestions;
    }

    private String cleanSuggestions(String reply) {
        int idx = reply.indexOf("[SUGERENCIAS]:");
        if (idx >= 0) {
            return reply.substring(0, idx).trim();
        }
        return reply;
    }
}
