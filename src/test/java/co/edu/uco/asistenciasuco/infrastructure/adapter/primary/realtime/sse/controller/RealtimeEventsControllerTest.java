package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.controller;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import co.edu.uco.asistenciasuco.crosscutting.validation.ValidationIssue;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeEventsControllerTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final RealtimeStreamGateway gateway = mock(RealtimeStreamGateway.class);
    private final AuthenticatedUserResolver authenticatedUserResolver = () -> USUARIO;
    private final RealtimeEventsController controller = new RealtimeEventsController(gateway, authenticatedUserResolver);

    @Test
    void subscribe_produce_serverSentEvent_conservando_id_y_type_del_evento() {
        final RealtimeEventResponse response = new RealtimeEventResponse(
                UUID.randomUUID(),
                "ASISTENCIA_REGISTRADA",
                Instant.now(),
                "corr-1",
                Map.of("estudiante", "123")
        );
        when(gateway.subscribe(USUARIO, GRUPO)).thenReturn(Flux.just(response));

        StepVerifier.create(controller.subscribe(GRUPO))
                .expectNextMatches(sse -> " conectado".equals(sse.comment()))
                .assertNext(sse -> {
                    assertEquals(response.eventId().toString(), sse.id());
                    assertEquals("ASISTENCIA_REGISTRADA", sse.event());
                    assertEquals(response, sse.data());
                })
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_resuelve_usuario_autenticado_y_delega_grupoId_en_el_gateway() {
        when(gateway.subscribe(any(), any())).thenReturn(Flux.never());

        controller.subscribe(GRUPO).subscribe().dispose();

        verify(gateway).subscribe(USUARIO, GRUPO);
    }

    @Test
    void subscribe_emite_varios_eventos_en_orden() {
        final RealtimeEventResponse first = response("A");
        final RealtimeEventResponse second = response("B");
        when(gateway.subscribe(USUARIO, GRUPO)).thenReturn(Flux.just(first, second));

        StepVerifier.create(controller.subscribe(GRUPO))
                .expectNextMatches(sse -> " conectado".equals(sse.comment()))
                .expectNextMatches(sse -> first.eventId().toString().equals(sse.id()))
                .expectNextMatches(sse -> second.eventId().toString().equals(sse.id()))
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_emite_heartbeat_como_comentario_sse() {
        when(gateway.subscribe(USUARIO, GRUPO)).thenReturn(Flux.never());

        StepVerifier.withVirtualTime(() -> controller.subscribe(GRUPO))
                .expectNextMatches(sse -> " conectado".equals(sse.comment()))
                .thenAwait(RealtimeEventsController.HEARTBEAT_INTERVAL)
                .assertNext(sse -> assertEquals("heartbeat", sse.comment()))
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_delega_en_el_gateway_y_la_desconexion_no_lanza() {
        final Sinks.Many<RealtimeEventResponse> sink =
                Sinks.many().multicast().directBestEffort();
        when(gateway.subscribe(USUARIO, GRUPO)).thenReturn(sink.asFlux());

        final reactor.core.Disposable subscription = controller.subscribe(GRUPO).subscribe();
        assertEquals(Sinks.EmitResult.OK, sink.tryEmitNext(response("VIVO")));

        subscription.dispose();

        assertEquals(
                Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER,
                sink.tryEmitNext(response("DESPUES_DE_DESCONECTAR"))
        );
    }

    @Test
    void getStatus_reporta_suscriptores_activos_y_estado_online() {
        when(gateway.activeSubscribersCount()).thenReturn(3);

        final var result = controller.getStatus();

        assertEquals(200, result.getStatusCode().value());
        assertEquals(3, result.getBody().datos().get("activeSubscribers"));
        assertEquals("ONLINE", result.getBody().datos().get("status"));
    }

    // CULM-001 D.9-c (F-TRX-016, DEC-042, DEC-044 (5)): POST /realtime/emit con cuerpo validado
    // {tipo del catalogo, audiencia de la politica de C, payload objeto JSON}.
    @Test
    void emitCustomEvent_valido_publica_el_tipo_del_catalogo_con_su_payload() {
        when(gateway.activeSubscribersCount()).thenReturn(2);

        final var result = controller.emitCustomEvent(Map.of(
                "tipo", "SESION_CANCELADA",
                "audiencia", "ESTUDIANTE_GRUPO",
                "payload", Map.of("grupo", GRUPO.toString())
        ));

        verify(gateway).emit(eq("SESION_CANCELADA"), eq(Map.of("grupo", GRUPO.toString())));
        assertEquals(200, result.getStatusCode().value());
        assertTrue(((String) result.getBody().datos().get("mensajeUsuario")).contains("2 suscriptores"));
    }

    @Test
    void emitCustomEvent_acepta_el_tipo_parameter_updated_para_el_administrador() {
        controller.emitCustomEvent(Map.of("tipo", "PARAMETER_UPDATED", "audiencia", "ADMINISTRADOR", "payload", Map.of()));

        verify(gateway).emit(eq("PARAMETER_UPDATED"), eq(Map.of()));
    }

    @Test
    void emitCustomEvent_cuerpo_vacio_responde_400_validation_error_con_los_tres_campos() {
        final RequestValidationException error = assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(Map.of()));

        assertEquals(Set.of("tipo", "audiencia", "payload"),
                error.getValidationResult().issues().stream().map(ValidationIssue::field).collect(Collectors.toSet()));
        verify(gateway, never()).emit(any(), any());
    }

    @Test
    void emitCustomEvent_tipo_desconocido_o_con_formato_topic_action_responde_400() {
        for (final String tipo : new String[]{"NO_EXISTE", "ASISTENCIA.CREADA", "sesion_cancelada", " ", "SESION_CANCELADA_X"}) {
            final Map<String, Object> cuerpo = Map.of("tipo", tipo, "audiencia", "DOCENTE_TITULAR", "payload", Map.of());
            final RequestValidationException error = assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(cuerpo));
            assertEquals("tipo", error.getValidationResult().issues().getFirst().field(), tipo);
        }
        verify(gateway, never()).emit(any(), any());
    }

    @Test
    void emitCustomEvent_sin_audiencia_o_con_audiencia_que_la_politica_no_autoriza_para_el_tipo_responde_400() {
        final Map<String, Object> sinAudiencia = Map.of("tipo", "SESION_CANCELADA", "payload", Map.of());
        assertEquals("audiencia", assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(sinAudiencia))
                .getValidationResult().issues().getFirst().field());
        // TOMA_ABIERTA es solo del titular: ESTUDIANTE_GRUPO no esta en su politica.
        final Map<String, Object> ajena = Map.of("tipo", "TOMA_ABIERTA", "audiencia", "ESTUDIANTE_GRUPO", "payload", Map.of());
        assertEquals("audiencia", assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(ajena))
                .getValidationResult().issues().getFirst().field());
        final Map<String, Object> desconocida = Map.of("tipo", "TOMA_ABIERTA", "audiencia", "TODOS", "payload", Map.of());
        assertEquals("audiencia", assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(desconocida))
                .getValidationResult().issues().getFirst().field());
        verify(gateway, never()).emit(any(), any());
    }

    @Test
    void emitCustomEvent_payload_que_no_es_objeto_json_o_con_claves_no_texto_responde_400() {
        final Map<String, Object> noObjeto = Map.of("tipo", "TOMA_ABIERTA", "audiencia", "DOCENTE_TITULAR", "payload", "texto");
        assertEquals("payload", assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(noObjeto))
                .getValidationResult().issues().getFirst().field());
        final Map<String, Object> tipoNoTexto = Map.of("tipo", 7, "audiencia", 8, "payload", Map.of());
        assertEquals(Set.of("tipo", "audiencia"), assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(tipoNoTexto))
                .getValidationResult().issues().stream().map(ValidationIssue::field).collect(Collectors.toSet()));
        final Map<Object, Object> clavesNoTexto = new java.util.HashMap<>();
        clavesNoTexto.put(1, "x");
        final Map<String, Object> conClaveNumerica = Map.of("tipo", "TOMA_ABIERTA", "audiencia", "DOCENTE_TITULAR", "payload", clavesNoTexto);
        assertEquals("payload", assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(conClaveNumerica))
                .getValidationResult().issues().getFirst().field());
        verify(gateway, never()).emit(any(), any());
    }

    @Test
    void emitCustomEvent_cuerpo_nulo_responde_400() {
        assertThrows(RequestValidationException.class, () -> controller.emitCustomEvent(null));
    }

    @Test
    void subscribe_abre_con_el_comentario_sse_conectado_antes_de_cualquier_evento() {
        when(gateway.subscribe(USUARIO, GRUPO)).thenReturn(Flux.never());

        StepVerifier.create(controller.subscribe(GRUPO))
                .assertNext(sse -> assertEquals(" conectado", sse.comment()))
                .thenCancel()
                .verify();
    }

    private static RealtimeEventResponse response(final String type) {
        return new RealtimeEventResponse(
                UUID.randomUUID(),
                type,
                Instant.now(),
                null,
                Map.of()
        );
    }
}
