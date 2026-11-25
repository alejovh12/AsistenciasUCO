package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.redis;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REDIS-001: formato del mensaje que viaja entre replicas por el canal Redis Pub/Sub del realtime.
 *
 * <p>Es interno del backend (no es el contrato SSE que ve el cliente, que sigue siendo {@code RealtimeEventResponse}).
 * Lleva tambien los destinatarios del evento, que son metadato de enrutamiento y nunca se serializan hacia el cliente.
 * {@code v} versiona el formato: un mensaje de otra version se descarta.</p>
 */
public final class RedisRealtimeCodec {

    static final int VERSION = 1;

    private final JsonMapper mapper;

    public RedisRealtimeCodec(final JsonMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "JsonMapper es obligatorio.");
    }

    public String escribir(final RealtimeEvent event) {
        Objects.requireNonNull(event, "El evento realtime es obligatorio.");
        return mapper.writeValueAsString(new Mensaje(VERSION, event.eventId().toString(), event.type(),
                event.occurredAt().toString(), event.correlationId(), event.traceId(), event.spanId(),
                new LinkedHashMap<>(event.payload()),
                event.destinatariosUsuario().stream().map(UUID::toString).sorted().toList()));
    }

    /** Reconstruye el evento; devuelve {@code null} si el mensaje es de otra version del formato. */
    public RealtimeEvent leer(final String json) {
        final Mensaje mensaje = mapper.readValue(json, Mensaje.class);
        if (mensaje.v() != VERSION) {
            return null;
        }
        final Set<UUID> destinatarios = mensaje.destinatarios() == null ? Set.of()
                : mensaje.destinatarios().stream().map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
        return new RealtimeEvent(UUID.fromString(mensaje.eventId()), mensaje.type(), Instant.parse(mensaje.occurredAt()),
                mensaje.correlationId(), mensaje.traceId(), mensaje.spanId(), mensaje.payload(), destinatarios);
    }

    record Mensaje(int v, String eventId, String type, String occurredAt, String correlationId, String traceId,
                   String spanId, Map<String, Object> payload, List<String> destinatarios) {
    }
}
