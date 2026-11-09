package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CancelarSesionCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response.MotivoCancelacionSesionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response.SesionCanceladaCoordinadorResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.CancelarSesionCoordinadorRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): el coordinador del programa cancela una sesion creada manualmente por el docente. El ambito
 * (programas del grupo) y que la sesion sea manual los fuerza la DB: 403 fuera de ambito, 409 si no es manual o ya esta cancelada.
 */
@RestController
@RequestMapping("/api/v1/coordinador/sesiones")
public final class CoordinadorSesionCancelacionController {

    private static final CancelarSesionCoordinadorRequestValidator VALIDATOR = new CancelarSesionCoordinadorRequestValidator();

    private final CancelarSesionCoordinadorInputPort cancelarSesion;
    private final ConsultarMotivosCancelacionInputPort consultarMotivos;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorSesionCancelacionController(
            final CancelarSesionCoordinadorInputPort cancelarSesion,
            final ConsultarMotivosCancelacionInputPort consultarMotivos,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.cancelarSesion = Objects.requireNonNull(cancelarSesion, "CancelarSesionCoordinadorInputPort es obligatorio.");
        this.consultarMotivos = Objects.requireNonNull(consultarMotivos, "ConsultarMotivosCancelacionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/motivos-cancelacion")
    public ResponseEntity<ApiListResponse<MotivoCancelacionSesionResponse>> consultarMotivos() {
        final List<MotivoCancelacionSesionResponse> motivos = consultarMotivos.execute(
                        new ConsultarMotivosCancelacionInputPort.Comando(authenticatedUserResolver.requireAuthenticatedUserId()))
                .stream().map(MotivoCancelacionSesionResponse::from).toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, motivos, motivos.size()));
    }

    @PostMapping("/{sesionId}/cancelacion")
    @AuditableOperation(action = "CANCELAR_SESION_COORDINADOR", resourceType = "SESION", resourceIdPathVariable = "sesionId")
    public ResponseEntity<ApiDataResponse<SesionCanceladaCoordinadorResponse>> cancelar(
            @PathVariable final UUID sesionId,
            @RequestBody(required = false) final CancelarSesionCoordinadorRequest request
    ) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        final CancelarSesionCoordinadorInputPort.Resultado resultado = cancelarSesion.execute(
                new CancelarSesionCoordinadorInputPort.Comando(sesionId, request.getCodigoMotivo().strip(), request.getObservacion(),
                        authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, SesionCanceladaCoordinadorResponse.from(resultado)));
    }
}
