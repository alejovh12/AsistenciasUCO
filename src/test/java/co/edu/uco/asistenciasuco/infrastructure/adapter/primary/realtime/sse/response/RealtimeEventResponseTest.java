package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeEventResponseTest {

    @Test
    void occurredAt_se_serializa_como_iso_8601_utc_con_z() {
        final Instant occurredAt = Instant.parse("2026-09-23T18:52:28.123Z");
        final RealtimeEvent event = new RealtimeEvent(
                UUID.randomUUID(),
                "ASISTENCIAS_SESION_ACTUALIZADAS",
                occurredAt,
                "correlation-id",
                null,
                null,
                Map.of("grupo", UUID.randomUUID().toString())
        );

        final String json = JsonMapper.builder().build().writeValueAsString(RealtimeEventResponse.from(event));

        assertTrue(json.contains("\"occurredAt\":\"2026-09-23T18:52:28.123Z\""), json);
    }
}
