package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hub centralizado para la gestión de clientes conectados mediante Server-Sent Events (SSE).
 * Permite la difusión reactiva de cambios en tiempo real entre múltiples usuarios concurrentes.
 */
@Component
public class RealtimeEventHub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeEventHub.class);
    private static final long SSE_TIMEOUT = 180_000L; // 3 minutos

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter registerClient(final String clientId) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(clientId);
        });
        emitter.onError(ex -> emitters.remove(clientId));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .id(UUID.randomUUID().toString())
                    .data(Map.of(
                            "clientId", clientId,
                            "timestamp", Instant.now().toString(),
                            "mensaje", "Conexión reactiva en tiempo real establecida exitosamente."
                    )));
        } catch (IOException e) {
            emitters.remove(clientId);
        }

        return emitter;
    }

    public void broadcast(final String topic, final String action, final Map<String, Object> payload) {
        final String eventId = UUID.randomUUID().toString();
        final Map<String, Object> eventData = Map.of(
                "id", eventId,
                "topic", topic,
                "action", action,
                "timestamp", Instant.now().toString(),
                "data", payload
        );

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("DATA_CHANGE")
                        .id(eventId)
                        .data(eventData));
            } catch (Exception ex) {
                emitter.complete();
                emitters.remove(clientId);
            }
        });
    }

    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("PING")
                        .data(Map.of("heartbeat", Instant.now().toString())));
            } catch (Exception ex) {
                emitter.complete();
                emitters.remove(clientId);
            }
        });
    }

    public int getActiveSubscribersCount() {
        return emitters.size();
    }
}
