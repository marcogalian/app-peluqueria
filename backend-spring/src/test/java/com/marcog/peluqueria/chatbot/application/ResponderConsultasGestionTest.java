package com.marcog.peluqueria.chatbot.application;

import com.marcog.peluqueria.chatbot.domain.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.ChatResponse;
import com.marcog.peluqueria.chatbot.domain.LlmResult;
import com.marcog.peluqueria.chatbot.domain.ContextoNegocio;
import com.marcog.peluqueria.chatbot.domain.ConsultasGestionPeluqueria;
import com.marcog.peluqueria.chatbot.domain.ModeloLenguaje;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio de aplicacion del chatbot.
 * Verifica function calling, filtrado por rol y parseo de sugerencias.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResponderConsultasGestionTest {

    @Mock private ModeloLenguaje llmClient;
    @Mock private ContextoNegocio contextLoader;
    @Mock private ConsultasGestionPeluqueria functionExecutor;
    @Mock private PeluqueroRepository peluqueroRepository;

    @InjectMocks
    private ResponderConsultasGestion ResponderConsultasGestion;

    private CustomUserDetails adminUser;
    private CustomUserDetails empleadoUser;

    @BeforeEach
    void setUp() {
        UserEntity admin = UserEntity.builder()
                .id(UUID.randomUUID()).username("admin").role(Role.ROLE_ADMIN).build();
        adminUser = new CustomUserDetails(admin);

        UserEntity empleado = UserEntity.builder()
                .id(UUID.randomUUID()).username("sofia").role(Role.ROLE_HAIRDRESSER).build();
        empleadoUser = new CustomUserDetails(empleado);

        when(contextLoader.getContext()).thenReturn("{\"negocio\":\"test\"}");
    }

    @Test
    @DisplayName("Respuesta de texto: no llama a ninguna funcion")
    void chat_respuestaTexto_noLlamaFunciones() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Hola, soy el asistente.", null, null, "gemini-2.5-flash"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Dime una recomendacion para organizar la agenda"), empleadoUser);

        assertEquals("Hola, soy el asistente.", respuesta.getReply());
        verify(functionExecutor, never()).execute(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("FunctionCall: ejecuta la funcion y vuelve a llamar al modelo con el resultado")
    void chat_functionCall_ejecutaFuncionYReintenta() {
        LlmResult primera = new LlmResult(null, "getCitasEmpleado", null, "gemini-2.5-flash");
        LlmResult segunda = new LlmResult("Tienes 3 citas hoy.", null, null, "gemini-2.5-flash");

        when(llmClient.generateContent(any(), any(), any(), anyList())).thenReturn(primera);
        when(functionExecutor.execute(eq("getCitasEmpleado"), any(), any(), anyBoolean()))
                .thenReturn("{\"totalCitas\":3}");
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(segunda);
        when(peluqueroRepository.findByUserId(any())).thenReturn(Optional.of(peluqueroFake()));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Que citas tengo?"), empleadoUser);

        assertEquals("Tienes 3 citas hoy.", respuesta.getReply());
        verify(functionExecutor).execute(eq("getCitasEmpleado"), any(), any(), eq(false));
        verify(llmClient).sendFunctionResponse(
                eq("gemini-2.5-flash"), any(), any(), any(),
                eq("getCitasEmpleado"), any(), eq("{\"totalCitas\":3}"), anyList());
    }

    @Test
    @DisplayName("Admin recibe mas tools que empleado (filtrado por rol)")
    void chat_admin_pasaToolsAdmin() {
        when(llmClient.generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() > 5)))
                .thenReturn(new LlmResult("ok", null, null, "modelo"));

        ResponderConsultasGestion.chat(peticionConMensaje("test"), adminUser);

        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() > 5));
    }

    @Test
    @DisplayName("Consulta clientes VIP: el modelo decide llamar a la funcion")
    void chat_clientesVip_functionCalling() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(null, "getClientesVip", null, "modelo"));
        when(functionExecutor.execute(eq("getClientesVip"), any(), any(), eq(true)))
                .thenReturn("""
                        {"totalVip":2,"clientes":[
                          {"nombre":"Sofia Martinez","telefono":"600111222","descuento":10},
                          {"nombre":"Lucia Gomez","telefono":"600333444","descuento":15}
                        ]}
                        """);
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Tenemos 2 clientes VIP: Sofia Martinez y Lucia Gomez.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Dame los nombres de los clientes VIP"), adminUser);

        assertEquals("Tenemos 2 clientes VIP: Sofia Martinez y Lucia Gomez.", respuesta.getReply());
        verify(functionExecutor).execute(eq("getClientesVip"), any(), any(), eq(true));
    }

    @Test
    @DisplayName("Saludo: tambien lo responde el modelo, no un FAQ local")
    void chat_saludo_delegaAlModelo() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Hola. Soy el asistente de gestión de Peluquería Isabella.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Hola"), empleadoUser);

        assertTrue(respuesta.getReply().contains("Peluquería Isabella"));
        verify(llmClient).generateContent(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Pregunta fuera del negocio: la restriccion vive en el system prompt")
    void chat_fueraDelNegocio_seDelegaAlModeloConSystemPrompt() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(
                        "Solo puedo ayudarte con informacion relacionada con la gestion de Peluqueria Isabella.",
                        null,
                        null,
                        "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("quiero que me digas que es Spring Boot"), empleadoUser);

        assertTrue(respuesta.getReply().contains("Solo puedo ayudarte"));
        verify(llmClient).generateContent(
                argThat(system -> system != null
                        && system.contains("Solo puedes responder sobre temas relacionados")
                        && system.contains("Solo puedo ayudarte con informacion relacionada")),
                any(),
                any(),
                anyList());
    }

    @Test
    @DisplayName("Consulta total clientes: el modelo decide llamar a la funcion")
    void chat_totalClientes_functionCalling() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(null, "getTotalClientes", null, "modelo"));
        when(functionExecutor.execute(eq("getTotalClientes"), any(), any(), eq(true)))
                .thenReturn("{\"activos\":30,\"archivados\":2,\"vip\":4}");
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Hay 30 clientes activos, 4 VIP y 2 archivados.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Cuantos clientes tenemos en total?"), adminUser);

        assertEquals("Hay 30 clientes activos, 4 VIP y 2 archivados.", respuesta.getReply());
        verify(functionExecutor).execute(eq("getTotalClientes"), any(), any(), eq(true));
    }

    @Test
    @DisplayName("Admin consulta vacaciones de Sofía mediante function calling")
    void chat_vacacionesEmpleadoPorNombre_functionCalling() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(null, "getVacacionesEmpleadoPorNombre", null, "modelo"));
        when(functionExecutor.execute(eq("getVacacionesEmpleadoPorNombre"), any(), any(), eq(true)))
                .thenReturn("""
                        {"empleado":"Sofía Martínez","totalSolicitudes":1,"aprobadas":1,"pendientes":0,
                         "rechazadas":0,"canceladas":0,
                         "detalle":[{"tipo":"VACACIONES","estado":"APROBADA","fechaInicio":"2026-08-03","fechaFin":"2026-08-09","motivo":""}]}
                        """);
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Sofía Martínez tiene vacaciones aprobadas del 2026-08-03 al 2026-08-09.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Sofia por ejemplo, ha seleccionado vacaciones?"), adminUser);

        assertTrue(respuesta.getReply().contains("Sofía Martínez tiene vacaciones aprobadas"));
        assertTrue(respuesta.getReply().contains("2026-08-03"));
        verify(functionExecutor).execute(eq("getVacacionesEmpleadoPorNombre"), any(), any(), eq(true));
    }

    @Test
    @DisplayName("Admin consulta si algun empleado tiene vacaciones mediante function calling")
    void chat_vacacionesEquipo_functionCalling() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(null, "getVacacionesEmpleados", null, "modelo"));
        when(functionExecutor.execute(eq("getVacacionesEmpleados"), any(), any(), eq(true)))
                .thenReturn("""
                        {"totalEmpleados":3,"empleadosConSolicitudes":1,
                         "empleados":[{"empleado":"Sofía Martínez","totalSolicitudes":2,"aprobadas":1,"pendientes":1,"rechazadas":0,"canceladas":0,"detalle":[]}]}
                        """);
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Sí. Sofía Martínez tiene vacaciones o ausencias registradas.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("algun empleado ha seleccionado ya sus vacaciones?"), adminUser);

        assertTrue(respuesta.getReply().contains("Sí."));
        assertTrue(respuesta.getReply().contains("Sofía Martínez"));
        verify(functionExecutor).execute(eq("getVacacionesEmpleados"), any(), any(), eq(true));
    }

    @Test
    @DisplayName("Admin consulta economia mediante function calling")
    void chat_economiaAdmin_functionCalling() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(null, "getResultados", null, "modelo"));
        when(functionExecutor.execute(eq("getResultados"), any(), any(), eq(true)))
                .thenReturn("""
                        {"periodo":"mes","ingresosPeriodo":1200.0,"ticketMedio":40.0,
                         "citasCompletadas":30,"tasaCancelacion":5.0,"variacionMes":10.0,
                         "topServicios":[],"topEmpleados":[]}
                        """);
        when(llmClient.sendFunctionResponse(any(), any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Este mes llevamos 1200,00 € y 30 citas completadas.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("cuanto hemos ganado este mes"), adminUser);

        assertTrue(respuesta.getReply().contains("1200,00 €"));
        assertTrue(respuesta.getReply().contains("30 citas completadas"));
        verify(functionExecutor).execute(eq("getResultados"), any(), any(), eq(true));
    }

    @Test
    @DisplayName("Empleado no recibe tools de economia y el bloqueo lo responde el modelo")
    void chat_economiaEmpleado_bloqueadoPorToolsYPrompt() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Solo el administrador puede consultar datos económicos del negocio.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("cuanto hemos ganado este mes"), empleadoUser);

        assertTrue(respuesta.getReply().contains("Solo el administrador"));
        verify(functionExecutor, never()).execute(eq("getResultados"), any(), any(), anyBoolean());
        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools.stream()
                        .noneMatch(tool -> "getResultados".equals(tool.get("name")))));
    }

    @Test
    @DisplayName("Empleado solo recibe tools propias sin tools de admin ni empleados")
    void chat_empleado_pasaSoloToolsPropias() {
        when(llmClient.generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() == 3)))
                .thenReturn(new LlmResult("ok", null, null, "modelo"));

        ResponderConsultasGestion.chat(peticionConMensaje("test"), empleadoUser);

        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null
                        && tools.size() == 3
                        && tools.stream().noneMatch(tool -> "getEmpleados".equals(tool.get("name")))));
    }

    @Test
    @DisplayName("Empleado no recibe tools para consultar otros empleados")
    void chat_empleadoPreguntaOtrosEmpleados_sinToolsAdmin() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Solo el administrador puede consultar datos de otros empleados.", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("Sofia tiene vacaciones?"), empleadoUser);

        assertTrue(respuesta.getReply().contains("Solo el administrador"));
        verify(functionExecutor, never()).execute(eq("getVacacionesEmpleadoPorNombre"), any(), any(), anyBoolean());
        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools.stream()
                        .noneMatch(tool -> "getVacacionesEmpleadoPorNombre".equals(tool.get("name")))));
    }

    @Test
    @DisplayName("Empleado recibe contexto sin equipo ni productos de inventario")
    void chat_empleado_contextoSinDatosAdmin() {
        when(contextLoader.getContext()).thenReturn("""
                {"negocio":{"nombre":"Peluquería Isabella"},
                 "equipo":[{"nombre":"Sofía Martínez"}],
                 "totalEmpleados":3,
                 "productos":[{"nombre":"Champú","stock":10}],
                 "servicios":[{"nombre":"Corte"}]}
                """);
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("ok", null, null, "modelo"));

        ResponderConsultasGestion.chat(peticionConMensaje("que servicios hay"), empleadoUser);

        verify(llmClient).generateContent(
                argThat(system -> system != null
                        && !system.substring(system.indexOf("Rol del usuario: EMPLEADO.")).contains("Sofía Martínez")
                        && !system.substring(system.indexOf("Rol del usuario: EMPLEADO.")).contains("totalEmpleados")
                        && !system.substring(system.indexOf("Rol del usuario: EMPLEADO.")).contains("Champú")
                        && system.contains("Corte")),
                any(), any(), anyList());
    }

    @Test
    @DisplayName("Las sugerencias [SUGERENCIAS]: del prompt se extraen y limpian del reply")
    void chat_extraeSugerencias() {
        String replyConSugerencias =
                "Aqui tienes el dato.\n[SUGERENCIAS]: [\"Pregunta 1\", \"Pregunta 2\"]";
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(replyConSugerencias, null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("test"), empleadoUser);

        assertEquals("Aqui tienes el dato.", respuesta.getReply());
        assertEquals(2, respuesta.getSuggestedQuestions().size());
        assertTrue(respuesta.getSuggestedQuestions().contains("Pregunta 1"));
        assertTrue(respuesta.getSuggestedQuestions().contains("Pregunta 2"));
    }

    @Test
    @DisplayName("Sin marcador [SUGERENCIAS]: la lista de sugerencias queda vacia")
    void chat_sinSugerencias_listaVacia() {
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult("Solo respuesta sin sugerencias", null, null, "modelo"));

        ChatResponse respuesta = ResponderConsultasGestion.chat(
                peticionConMensaje("test"), empleadoUser);

        assertEquals("Solo respuesta sin sugerencias", respuesta.getReply());
        assertTrue(respuesta.getSuggestedQuestions().isEmpty());
    }

    @Test
    @DisplayName("regenerar() delega en el contextLoader")
    void regenerar_delegaEnContextLoader() {
        ResponderConsultasGestion.regenerar();

        verify(contextLoader).regenerar();
    }

    private Peluquero peluqueroFake() {
        Peluquero peluquero = new Peluquero();
        peluquero.setId(UUID.randomUUID());
        peluquero.setNombre("Sofia");
        return peluquero;
    }

    private ChatRequest peticionConMensaje(String mensaje) {
        ChatRequest peticion = new ChatRequest();
        peticion.setMessage(mensaje);
        return peticion;
    }
}
