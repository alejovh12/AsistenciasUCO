package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave;

import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.RestablecerClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.SolicitarRecuperacionClaveInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request.RestablecerClaveRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request.SolicitarRecuperacionClaveRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.response.MensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPublicEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * HU177: recuperacion publica de la contrasena. Son dos rutas publicas declaradas una por una, sin comodin:
 * <ul>
 *   <li>la solicitud responde siempre lo mismo, exista o no una cuenta con ese correo;</li>
 *   <li>la confirmacion exige el token de un solo uso del enlace y responde igual para cualquier enlace que no sirve.</li>
 * </ul>
 * Ninguna respuesta incluye datos personales. El limite de intentos por IP lo aplica el filtro registrado en
 * {@code RecuperacionClaveWiringConfiguration}.
 */
@RestController
public final class RecuperacionClaveController {

    private final SolicitarRecuperacionClaveInputPort solicitarInputPort;
    private final RestablecerClaveInputPort restablecerInputPort;

    public RecuperacionClaveController(
            final SolicitarRecuperacionClaveInputPort solicitarInputPort,
            final RestablecerClaveInputPort restablecerInputPort
    ) {
        this.solicitarInputPort = Objects.requireNonNull(solicitarInputPort, "SolicitarRecuperacionClaveInputPort es obligatorio.");
        this.restablecerInputPort = Objects.requireNonNull(restablecerInputPort, "RestablecerClaveInputPort es obligatorio.");
    }

    @PostMapping(SecurityPublicEndpoints.RECUPERACION_CLAVE_SOLICITUD)
    public ResponseEntity<ApiDataResponse<MensajeResponse>> solicitar(
            @RequestBody(required = false) final SolicitarRecuperacionClaveRequest request
    ) {
        final String mensaje = solicitarInputPort.execute(request == null ? null : request.getCorreo());
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MensajeResponse(mensaje)));
    }

    @PostMapping(SecurityPublicEndpoints.RECUPERACION_CLAVE_CONFIRMACION)
    public ResponseEntity<ApiDataResponse<MensajeResponse>> confirmar(
            @RequestBody(required = false) final RestablecerClaveRequest request
    ) {
        final String mensaje = restablecerInputPort.execute(
                request == null ? null : request.getToken(),
                request == null ? null : request.getClave());
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MensajeResponse(mensaje)));
    }
}
