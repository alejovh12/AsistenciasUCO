package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.application.model.EstadoCanal;
import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.CorreoPruebaRequest;
import co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto.NotificacionAceptadaResponse;
import co.edu.uco.notificaciones.infrastructure.web.CorrelationFilter;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operacion del canal: estado y correo de prueba. */
@RestController
@RequestMapping("/api/v1")
public class CanalController {

    private final NotificacionesUseCase notificaciones;

    public CanalController(final NotificacionesUseCase notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/estado-canal")
    public EstadoCanal estadoCanal() {
        return notificaciones.estadoCanal();
    }

    @PostMapping("/correos-prueba")
    public ResponseEntity<NotificacionAceptadaResponse> correoPrueba(@Valid @RequestBody final CorreoPruebaRequest cuerpo) {
        final Notificacion n = notificaciones.enviarCorreoPrueba(cuerpo.tipo(), cuerpo.correoDestino(), cuerpo.solicitadoPor(),
                MDC.get(CorrelationFilter.MDC_CLAVE));
        return ResponseEntity.accepted().body(new NotificacionAceptadaResponse(n.id(), n.estado().name(), n.idempotencyKey(), false));
    }
}
