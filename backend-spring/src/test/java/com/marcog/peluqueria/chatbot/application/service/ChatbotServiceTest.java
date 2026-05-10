package com.marcog.peluqueria.chatbot.application.service;

import com.marcog.peluqueria.chatbot.domain.model.ChatRequest;
import com.marcog.peluqueria.chatbot.domain.model.ChatResponse;
import com.marcog.peluqueria.chatbot.domain.model.LlmResult;
import com.marcog.peluqueria.chatbot.domain.port.out.BusinessContextPort;
import com.marcog.peluqueria.chatbot.domain.port.out.ChatFunctionExecutorPort;
import com.marcog.peluqueria.chatbot.domain.port.out.LlmClientPort;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.port.out.PeluqueroRepositoryPort;
import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.infrastructure.config.CustomUserDetails;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
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
class ChatbotServiceTest {

    @Mock private LlmClientPort llmClient;
    @Mock private BusinessContextPort contextLoader;
    @Mock private ChatFunctionExecutorPort functionExecutor;
    @Mock private PeluqueroRepositoryPort peluqueroRepository;

    @InjectMocks
    private ChatbotService chatbotService;

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

        ChatResponse respuesta = chatbotService.chat(
                peticionConMensaje("Hola"), empleadoUser);

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

        ChatResponse respuesta = chatbotService.chat(
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

        chatbotService.chat(peticionConMensaje("test"), adminUser);

        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() > 5));
    }

    @Test
    @DisplayName("Consulta clientes VIP: responde con nombres desde funcion directa sin depender del modelo")
    void chat_clientesVip_respuestaDirectaConNombres() {
        when(functionExecutor.execute(eq("getClientesVip"), any(), any(), eq(true)))
                .thenReturn("""
                        {"totalVip":2,"clientes":[
                          {"nombre":"Sofia Martinez","telefono":"600111222","descuento":10},
                          {"nombre":"Lucia Gomez","telefono":"600333444","descuento":15}
                        ]}
                        """);

        ChatResponse respuesta = chatbotService.chat(
                peticionConMensaje("Dame los nombres de los clientes VIP"), adminUser);

        assertEquals("Tenemos 2 clientes VIP: Sofia Martinez, Lucia Gomez.", respuesta.getReply());
        verify(functionExecutor).execute(eq("getClientesVip"), any(), any(), eq(true));
        verify(llmClient, never()).generateContent(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Empleado solo recibe 2 tools (citas + vacaciones)")
    void chat_empleado_pasaSolo2Tools() {
        when(llmClient.generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() == 2)))
                .thenReturn(new LlmResult("ok", null, null, "modelo"));

        chatbotService.chat(peticionConMensaje("test"), empleadoUser);

        verify(llmClient).generateContent(any(), any(), any(),
                argThat((List<Map<String, Object>> tools) -> tools != null && tools.size() == 2));
    }

    @Test
    @DisplayName("Las sugerencias [SUGERENCIAS]: del prompt se extraen y limpian del reply")
    void chat_extraeSugerencias() {
        String replyConSugerencias =
                "Aqui tienes el dato.\n[SUGERENCIAS]: [\"Pregunta 1\", \"Pregunta 2\"]";
        when(llmClient.generateContent(any(), any(), any(), anyList()))
                .thenReturn(new LlmResult(replyConSugerencias, null, null, "modelo"));

        ChatResponse respuesta = chatbotService.chat(
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

        ChatResponse respuesta = chatbotService.chat(
                peticionConMensaje("test"), empleadoUser);

        assertEquals("Solo respuesta sin sugerencias", respuesta.getReply());
        assertTrue(respuesta.getSuggestedQuestions().isEmpty());
    }

    @Test
    @DisplayName("regenerar() delega en el contextLoader")
    void regenerar_delegaEnContextLoader() {
        chatbotService.regenerar();

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
