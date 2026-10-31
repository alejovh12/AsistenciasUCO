package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo;

import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.VerificarCorreoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.request.VerificarCorreoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.response.MensajeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * IDENT-001: verificacion publica del correo. Es la unica ruta publica nueva (sin comodin {@code /public/**}); su
 * credencial es el token de un solo uso del enlace. La respuesta nunca incluye datos personales y cualquier token invalido
 * responde exactamente igual (el formato se valida en Application para no distinguir casos).
 */
@RestController
@RequestMapping("/api/v1/public/verificar-correo")
public final class VerificacionCorreoController {

    private final VerificarCorreoInputPort verificarCorreoInputPort;

    public VerificacionCorreoController(final VerificarCorreoInputPort verificarCorreoInputPort) {
        this.verificarCorreoInputPort = Objects.requireNonNull(verificarCorreoInputPort, "VerificarCorreoInputPort es obligatorio.");
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<MensajeResponse>> verificar(@RequestBody(required = false) final VerificarCorreoRequest request) {
        final String mensaje = verificarCorreoInputPort.execute(request == null ? null : request.getToken());
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MensajeResponse(mensaje)));
    }
}
