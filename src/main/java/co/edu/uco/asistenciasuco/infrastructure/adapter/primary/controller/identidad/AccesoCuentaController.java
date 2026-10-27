package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.AccesoCuentaInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoCuentaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.AccesoCuentaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.validation.AccesoCuentaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Estado de acceso de una cuenta registrada (ACTIVA, PENDIENTE_SINCRONIZACION, INACTIVA o PROBLEMA) y reconciliación
 * idempotente. El correo viaja en el cuerpo, nunca en la URL. El alcance (administrador, decano de su facultad,
 * coordinador de su programa) se valida en Application; el PEP/PDP autoriza la ruta por rol.
 */
@RestController
@RequestMapping("/api/v1/identidad/cuentas/acceso")
public final class AccesoCuentaController {

    private static final AccesoCuentaRequestValidator VALIDATOR = new AccesoCuentaRequestValidator();

    private final AccesoCuentaInputPort accesoCuentaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AccesoCuentaController(
            final AccesoCuentaInputPort accesoCuentaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.accesoCuentaInputPort = Objects.requireNonNull(accesoCuentaInputPort, "AccesoCuentaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/consultas")
    public ResponseEntity<ApiDataResponse<AccesoCuentaDTO>> consultar(@RequestBody final AccesoCuentaRequest request) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                accesoCuentaInputPort.consultar(authenticatedUserResolver.requireAuthenticatedUserId(), correoValidado(request))));
    }

    @PostMapping("/reconciliaciones")
    @AuditableOperation(action = "RECONCILIAR_ACCESO_CUENTA", resourceType = "USUARIO")
    public ResponseEntity<ApiDataResponse<AccesoCuentaDTO>> reconciliar(@RequestBody final AccesoCuentaRequest request) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        return ResponseEntity.ok(new ApiDataResponse<>(true,
                accesoCuentaInputPort.reconciliar(authenticatedUserResolver.requireAuthenticatedUserId(), correoValidado(request))));
    }

    /** La validacion previa rechaza una solicitud nula; esta guarda lo hace explicito antes de leer el correo. */
    private static String correoValidado(final AccesoCuentaRequest request) {
        return Objects.requireNonNull(request, "La informacion de la cuenta es obligatoria.").getCorreo();
    }
}
