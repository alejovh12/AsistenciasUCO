package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.SolicitudNotificacion;
import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.NotificacionAceptadaResponse;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.PaginaNotificacionesResponse;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.ReintentoResponse;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.SolicitudNotificacionRequest;
import co.edu.uco.notificaciones.infrastructure.web.CorrelationFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Cola de notificaciones: recepcion idempotente, consulta administrativa y reintento. */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionesController {

    private final NotificacionesUseCase notificaciones;

    public NotificacionesController(final NotificacionesUseCase notificaciones) {
        this.notificaciones = notificaciones;
    }

    @PostMapping
    public ResponseEntity<NotificacionAceptadaResponse> registrar(
            @Valid @RequestBody final SolicitudNotificacionRequest cuerpo,
            @RequestHeader(value = "traceparent", required = false) final String traceparent
    ) {
        final String correlacion = cuerpo.idCorrelacion() != null && !cuerpo.idCorrelacion().isBlank()
                ? cuerpo.idCorrelacion() : MDC.get(CorrelationFilter.MDC_CLAVE);
        final ResultadoRegistro resultado = notificaciones.registrar(new SolicitudNotificacion(
                cuerpo.aplicacion(), cuerpo.idempotencyKey(), cuerpo.tipo(), cuerpo.idioma(), cuerpo.asunto(), cuerpo.mensaje(),
                cuerpo.destinatarios().stream().map(d -> new Destinatario(d.correo(), d.nombre())).toList(), cuerpo.datos(),
                correlacion, cuerpo.traceparent() != null && !cuerpo.traceparent().isBlank() ? cuerpo.traceparent() : traceparent));
        final var n = resultado.notificacion();
        return ResponseEntity.status(resultado.creada() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(new NotificacionAceptadaResponse(n.id(), n.estado().name(), n.idempotencyKey(), !resultado.creada()));
    }

    @GetMapping
    public PaginaNotificacionesResponse consultar(
            @RequestParam(required = false) final String estado,
            @RequestParam(required = false) final String tipo,
            @RequestParam(required = false) final String aplicacion,
            @RequestParam(defaultValue = "1") final int pagina,
            @RequestParam(defaultValue = "20") final int tamano
    ) {
        return PaginaNotificacionesResponse.desde(notificaciones.consultar(new FiltroNotificaciones(estado, tipo, aplicacion), pagina, tamano));
    }

    @PostMapping("/{id}/reintento")
    public ResponseEntity<ReintentoResponse> reintentar(@PathVariable final UUID id) {
        notificaciones.reintentar(id);
        return ResponseEntity.accepted().body(new ReintentoResponse(id, EstadoEnvio.PENDIENTE.name()));
    }
}
