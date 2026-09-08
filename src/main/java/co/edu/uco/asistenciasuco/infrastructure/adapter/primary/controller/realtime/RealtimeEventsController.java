package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeEventsController {

    private final RealtimeEventHub eventHub;

    public RealtimeEventsController(final RealtimeEventHub eventHub) {
        this.eventHub = Objects.requireNonNull(eventHub, "RealtimeEventHub es requerido.");
    }

    /**
     * Canal SSE permanente para recibir eventos reactivos push del sistema.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(value = "clientId", required = false) final String clientId
    ) {
        final String effectiveClientId = (clientId != null && !clientId.isBlank())
                ? clientId
                : UUID.randomUUID().toString();
        return eventHub.registerClient(effectiveClientId);
    }

    /**
     * Endpoint para consultar métricas del canal SSE o disparar eventos manuales de sincronización.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "activeSubscribers", eventHub.getActiveSubscribersCount(),
                "status", "ONLINE"
        )));
    }

    @PostMapping("/emit")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> emitCustomEvent(
            @RequestBody final Map<String, Object> payload
    ) {
        final String topic = Objects.toString(payload.get("topic"), "GENERAL");
        final String action = Objects.toString(payload.get("action"), "CUSTOM_EVENT");
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());

        eventHub.broadcast(topic, action, data);

        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "mensajeUsuario", "Evento reactivo emitido a " + eventHub.getActiveSubscribersCount() + " suscriptores."
        )));
    }
}
