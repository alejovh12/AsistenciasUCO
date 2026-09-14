package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeStreamGateway;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeEventsControllerTest {

    private final RealtimeStreamGateway gateway = mock(RealtimeStreamGateway.class);
    private final RealtimeEventsController controller = new RealtimeEventsController(gateway);

    @Test
    void subscribe_produce_serverSentEvent_conservando_id_y_type_del_evento() {
        final RealtimeEventResponse response = new RealtimeEventResponse(
                UUID.randomUUID(),
                "ASISTENCIA_REGISTRADA",
                Instant.now(),
                "corr-1",
                Map.of("estudiante", "123")
        );
        when(gateway.subscribe()).thenReturn(Flux.just(response));

        StepVerifier.create(controller.subscribe())
                .assertNext(sse -> {
                    assertEquals(response.eventId().toString(), sse.id());
                    assertEquals("ASISTENCIA_REGISTRADA", sse.event());
                    assertEquals(response, sse.data());
                })
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_emite_varios_eventos_en_orden() {
        final RealtimeEventResponse first = response("A");
        final RealtimeEventResponse second = response("B");
        when(gateway.subscribe()).thenReturn(Flux.just(first, second));

        StepVerifier.create(controller.subscribe())
                .expectNextMatches(sse -> first.eventId().toString().equals(sse.id()))
                .expectNextMatches(sse -> second.eventId().toString().equals(sse.id()))
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_emite_heartbeat_como_comentario_sse() {
        when(gateway.subscribe()).thenReturn(Flux.never());

        StepVerifier.withVirtualTime(controller::subscribe)
                .thenAwait(RealtimeEventsController.HEARTBEAT_INTERVAL)
                .assertNext(sse -> assertEquals("heartbeat", sse.comment()))
                .thenCancel()
                .verify();
    }

    @Test
    void subscribe_delega_en_el_gateway_y_la_desconexion_no_lanza() {
        final Sinks.Many<RealtimeEventResponse> sink =
                Sinks.many().multicast().directBestEffort();
        when(gateway.subscribe()).thenReturn(sink.asFlux());

        final reactor.core.Disposable subscription = controller.subscribe().subscribe();
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

    @Test
    void emitCustomEvent_delega_en_el_gateway_con_topic_y_action_combinados() {
        when(gateway.activeSubscribersCount()).thenReturn(2);

        final var result = controller.emitCustomEvent(Map.of(
                "topic", "ASISTENCIA",
                "action", "CREADA",
                "data", Map.of("id", 42)
        ));

        verify(gateway).emit(eq("ASISTENCIA.CREADA"), eq(Map.of("id", 42)));
        assertEquals(200, result.getStatusCode().value());
        assertTrue(
                ((String) result.getBody().datos().get("mensajeUsuario"))
                        .contains("2 suscriptores")
        );
    }

    @Test
    void emitCustomEvent_sin_topic_ni_action_usa_valores_por_defecto() {
        controller.emitCustomEvent(Map.of());

        verify(gateway).emit(eq("GENERAL.CUSTOM_EVENT"), eq(Map.of()));
    }

    @Test
    void emitCustomEvent_rechaza_data_que_no_sea_objeto_json() {
        final ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.emitCustomEvent(Map.of("data", "no-es-un-objeto"))
        );

        assertEquals(400, exception.getStatusCode().value());
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
