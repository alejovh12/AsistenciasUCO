package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.SolicitarRevisionAsistenciaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Adaptador primario REST para operaciones relacionadas con asistencias.
 *
 * <p>CULM-001 D.7 (TD-009): {@code POST /api/v1/asistencias} (registro individual) se retiro; la asistencia se registra
 * con {@code POST /asistencias/lote} o por toma ({@code POST /tomas/{id}/asistencias}).</p>
 */
@RestController
@RequestMapping("/api/v1/asistencias")
public final class AsistenciaController {

    private static final SolicitarRevisionAsistenciaRequestValidator REQUEST_REVISION_VALIDATOR = new SolicitarRevisionAsistenciaRequestValidator();

    private final RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort;
    private final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AsistenciaController(
            final RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort,
            final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.registrarAsistenciasSesionInputPort = Objects.requireNonNull(registrarAsistenciasSesionInputPort,
                "RegistrarAsistenciasSesionInputPort es obligatorio.");
        this.solicitarRevisionAsistenciaInputPort = Objects.requireNonNull(solicitarRevisionAsistenciaInputPort,
                "SolicitarRevisionAsistenciaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/revisiones")
    @AuditableOperation(action = "SOLICITAR_REVISION_ASISTENCIA", resourceType = "SESION", resourceIdRequestField = "sesionId")
    public ResponseEntity<ApiMessageResponse> solicitarRevision(
            @Valid @RequestBody final SolicitarRevisionAsistenciaRequest request
    ) {
        RequestValidationGuard.validate(REQUEST_REVISION_VALIDATOR.validate(request));
        final SolicitarRevisionAsistenciaDTO dto = AsistenciaHttpMapper.toApplicationDTO(
                request,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        solicitarRevisionAsistenciaInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiMessageResponse(true, "Solicitud de revision registrada correctamente."));
    }

    @PostMapping("/lote")
    @AuditableOperation(action = "REGISTRAR_ASISTENCIAS_SESION", resourceType = "SESION", resourceIdRequestField = "sesionId")
    public ResponseEntity<ApiMessageResponse> registrarAsistenciasLote(
            @Valid @RequestBody final RegistrarAsistenciasSesionRequest request
    ) {
        final RegistrarAsistenciasSesionDTO dto = AsistenciaHttpMapper.toApplicationDTO(
                request,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        registrarAsistenciasSesionInputPort.execute(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Asistencias de sesion registradas correctamente."));
    }
}
