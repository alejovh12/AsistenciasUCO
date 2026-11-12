package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.controller;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * FIX-001 (RED): un cliente SSE real (EventSource) envia solo {@code Accept: text/event-stream}.
 * Los errores del canal deben conservar el status contractual de
 * {@code /api/v1/realtime/stream} (400/403) con cuerpo {@code ApiErrorResponse} JSON, en lugar de
 * degradar a 500 o a un 400 sin cuerpo por fallo de negociacion de contenido del handler.
 */
class RealtimeStreamErrorHttpTest {

    private static final String STREAM = "/api/v1/realtime/stream";
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();

    private final ReactorRealtimeAdapter realtimeAdapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
    private final ResolverAudienciaRealtimeInputPort institutionalScopePort = mock(ResolverAudienciaRealtimeInputPort.class);
    private final ResolverMensajeUsuarioInputPort resolverMensajeUsuario = mock(ResolverMensajeUsuarioInputPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(resolverMensajeUsuario.execute(any())).thenReturn(Optional.empty());
        when(institutionalScopePort.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_PROPIO))).thenReturn(new AudienciaRealtimeGrupo(true, false, false));
        when(institutionalScopePort.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO, GRUPO_AJENO))).thenReturn(new AudienciaRealtimeGrupo(false, false, false));
        final RealtimeEventsController controller = new RealtimeEventsController(
                new LocalSseRealtimeStreamGateway(realtimeAdapter, institutionalScopePort),
                () -> USUARIO
        );
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(resolverMensajeUsuario))
                .build();
    }

    @Test
    void grupo_ajeno_con_accept_event_stream_responde_403_forbidden_json_sin_abrir_stream() throws Exception {
        mockMvc.perform(get(STREAM).param("grupoId", GRUPO_AJENO.toString()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value(STREAM));

        assertEquals(0, realtimeAdapter.activeSubscribersCount());
    }

    @Test
    void grupoId_ausente_con_accept_event_stream_responde_400_con_cuerpo_de_error_y_campo() throws Exception {
        mockMvc.perform(get(STREAM).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("grupoId"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
    }

    @Test
    void grupoId_no_uuid_con_accept_event_stream_responde_400_invalid_request() throws Exception {
        mockMvc.perform(get(STREAM).param("grupoId", "no-es-uuid").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("grupoId"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_UUID"));
    }

    @Test
    void grupo_propio_con_accept_event_stream_conserva_el_stream_sse_exitoso() throws Exception {
        final MvcResult result = mockMvc.perform(
                        get(STREAM).param("grupoId", GRUPO_PROPIO.toString()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertEquals(1, realtimeAdapter.activeSubscribersCount());
        result.getRequest().getAsyncContext().complete();
    }

    // CULM-001 D.9-c (F-TRX-019, golden path 1.6.0, CONTRACT §12): un Accept incompatible responde 406 JSON, no 500.
    @Test
    void accept_application_json_responde_406_not_acceptable_con_cuerpo_de_error_sin_abrir_stream() throws Exception {
        mockMvc.perform(get(STREAM).param("grupoId", GRUPO_PROPIO.toString()).accept(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value(STREAM));

        assertEquals(0, realtimeAdapter.activeSubscribersCount());
    }

    // F-TRX-019: la primera respuesta (el comentario inicial) llega en menos de 1 s, sin esperar el heartbeat de 25 s.
    @Test
    void la_primera_respuesta_del_stream_es_el_comentario_conectado_en_menos_de_un_segundo() throws Exception {
        final long inicio = System.nanoTime();
        final MvcResult result = mockMvc.perform(
                        get(STREAM).param("grupoId", GRUPO_PROPIO.toString()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        final long limiteNanos = java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
        String cuerpo = "";
        while (!cuerpo.contains(": conectado") && System.nanoTime() - inicio < limiteNanos) {
            cuerpo = result.getResponse().getContentAsString();
            Thread.sleep(10);
        }
        assertTrue(cuerpo.contains(": conectado"), "Se esperaba el comentario SSE inicial y llego: '" + cuerpo + "'");
        assertTrue(System.nanoTime() - inicio < limiteNanos, "El comentario inicial tardo mas de 1 s");
        result.getRequest().getAsyncContext().complete();
    }

    // F-TRX-019: el GUID del grupo no distingue mayusculas y los eventos del grupo se entregan con el formato canonico.
    @Test
    void grupoId_en_mayusculas_se_acepta_y_entrega_los_eventos_del_grupo_en_formato_canonico() throws Exception {
        final MvcResult result = mockMvc.perform(
                        get(STREAM).param("grupoId", GRUPO_PROPIO.toString().toUpperCase(java.util.Locale.ROOT)).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andReturn();

        realtimeAdapter.publish(RealtimeEvent.of("TOMA_ABIERTA", java.util.Map.of("grupo", GRUPO_PROPIO.toString())));

        final long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
        String cuerpo = "";
        // El evento se escribe en varios fragmentos (event:, data:): se espera hasta que llegue tambien el dato del grupo.
        while (!(cuerpo.contains("TOMA_ABIERTA") && cuerpo.contains(GRUPO_PROPIO.toString())) && System.nanoTime() < limite) {
            cuerpo = result.getResponse().getContentAsString();
            Thread.sleep(10);
        }
        assertTrue(cuerpo.contains("event:TOMA_ABIERTA") || cuerpo.contains("event: TOMA_ABIERTA"), cuerpo);
        assertTrue(cuerpo.contains(GRUPO_PROPIO.toString()), cuerpo);
        result.getRequest().getAsyncContext().complete();
    }
}
