package com.marcog.peluqueria.chatbot.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcog.peluqueria.chatbot.domain.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.ChatResponse;
import com.marcog.peluqueria.chatbot.domain.LlmResult;
import com.marcog.peluqueria.chatbot.domain.ContextoNegocio;
import com.marcog.peluqueria.chatbot.domain.ConsultasGestionPeluqueria;
import com.marcog.peluqueria.chatbot.domain.ModeloLenguaje;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso principal del asistente de gestion.
 *
 * Orquesta la conversacion con el modelo y ejecuta las funciones de negocio que
 * el modelo decida invocar. Los permisos se aplican por rol en las tools
 * disponibles y de nuevo en el ejecutor de funciones.
 */
@Service
@RequiredArgsConstructor
public class ResponderConsultasGestion implements ConversarConAsistente, RegenerarContextoNegocio {

    // ── Dependencias (solo puertos) ─────────────────────────────────
    private final ModeloLenguaje llmClient;
    private final ContextoNegocio contextLoader;
    private final ConsultasGestionPeluqueria functionExecutor;
    private final PeluqueroRepository peluqueroRepository;

    private static final int MAX_ITERACIONES_FUNCTION_CALLING = 3;
    private static final Locale LOCALE_ES = Locale.of("es", "ES");
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
            - getCitasProgramadas(periodo?): citas previstas del negocio para admin o propias para empleado. Periodo: hoy, semana, mes
            - getVacacionesEmpleado(): vacaciones del empleado autenticado
            - getEmpleados(): listado y estado operativo del equipo
            - Solo admin: getVacacionesEmpleadoPorNombre(nombre), getVacacionesEmpleados
            - Solo admin: getGanancias, getResultados, getCitasAtendidas, getProductosStockBajo, getProductosMasVendidos, getInventario, getClientesVip, getTotalClientes

            MAPA DE CAPACIDADES DEL PROYECTO:
            - Agenda y citas: listar citas de hoy, manana, semana o mes, citas propias, citas del salon, estados, clientes y servicios asociados.
            - Clientes: total de clientes, clientes VIP, clientes activos o archivados, historial clinico y fotos si el usuario pide datos concretos disponibles.
            - Empleados: numero de empleados, nombres, especialidades, horarios, disponibilidad, bajas y vacaciones.
            - Servicios: catalogo, precios, duracion, categoria, genero y descripcion.
            - Inventario y productos: productos, precios, stock, stock minimo, bajo stock, ranking de ventas y ventas por genero.
            - Ventas, resultados y finanzas: ingresos, gastos, beneficio, ticket medio, tasa de cancelacion, top servicios y top empleados. Solo admin.
            - Ausencias: vacaciones, bajas, asuntos propios, solicitudes aprobadas, pendientes, rechazadas o canceladas.
            - Configuracion: datos del centro, horario, telefono, email, direccion, politicas, formas de pago, ofertas y dias especiales.
            - Mensajes y auditoria: puedes orientar sobre el modulo; no inventes historiales si no hay funcion disponible.

            IMPORTANTE: Si el admin pregunta por clientes (cuantos hay, VIP, nombres), DEBES llamar a getTotalClientes o getClientesVip. NUNCA digas que no tienes acceso a clientes — siempre llama la funcion.

            REGLAS IMPORTANTES:
            - Solo puedes responder sobre temas relacionados con la gestion de Peluqueria Isabella:
              citas, clientes, empleados, servicios, productos, inventario, ventas, vacaciones,
              mensajes, horarios, politicas, ofertas y datos operativos del salon.
            - Si el usuario pregunta sobre programacion, tecnologia general, criptomonedas,
              politica, deportes, recetas u otros temas ajenos al salon, responde exactamente:
              "Solo puedo ayudarte con informacion relacionada con la gestion de Peluqueria Isabella."
            - Responde SIEMPRE en espanol, amable y conciso (2-3 frases para respuestas simples)
            - No uses emoticonos ni emojis
            - No uses Markdown visible como **negritas**. Si necesitas listar, usa lineas simples con guiones
            - Para preguntas sobre empleados, servicios, productos, precios, horarios, ofertas, politicas: USA EL JSON DE CONTEXTO, NO digas que no sabes
            - Para citas, vacaciones, ausencias, dinero, ventas, stock actual: LLAMA a la funcion correspondiente
            - Si existe una funcion para el dato solicitado, usala antes de responder. Si no existe, responde solo con el contexto disponible y explica brevemente el limite.
            - No inventes datos
            - Al final, sugiere 2-3 preguntas relacionadas: [SUGERENCIAS]: ["pregunta1", "pregunta2", "pregunta3"]

            Contexto del negocio:
            """;

    // ── Casos de uso ────────────────────────────────────────────────

    @Override
    public ChatResponse chat(ChatRequest request, CustomUserDetails userDetails) {
        boolean isAdmin = esAdmin(userDetails);
        UUID peluqueroId = resolverPeluqueroDelUsuario(userDetails);

        String systemInstruction = construirSystemInstruction(isAdmin);
        List<Map<String, Object>> tools = buildToolDeclarations(isAdmin);

        LlmResult resultadoModelo;
        resultadoModelo = llmClient.generateContent(
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
        reply = limpiarEstiloRespuesta(cleanSuggestions(reply));

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

    private String construirSystemInstruction(boolean isAdmin) {
        LocalDate hoy = LocalDate.now();
        String fechaHoy = hoy.toString();
        String diaSemana = hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);

        return SYSTEM_PROMPT
                + "\nFecha actual: " + fechaHoy + " (" + diaSemana + ")\n"
                + "Cuando el usuario diga 'hoy', usa esta fecha. Para 'mañana' suma un dia.\n\n"
                + "Rol del usuario: " + (isAdmin ? "ADMIN" : "EMPLEADO") + ".\n"
                + reglasPorRol(isAdmin)
                + contextoPermitidoPorRol(isAdmin);
    }

    private String reglasPorRol(boolean isAdmin) {
        if (isAdmin) {
            return "Puede consultar datos globales del negocio, empleados, clientes y economia.\n\n";
        }
        return """
                El usuario es un empleado, no administrador. No tiene acceso a datos economicos, clientes, \
                inventario/stock, ventas globales, informacion de otros empleados del equipo, \
                vacaciones o bajas de otros empleados, citas de otros empleados ni metricas globales.
                Solo puede consultar sus propias citas y sus propias vacaciones, ademas de \
                informacion publica del negocio: servicios, precios, politicas, horario y ofertas.
                Cuando le expliques sus limitaciones, dirigete al usuario en segunda persona: \
                "solo puedes consultar tus propias citas y vacaciones", nunca uses primera persona.

                """;
    }

    private String contextoPermitidoPorRol(boolean isAdmin) {
        String contexto = contextLoader.getContext();
        if (isAdmin) return contexto;
        try {
            JsonNode raiz = MAPPER.readTree(contexto);
            if (raiz.isObject()) {
                var objeto = (com.fasterxml.jackson.databind.node.ObjectNode) raiz.deepCopy();
                objeto.remove("equipo");
                objeto.remove("totalEmpleados");
                objeto.remove("productos");
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(objeto);
            }
        } catch (Exception ignored) {
            return "{}";
        }
        return contexto;
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
        tools.add(Map.of(
                "name", "getCitasProgramadas",
                "description", "Obtiene las citas programadas. Para admin devuelve todas las citas del negocio; para empleado solo sus propias citas.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "periodo", Map.of("type", "string", "description", "Periodo: hoy, manana, semana o mes")
                        )
                )
        ));
        if (!isAdmin) return tools;

        // ── Funciones solo para admin ───────────────────────────────
        tools.add(Map.of(
                "name", "getEmpleados",
                "description", "Obtiene el listado de empleados con especialidad, horario, disponibilidad, baja y vacaciones",
                "parameters", Map.of("type", "object", "properties", Map.of())
        ));
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
                "name", "getResultados",
                "description", "Obtiene KPIs de resultados: ingresos, ticket medio, tasa de cancelacion, top servicios y top empleados",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "periodo", Map.of("type", "string", "description", "Periodo: semana, mes, trimestre o anio")
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
        tools.add(Map.of(
                "name", "getVacacionesEmpleadoPorNombre",
                "description", "Obtiene las vacaciones y ausencias de un empleado concreto por nombre",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "nombre", Map.of("type", "string", "description", "Nombre o parte del nombre del empleado")
                        ),
                        "required", List.of("nombre")
                )
        ));
        tools.add(Map.of(
                "name", "getVacacionesEmpleados",
                "description", "Obtiene el resumen de vacaciones y ausencias de todos los empleados",
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

    private String limpiarEstiloRespuesta(String reply) {
        if (reply == null) return "";
        return reply
                .replace("**", "")
                .replace("__", "")
                .replaceAll("[\\p{So}\\uFE0F\\u200D]", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
    }
}
