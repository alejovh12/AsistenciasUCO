package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.controller;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.MensajesExito;
import org.springframework.beans.factory.annotation.Autowired;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.MensajesExitoHttp;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.validation.RealtimeEmitValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
    static final String COMENTARIO_CONECTADO = " conectado"; // Spring antepone ":" sin espacio: el wire queda ": conectado"

    private final RealtimeStreamGateway realtimeStreamGateway;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final MensajesExito mensajes;

    public RealtimeEventsController(
            final RealtimeStreamGateway realtimeStreamGateway,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this(realtimeStreamGateway, authenticatedUserResolver, null);
    }

    @Autowired // dos constructores publicos (el corto lo usan las pruebas): Spring debe usar el que resuelve las confirmaciones por catalogo
    public RealtimeEventsController(
            final RealtimeStreamGateway realtimeStreamGateway,
            final AuthenticatedUserResolver authenticatedUserResolver,
            final ResolverMensajeUsuarioInputPort resolverMensajeUsuario
    ) {
        this.mensajes = new MensajesExito(resolverMensajeUsuario);
        this.realtimeStreamGateway = Objects.requireNonNull(
                realtimeStreamGateway,
                "RealtimeStreamGateway es requerido."
        );
        this.authenticatedUserResolver = Objects.requireNonNull(
                authenticatedUserResolver,
                "AuthenticatedUserResolver es requerido."
        );
    }

    /**
     * Canal SSE permanente, scopeado por grupo.
     *
     * <p>{@code grupoId} es obligatorio: la titularidad del docente autenticado sobre ese grupo
     * se valida una unica vez al establecer la suscripcion (ver {@link RealtimeStreamGateway});
     * el stream resultante solo entrega eventos de negocio de ese grupo. Los comentarios
     * heartbeat mantienen actividad en conexiones ociosas sin contaminar el flujo de eventos de
     * negocio ni las metricas del publisher.</p>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeEventResponse>> subscribe(@RequestParam final UUID grupoId) {
        final UUID usuarioId = authenticatedUserResolver.requireAuthenticatedUserId();
        final Flux<ServerSentEvent<RealtimeEventResponse>> businessEvents =
                realtimeStreamGateway.subscribe(usuarioId, grupoId).map(RealtimeEventsController::toServerSentEvent);

        final Flux<ServerSentEvent<RealtimeEventResponse>> heartbeats =
                Flux.interval(HEARTBEAT_INTERVAL)
                        .map(sequence -> ServerSentEvent.<RealtimeEventResponse>builder()
                                .comment("heartbeat")
                                .build());

        // F-TRX-019: comentario inicial para que el cliente vea la conexion abierta sin esperar el primer heartbeat.
        final Flux<ServerSentEvent<RealtimeEventResponse>> conectado = Flux.just(ServerSentEvent.<RealtimeEventResponse>builder()
                .comment(COMENTARIO_CONECTADO)
                .build());

        return Flux.concat(conectado, Flux.merge(businessEvents, heartbeats));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "activeSubscribers", realtimeStreamGateway.activeSubscribersCount(),
                "status", "ONLINE"
        )));
    }

    /**
     * Utilidad diagnostica restringida a ADMINISTRADOR (DEC-042, DEC-044 (5)).
     *
     * <p>Cuerpo validado (CULM-001 D.9-c, F-TRX-016): {@code tipo} del catalogo de eventos de
     * {@link RealtimeAudienciaPolicy}, {@code audiencia} obligatoria y autorizada por la politica para ese tipo, y
     * {@code payload} objeto JSON. Cualquier incumplimiento responde 400 {@code VALIDATION_ERROR}. No sustituye la
     * publicacion desde casos de uso reales y no debe usarse como API de negocio.</p>
     */
    @PostMapping("/emit")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> emitCustomEvent(
            @RequestBody(required = false) final Map<String, Object> cuerpo
    ) {
        final RealtimeEmitValidator.EventoEmitido evento = RealtimeEmitValidator.validar(cuerpo);

        realtimeStreamGateway.emit(evento.tipo(), evento.payload());

        return ResponseEntity.ok(new ApiDataResponse<>(true, Map.of(
                "mensajeUsuario",
                mensajes.de(MensajesExitoHttp.EVENTO_EMITIDO, realtimeStreamGateway.activeSubscribersCount())
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
