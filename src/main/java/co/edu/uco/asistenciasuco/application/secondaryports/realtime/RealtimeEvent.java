package co.edu.uco.asistenciasuco.application.secondaryports.realtime;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Modelo neutral de un evento de negocio publicable en tiempo real.
 *
 * <p>No depende de ninguna tecnologia de transporte (Reactor, SSE, WebSocket, etc.) ni de
 * infraestructura de observabilidad (OpenTelemetry). {@code correlationId}, {@code traceId} y
 * {@code spanId} son opcionales porque Application no tiene forma de resolverlos por si misma
 * (viven en infraestructura); el adaptador tecnologico que implemente {@link RealtimePublisherPort}
 * es responsable de completarlos con el contexto de observabilidad vigente antes de emitir.</p>
 *
 * <p>{@code payload} debe limitarse a datos de negocio no sensibles: nunca tokens, contrasenas
 * ni identificadores de sesion de seguridad.</p>
 *
 * <p>CULM-001 C.1 (contrato 3.3, SUPUESTO-C-11): {@code destinatariosUsuario} es un metadato interno de enrutamiento, no
 * serializado (la respuesta SSE no lo copia): lista los usuarios a quienes se entrega un evento de audiencia
 * {@code DESTINATARIO}. Vacio por defecto.</p>
 */
public record RealtimeEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        String correlationId,
        String traceId,
        String spanId,
        Map<String, Object> payload,
        Set<UUID> destinatariosUsuario
) {

    public RealtimeEvent {
        Objects.requireNonNull(eventId, "El eventId del evento realtime es obligatorio.");
        Objects.requireNonNull(type, "El type del evento realtime es obligatorio.");
        Objects.requireNonNull(occurredAt, "El occurredAt del evento realtime es obligatorio.");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        destinatariosUsuario = destinatariosUsuario == null ? Set.of() : Set.copyOf(destinatariosUsuario);
    }

    /** Constructor compatible con los 7 componentes anteriores a C.1 (sin destinatarios). */
    public RealtimeEvent(
            final UUID eventId,
            final String type,
            final Instant occurredAt,
            final String correlationId,
            final String traceId,
            final String spanId,
            final Map<String, Object> payload
    ) {
        this(eventId, type, occurredAt, correlationId, traceId, spanId, payload, Set.of());
    }

    /**
     * Crea un evento de negocio con identidad y marca de tiempo generadas automaticamente.
     * {@code correlationId}/{@code traceId}/{@code spanId} quedan sin resolver: el adaptador de
     * infraestructura los completa en el momento de publicar.
     */
    public static RealtimeEvent of(final String type, final Map<String, Object> payload) {
        return new RealtimeEvent(UUID.randomUUID(), type, Instant.now(), null, null, null, payload, Set.of());
    }

    /** Evento dirigido: ademas de la audiencia por rol, llega a los usuarios indicados (audiencia DESTINATARIO). */
    public static RealtimeEvent paraDestinatarios(final String type, final Map<String, Object> payload, final Set<UUID> destinatarios) {
        return new RealtimeEvent(UUID.randomUUID(), type, Instant.now(), null, null, null, payload, destinatarios);
    }

    /**
     * Devuelve una copia de este evento con el contexto de correlacion/trazabilidad resuelto.
     * Pensado para ser usado exclusivamente por adaptadores de infraestructura.
     */
    public RealtimeEvent withObservabilityContext(
            final String resolvedCorrelationId,
            final String resolvedTraceId,
            final String resolvedSpanId
    ) {
        return new RealtimeEvent(eventId, type, occurredAt, resolvedCorrelationId, resolvedTraceId, resolvedSpanId, payload, destinatariosUsuario);
    }
}
