package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.RegistrarAsistenciaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.SolicitarRevisionAsistenciaRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Adaptador primario REST para operaciones relacionadas con asistencias.
 */
@RestController
@RequestMapping("/api/v1/asistencias")
public final class AsistenciaController {

    private static final RegistrarAsistenciaRequestValidator REGISTER_ATTENDANCE_VALIDATOR = new RegistrarAsistenciaRequestValidator();
    private static final SolicitarRevisionAsistenciaRequestValidator REQUEST_REVISION_VALIDATOR = new SolicitarRevisionAsistenciaRequestValidator();

    private final RegistrarAsistenciaInputPort registrarAsistenciaInputPort;
    private final RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort;
    private final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AsistenciaController(
            final RegistrarAsistenciaInputPort registrarAsistenciaInputPort,
            final RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort,
            final SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort,
            final AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.registrarAsistenciaInputPort = Objects.requireNonNull(registrarAsistenciaInputPort,
                "RegistrarAsistenciaInputPort es obligatorio.");
        this.registrarAsistenciasSesionInputPort = Objects.requireNonNull(registrarAsistenciasSesionInputPort,
                "RegistrarAsistenciasSesionInputPort es obligatorio.");
        this.solicitarRevisionAsistenciaInputPort = Objects.requireNonNull(solicitarRevisionAsistenciaInputPort,
                "SolicitarRevisionAsistenciaInputPort es obligatorio.");
        this.authenticatedUserProvider = Objects.requireNonNull(authenticatedUserProvider,
                "AuthenticatedUserProvider es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "REGISTRAR_ASISTENCIA", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> registrarAsistencia(
            @RequestBody final RegistrarAsistenciaRequest request
    ) {
        RequestValidationGuard.validate(REGISTER_ATTENDANCE_VALIDATOR.validate(request));
        final RegistrarAsistenciaDTO dto = AsistenciaHttpMapper.toApplicationDTO(request);
        registrarAsistenciaInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Asistencia registrada correctamente."));
    }

    @PostMapping("/revisiones")
    @AuditableOperation(action = "SOLICITAR_REVISION_ASISTENCIA", resourceType = "SESION", resourceIdRequestField = "sesionId")
    public ResponseEntity<ApiMessageResponse> solicitarRevision(
            @RequestBody final SolicitarRevisionAsistenciaRequest request
    ) {
        RequestValidationGuard.validate(REQUEST_REVISION_VALIDATOR.validate(request));
        final SolicitarRevisionAsistenciaDTO dto = AsistenciaHttpMapper.toApplicationDTO(
                request,
                authenticatedUserProvider.requireAuthenticatedUserId()
        );
        solicitarRevisionAsistenciaInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiMessageResponse(true, "Solicitud de revision registrada correctamente."));
    }

    @PostMapping("/lote")
    @AuditableOperation(action = "REGISTRAR_ASISTENCIAS_SESION", resourceType = "SESION", resourceIdRequestField = "sesionId")
    public ResponseEntity<ApiMessageResponse> registrarAsistenciasLote(
            @RequestBody final RegistrarAsistenciasSesionRequest request
    ) {
        final RegistrarAsistenciasSesionDTO dto = AsistenciaHttpMapper.toApplicationDTO(request);
        registrarAsistenciasSesionInputPort.execute(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Asistencias de sesion registradas correctamente."));
    }
}
