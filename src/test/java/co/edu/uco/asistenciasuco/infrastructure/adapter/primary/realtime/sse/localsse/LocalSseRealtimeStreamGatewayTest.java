package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeStreamGatewayImplTest {

    private final ReactorRealtimeAdapter realtimeAdapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
    private final LocalSseRealtimeStreamGateway gateway = new LocalSseRealtimeStreamGateway(realtimeAdapter);

    @Test
    void subscribe_mapea_el_evento_del_adapter_a_una_respuesta_http() {
        StepVerifier.create(gateway.subscribe())
                .then(() -> gateway.emit("ASISTENCIA_REGISTRADA", Map.of("estudiante", "123")))
                .assertNext(response -> {
                    assertEquals("ASISTENCIA_REGISTRADA", response.type());
                    assertEquals("123", response.payload().get("estudiante"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void activeSubscribersCount_refleja_el_conteo_del_adapter() {
        assertEquals(0, gateway.activeSubscribersCount());

        final reactor.core.Disposable subscription = gateway.subscribe().subscribe();
        try {
            assertEquals(1, gateway.activeSubscribersCount());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void emit_construye_el_evento_y_lo_publica_a_traves_del_adapter() {
        StepVerifier.create(gateway.subscribe())
                .then(() -> gateway.emit("GENERAL.CUSTOM_EVENT", Map.of("id", 42)))
                .assertNext(response -> {
                    assertEquals("GENERAL.CUSTOM_EVENT", response.type());
                    assertEquals(42, response.payload().get("id"));
                })
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
