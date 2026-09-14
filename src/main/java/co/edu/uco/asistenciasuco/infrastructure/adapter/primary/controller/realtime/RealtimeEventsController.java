package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeStreamGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canal HTTP SSE de la vertical realtime.
 *
 * <p>Spring MVC adapta el {@link Flux} como streaming {@code text/event-stream}. La fuente es
 * reactiva, pero el stack servlet sigue realizando escrituras HTTP bloqueantes mediante el
 * {@code AsyncTaskExecutor} configurado por Spring Boot. En este proyecto dicho executor usa
 * virtual threads porque {@code spring.threads.virtual.enabled=true}.</p>
 *
 * <p>Seguridad: {@code /api/v1/realtime/**} conserva autenticacion Bearer. No se aceptan tokens
 * por query string ni se hace publico el canal para simplificar al cliente.</p>
 */
@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeEventsController {

    static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(25);

    private final RealtimeStreamGateway realtimeStreamGateway;

    public RealtimeEventsController(final RealtimeStreamGateway realtimeStreamGateway) {
        this.realtimeStreamGateway = Objects.requireNonNull(
                realtimeStreamGateway,
                "RealtimeStreamGateway es requerido."
        );
    }

    /**
     * Canal SSE permanente.
     *
     * <p>Los comentarios heartbeat mantienen actividad en conexiones ociosas sin contaminar el
     * flujo de eventos de negocio ni las metricas del publisher.</p>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEventResponse>> subscribe() {
        final Flux<ServerSentEvent<RealtimeEventResponse>> businessEvents =
                realtimeStreamGateway.subscribe().map(RealtimeEventsController::toServerSentEvent);

        final Flux<ServerSentEvent<RealtimeEventResponse>> heartbeats =
                Flux.interval(HEARTBEAT_INTERVAL)
                        .map(sequence -> ServerSentEvent.<RealtimeEventResponse>builder()
                                .comment("heartbeat")
                                .build());

        return Flux.merge(businessEvents, heartbeats);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "activeSubscribers", realtimeStreamGateway.activeSubscribersCount(),
                "status", "ONLINE"
        )));
    }

    /**
     * Utilidad diagnostica restringida a ADMINISTRADOR.
     *
     * <p>No sustituye la publicacion desde casos de uso reales y no debe usarse como API de
     * negocio.</p>
     */
    @PostMapping("/emit")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> emitCustomEvent(
            @RequestBody final Map<String, Object> payload
    ) {
        final String topic = Objects.toString(payload.get("topic"), "GENERAL");
        final String action = Objects.toString(payload.get("action"), "CUSTOM_EVENT");
        final Map<String, Object> data = extractData(payload.get("data"));

        realtimeStreamGateway.emit(topic + "." + action, data);

        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "mensajeUsuario",
                "Evento reactivo emitido a "
                        + realtimeStreamGateway.activeSubscribersCount()
                        + " suscriptores."
        )));
    }

    private static Map<String, Object> extractData(final Object rawData) {
        if (rawData == null) {
            return Map.of();
        }
        if (!(rawData instanceof Map<?, ?> rawMap)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo data debe ser un objeto JSON."
            );
        }

        final Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Las claves del campo data deben ser texto."
                );
            }
            data.put(key, entry.getValue());
        }
        return Map.copyOf(data);
    }

    private static ServerSentEvent<RealtimeEventResponse> toServerSentEvent(final RealtimeEventResponse event) {
        return ServerSentEvent.<RealtimeEventResponse>builder()
                .id(event.eventId().toString())
                .event(event.type())
                .data(event)
                .build();
    }
}
