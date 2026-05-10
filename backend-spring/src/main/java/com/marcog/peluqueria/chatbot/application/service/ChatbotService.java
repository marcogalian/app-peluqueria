package com.marcog.peluqueria.chatbot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.chatbot.domain.model.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.model.ChatResponse;
import com.marcog.peluqueria.chatbot.domain.model.LlmResult;
import com.marcog.peluqueria.chatbot.domain.port.in.ChatbotUseCase;
import com.marcog.peluqueria.chatbot.domain.port.in.RegenerarContextoUseCase;
import com.marcog.peluqueria.chatbot.domain.port.out.BusinessContextPort;
import com.marcog.peluqueria.chatbot.domain.port.out.ChatFunctionExecutorPort;
import com.marcog.peluqueria.chatbot.domain.port.out.LlmClientPort;
import com.marcog.peluqueria.peluqueros.domain.port.out.PeluqueroRepositoryPort;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de aplicacion del chatbot.
 *
 * Implementa los casos de uso (entrada) y depende solo de puertos de salida:
 *  - LlmClientPort                  → para hablar con el modelo
 *  - BusinessContextPort            → para inyectar el contexto estatico
 *  - ChatFunctionExecutorPort       → para resolver las funciones que pide el modelo
 *  - PeluqueroRepositoryPort        → para resolver el peluquero del usuario autenticado
 */
@Service
@RequiredArgsConstructor
public class ChatbotService implements ChatbotUseCase, RegenerarContextoUseCase {

    // ── Dependencias (solo puertos) ─────────────────────────────────
    private final LlmClientPort llmClient;
    private final BusinessContextPort contextLoader;
    private final ChatFunctionExecutorPort functionExecutor;
    private final PeluqueroRepositoryPort peluqueroRepository;

    private static final int MAX_ITERACIONES_FUNCTION_CALLING = 3;
    private static final Locale LOCALE_ES = new Locale("es", "ES");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            Eres el asistente virtual de Peluqueria Isabella. Respondes en espanol, de forma amable y concisa.

            CONTEXTO ESTATICO (JSON debajo): contiene TODA la informacion del negocio que cambia poco:
            - negocio: nombre, direccion, telefono, email, horario
            - politicas: cancelacion, reservas, vacaciones, formas de pago
            - equipo: lista de peluqueras con nombre, especialidad, horario
            - totalEmpleados: numero de empleados
            - servicios: catalogo completo con precios, duracion, genero, categoria, descripcion
            - productos: catalogo con precio, categoria, genero
            - ofertas: promociones activas con descuentos y fechas

            FUNCIONES (datos en tiempo real desde BD):
            - getCitasEmpleado(fecha?): citas del empleado autenticado, default hoy
            - getVacacionesEmpleado(): vacaciones del empleado autenticado
            - Solo admin: getGanancias, getCitasAtendidas, getProductosStockBajo, getProductosMasVendidos, getInventario, getClientesVip, getTotalClientes

            IMPORTANTE: Si el admin pregunta por clientes (cuantos hay, VIP, nombres), DEBES llamar a getTotalClientes o getClientesVip. NUNCA digas que no tienes acceso a clientes — siempre llama la funcion.

            REGLAS IMPORTANTES:
            - Responde SIEMPRE en espanol, amable y conciso (2-3 frases para respuestas simples)
            - Para preguntas sobre empleados, servicios, productos, precios, horarios, ofertas, politicas: USA EL JSON DE CONTEXTO, NO digas que no sabes
            - Para citas, dinero, ventas, stock actual: LLAMA a la funcion correspondiente
            - No inventes datos
            - Al final, sugiere 2-3 preguntas relacionadas: [SUGERENCIAS]: ["pregunta1", "pregunta2", "pregunta3"]

            Contexto del negocio:
            """;

    // ── Casos de uso ────────────────────────────────────────────────

    @Override
    public ChatResponse chat(ChatRequest request, CustomUserDetails userDetails) {
        boolean isAdmin = esAdmin(userDetails);
        UUID peluqueroId = resolverPeluqueroDelUsuario(userDetails);

        ChatResponse respuestaDirecta = responderConsultaDirecta(request, peluqueroId, isAdmin);
        if (respuestaDirecta != null) {
            return respuestaDirecta;
        }

        String systemInstruction = construirSystemInstruction();
        List<Map<String, Object>> tools = buildToolDeclarations(isAdmin);

        LlmResult resultadoModelo = llmClient.generateContent(
                systemInstruction, request.getHistory(), request.getMessage(), tools);

        // Bucle de function calling. Limite duro para evitar loops infinitos.
        int iteracionesRestantes = MAX_ITERACIONES_FUNCTION_CALLING;
        while (resultadoModelo.isFunctionCall() && iteracionesRestantes-- > 0) {
            String resultadoFuncion = functionExecutor.execute(
                    resultadoModelo.functionName(),
                    resultadoModelo.functionArgs(),
                    peluqueroId,
                    isAdmin);

            // Mantener coherencia: usar el mismo modelo y los args originales
            resultadoModelo = llmClient.sendFunctionResponse(
                    resultadoModelo.modelUsed(),
                    systemInstruction,
                    request.getHistory(),
                    request.getMessage(),
                    resultadoModelo.functionName(),
                    resultadoModelo.functionArgs(),
                    resultadoFuncion,
                    tools);
        }

        String reply = resultadoModelo.text() != null
                ? resultadoModelo.text()
                : "No pude generar una respuesta.";
        List<String> suggestions = extractSuggestions(reply);
        reply = cleanSuggestions(reply);

        return ChatResponse.builder()
                .reply(reply)
                .suggestedQuestions(suggestions)
                .build();
    }

    @Override
    public void regenerar() {
        contextLoader.regenerar();
    }

    // ── Helpers privados ────────────────────────────────────────────

    private boolean esAdmin(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private UUID resolverPeluqueroDelUsuario(CustomUserDetails userDetails) {
        UUID userId = userDetails.getUserEntity().getId();
        // Puede devolver null para admins que no son peluqueros: aceptado.
        return peluqueroRepository.findByUserId(userId)
                .map(peluquero -> peluquero.getId())
                .orElse(null);
    }

    private String construirSystemInstruction() {
        LocalDate hoy = LocalDate.now();
        String fechaHoy = hoy.toString();
        String diaSemana = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);

        return SYSTEM_PROMPT
                + "\nFecha actual: " + fechaHoy + " (" + diaSemana + ")\n"
                + "Cuando el usuario diga 'hoy', usa esta fecha. Para 'mañana' suma un dia.\n\n"
                + contextLoader.getContext();
    }

    private ChatResponse responderConsultaDirecta(ChatRequest request, UUID peluqueroId, boolean isAdmin) {
        String mensaje = normalizar(request.getMessage());
        if (!esConsultaClientesVip(mensaje)) {
            return null;
        }

        if (!isAdmin) {
            return ChatResponse.builder()
                    .reply("Solo el administrador puede consultar el listado de clientes VIP.")
                    .suggestedQuestions(List.of(
                            "¿Cuántas citas tengo hoy?",
                            "¿Qué vacaciones tengo aprobadas?"
                    ))
                    .build();
        }

        String resultadoFuncion = functionExecutor.execute("getClientesVip", null, peluqueroId, true);
        return formatearClientesVip(resultadoFuncion);
    }

    private boolean esConsultaClientesVip(String mensaje) {
        return mensaje.contains("cliente") && mensaje.contains("vip");
    }

    private ChatResponse formatearClientesVip(String resultadoFuncion) {
        try {
            JsonNode raiz = MAPPER.readTree(resultadoFuncion);
            if (raiz.has("error")) {
                return ChatResponse.builder()
                        .reply("No he podido consultar los clientes VIP ahora mismo.")
                        .suggestedQuestions(List.of("¿Cuántos clientes tenemos en total?", "¿Qué servicios tenemos?"))
                        .build();
            }

            int totalVip = raiz.path("totalVip").asInt(0);
            JsonNode clientes = raiz.path("clientes");
            if (totalVip == 0 || !clientes.isArray() || clientes.isEmpty()) {
                return ChatResponse.builder()
                        .reply("Ahora mismo no hay clientes VIP activos.")
                        .suggestedQuestions(List.of("¿Cuántos clientes tenemos en total?", "¿Qué servicios tenemos?"))
                        .build();
            }

            List<String> nombres = new ArrayList<>();
            clientes.forEach(cliente -> {
                String nombre = cliente.path("nombre").asText("").trim();
                if (!nombre.isBlank()) nombres.add(nombre);
            });

            String reply = "Tenemos " + totalVip + " clientes VIP: " + String.join(", ", nombres) + ".";
            return ChatResponse.builder()
                    .reply(reply)
                    .suggestedQuestions(List.of(
                            "¿Cuántos clientes tenemos en total?",
                            "¿Qué clientes tienen descuento?",
                            "¿Qué servicios son los más vendidos?"
                    ))
                    .build();
        } catch (Exception ex) {
            return ChatResponse.builder()
                    .reply("No he podido leer el listado de clientes VIP ahora mismo.")
                    .suggestedQuestions(List.of("¿Cuántos clientes tenemos en total?", "¿Qué servicios tenemos?"))
                    .build();
        }
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(LOCALE_ES);
    }

    private List<Map<String, Object>> buildToolDeclarations(boolean isAdmin) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // ── Funciones para todos los roles ──────────────────────────
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

        if (!isAdmin) return tools;

        // ── Funciones solo para admin ───────────────────────────────
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
        tools.add(Map.of(
                "name", "getClientesVip",
                "description", "Obtiene el listado de clientes VIP con nombre, telefono y descuento personalizado",
                "parameters", Map.of("type", "object", "properties", Map.of())
        ));
        tools.add(Map.of(
                "name", "getTotalClientes",
                "description", "Obtiene el numero total de clientes (activos, archivados y cuantos son VIP)",
                "parameters", Map.of("type", "object", "properties", Map.of())
        ));

        return tools;
    }

    private List<String> extractSuggestions(String reply) {
        List<String> suggestions = new ArrayList<>();
        int indiceMarcador = reply.indexOf("[SUGERENCIAS]:");
        if (indiceMarcador < 0) return suggestions;

        String sugerenciasRaw = reply.substring(indiceMarcador + "[SUGERENCIAS]:".length()).trim();
        sugerenciasRaw = sugerenciasRaw.replaceAll("[\\[\\]\"]", "");
        for (String sugerencia : sugerenciasRaw.split(",")) {
            String limpia = sugerencia.trim();
            if (!limpia.isEmpty()) suggestions.add(limpia);
        }
        return suggestions;
    }

    private String cleanSuggestions(String reply) {
        int indiceMarcador = reply.indexOf("[SUGERENCIAS]:");
        return indiceMarcador >= 0 ? reply.substring(0, indiceMarcador).trim() : reply;
    }
}
