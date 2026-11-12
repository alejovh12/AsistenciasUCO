package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.AprobarSolicitudSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request.CancelarSesionCoordinadorRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response.SolicitudSesionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response.SolicitudSesionResueltaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.AprobarSolicitudSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.validation.CancelarSesionCoordinadorRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataMensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * DEC-065 (2): solicitudes de sesion extraordinaria del docente con aval previo del coordinador. El rol y el ambito (programas del
 * grupo) los fuerza la DB: 403 fuera de ambito ({@code SEC_002}), 409 si la solicitud no existe ({@code SES_025}), ya fue resuelta
 * ({@code SES_024}) o la sesion no admite la operacion ({@code SES_023}), 400 si el motivo no es del catalogo ({@code SES_019}).
 */
@RestController
@RequestMapping("/api/v1/coordinador/sesiones")
public final class CoordinadorSesionSolicitudController {

    private static final AprobarSolicitudSesionRequestValidator APROBAR_VALIDATOR = new AprobarSolicitudSesionRequestValidator();
    private static final CancelarSesionCoordinadorRequestValidator RECHAZAR_VALIDATOR = new CancelarSesionCoordinadorRequestValidator();

    private final ConsultarSolicitudesSesionInputPort consultar;
    private final AprobarSolicitudSesionInputPort aprobar;
    private final RechazarSolicitudSesionInputPort rechazar;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CoordinadorSesionSolicitudController(
            final ConsultarSolicitudesSesionInputPort consultar,
            final AprobarSolicitudSesionInputPort aprobar,
            final RechazarSolicitudSesionInputPort rechazar,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultar = Objects.requireNonNull(consultar, "ConsultarSolicitudesSesionInputPort es obligatorio.");
        this.aprobar = Objects.requireNonNull(aprobar, "AprobarSolicitudSesionInputPort es obligatorio.");
        this.rechazar = Objects.requireNonNull(rechazar, "RechazarSolicitudSesionInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<ApiPageResponse<SolicitudSesionResponse>> consultar(
            @RequestParam(name = "soloPendientes", defaultValue = "true") final boolean soloPendientes,
            @RequestParam(name = "programaId", required = false) final UUID programaId,
            @RequestParam(name = "grupoId", required = false) final UUID grupoId,
            @RequestParam(name = "pagina", defaultValue = "1") final int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") final int tamano
    ) {
        final ConsultarSolicitudesSesionInputPort.Resultado resultado = consultar.execute(new ConsultarSolicitudesSesionInputPort.Comando(
                authenticatedUserResolver.requireAuthenticatedUserId(), soloPendientes, programaId, grupoId, pagina, tamano));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(SolicitudSesionResponse::from).toList(),
                resultado.pagina(), resultado.tamano(), resultado.total()));
    }

    @PostMapping("/{sesionId}/aprobacion")
    @AuditableOperation(action = "APROBAR_SOLICITUD_SESION", resourceType = "SESION", resourceIdPathVariable = "sesionId")
    public ResponseEntity<ApiDataMensajeResponse<SolicitudSesionResueltaResponse>> aprobar(
            @PathVariable final UUID sesionId,
            @Valid @RequestBody(required = false) final AprobarSolicitudSesionRequest request
    ) {
        RequestValidationGuard.validate(APROBAR_VALIDATOR.validate(request));
        final String observacion = request == null ? null : request.getObservacion();
        final AprobarSolicitudSesionInputPort.Resultado resultado = aprobar.execute(new AprobarSolicitudSesionInputPort.Comando(
                sesionId, observacion, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, SolicitudSesionResueltaResponse.from(resultado),
                resultado.mensajeUsuario()));
    }

    @PostMapping("/{sesionId}/rechazo")
    @AuditableOperation(action = "RECHAZAR_SOLICITUD_SESION", resourceType = "SESION", resourceIdPathVariable = "sesionId")
    public ResponseEntity<ApiDataMensajeResponse<SolicitudSesionResueltaResponse>> rechazar(
            @PathVariable final UUID sesionId,
            @Valid @RequestBody(required = false) final CancelarSesionCoordinadorRequest request
    ) {
        RequestValidationGuard.validate(RECHAZAR_VALIDATOR.validate(request));
        final RechazarSolicitudSesionInputPort.Resultado resultado = rechazar.execute(new RechazarSolicitudSesionInputPort.Comando(
                sesionId, request.getCodigoMotivo().strip(), request.getObservacion(),
                authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, SolicitudSesionResueltaResponse.from(resultado),
                resultado.mensajeUsuario()));
    }
}
