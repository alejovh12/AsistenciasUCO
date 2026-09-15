package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Representacion HTTP/JSON de un evento realtime, serializada como dato de cada
 * Server-Sent Event. Vive fuera de {@code infrastructure.adapter.primary.controller} para que
 * los controllers nunca necesiten importar {@code application.secondaryports.realtime.RealtimeEvent}
 * directamente (ver {@link RealtimeStreamGateway} y ArchUnit
 * {@code ControllersMustDependOnlyOnInputPortsTest}).
 */
public record RealtimeEventResponse(
        UUID eventId,
        String type,
        Instant occurredAt,
        String correlationId,
        Map<String, Object> payload
) {

    public static RealtimeEventResponse from(final RealtimeEvent event) {
        return new RealtimeEventResponse(
                event.eventId(),
                event.type(),
                event.occurredAt(),
                event.correlationId(),
                event.payload()
        );
    }
}
