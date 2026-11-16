package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.primaryports.ReenviarActivacionCuentaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.AccesoCuentaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation.AccesoCuentaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo.response.MensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * IDENT-002: reenvio del enlace de activacion de una cuenta registrada por otra persona. El correo viaja en el cuerpo,
 * nunca en la URL. El alcance (administrador, decano de su facultad, coordinador de su programa) se valida en Application
 * y el perfil, la cuenta pendiente y la espera minima en la base de datos; el PEP/PDP autoriza la ruta por rol.
 */
@RestController
@RequestMapping("/api/v1/identidad/cuentas/activacion")
public final class ActivacionCuentaController {

    private static final AccesoCuentaRequestValidator VALIDATOR = new AccesoCuentaRequestValidator();

    private final ReenviarActivacionCuentaInputPort reenviarActivacionCuentaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ActivacionCuentaController(
            final ReenviarActivacionCuentaInputPort reenviarActivacionCuentaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.reenviarActivacionCuentaInputPort = Objects.requireNonNull(reenviarActivacionCuentaInputPort,
                "ReenviarActivacionCuentaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/reenvios")
    @AuditableOperation(action = "REENVIAR_ACTIVACION_CUENTA", resourceType = "USUARIO")
    public ResponseEntity<ApiDataResponse<MensajeResponse>> reenviar(@Valid @RequestBody final AccesoCuentaRequest request) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        final String correo = Objects.requireNonNull(request, "La informacion de la cuenta es obligatoria.").getCorreo();
        final String mensaje = reenviarActivacionCuentaInputPort.reenviar(authenticatedUserResolver.requireAuthenticatedUserId(), correo);
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MensajeResponse(mensaje)));
    }
}
