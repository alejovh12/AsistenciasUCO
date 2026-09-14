package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.RealtimeStreamGateway;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

/**
 * Canal SSE de eventos reactivos.
 *
 * <p>{@code GET /stream} produce {@code text/event-stream} usando el soporte reactivo nativo de
 * Spring MVC para tipos de retorno {@link Flux} (no requiere spring-boot-starter-webflux; basta
 * con Reactor Core en el classpath, ver pom.xml). El controller solo depende de
 * {@link RealtimeStreamGateway} (infraestructura primaria), nunca de
 * {@code application.secondaryports.realtime} ni de {@code infrastructure.adapter.secondary}
 * directamente (ver ArchUnit {@code ControllersMustDependOnlyOnInputPortsTest}).</p>
 *
 * <p>Seguridad: este endpoint exige {@code Authorization: Bearer} igual que el resto de la API
 * (ver {@code SecurityConfig}); no se debilita para facilitar pruebas ni se acepta el token por
 * query param. Ver {@code docs/architecture/reactive-realtime.md} para la estrategia de consumo
 * desde Angular.</p>
 */
@RestController
@RequestMapping("/api/v1/realtime")
public class RealtimeEventsController {

    private final RealtimeStreamGateway realtimeStreamGateway;

    public RealtimeEventsController(final RealtimeStreamGateway realtimeStreamGateway) {
        this.realtimeStreamGateway = Objects.requireNonNull(realtimeStreamGateway, "RealtimeStreamGateway es requerido.");
    }

    /**
     * Canal SSE permanente para recibir eventos reactivos push del sistema.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEventResponse>> subscribe() {
        return realtimeStreamGateway.subscribe().map(RealtimeEventsController::toServerSentEvent);
    }

    /**
     * Métricas basicas del canal SSE.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "activeSubscribers", realtimeStreamGateway.activeSubscribersCount(),
                "status", "ONLINE"
        )));
    }

    /**
     * Utilidad de desarrollo restringida a ADMINISTRADOR (ver SecurityConfig). No es el unico
     * punto de entrada de la reactividad: ver la integracion real de negocio documentada en
     * docs/architecture/reactive-realtime.md.
     */
    @PostMapping("/emit")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> emitCustomEvent(
            @RequestBody final Map<String, Object> payload
    ) {
        final String topic = Objects.toString(payload.get("topic"), "GENERAL");
        final String action = Objects.toString(payload.get("action"), "CUSTOM_EVENT");
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());

        realtimeStreamGateway.emit(topic + "." + action, data);

        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "mensajeUsuario", "Evento reactivo emitido a " + realtimeStreamGateway.activeSubscribersCount() + " suscriptores."
        )));
    }

    private static ServerSentEvent<RealtimeEventResponse> toServerSentEvent(final RealtimeEventResponse event) {
        return ServerSentEvent.<RealtimeEventResponse>builder()
                .id(event.eventId().toString())
                .event(event.type())
                .data(event)
                .build();
    }
}
