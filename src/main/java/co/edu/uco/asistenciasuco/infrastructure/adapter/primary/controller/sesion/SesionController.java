package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper.SesionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ActualizarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CerrarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.ConsultarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CrearSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con sesiones.
 */
@RestController
@RequestMapping("/api/v1/sesiones")
public final class SesionController {

    private static final CrearSesionRequestValidator CREATE_SESSION_VALIDATOR = new CrearSesionRequestValidator();
    private static final ConsultarSesionRequestValidator CONSULT_SESSION_VALIDATOR = new ConsultarSesionRequestValidator();
    private static final CerrarSesionRequestValidator CLOSE_SESSION_VALIDATOR = new CerrarSesionRequestValidator();

    private final CrearSesionInputPort crearSesionInputPort;
    private final ConsultarSesionInputPort consultarSesionInputPort;
    private final CerrarSesionInputPort cerrarSesionInputPort;
    private final ActualizarSesionInputPort actualizarSesionInputPort;
    private final GenerarSesionesGrupoInputPort generarSesionesGrupoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SesionController(
            final CrearSesionInputPort crearSesionInputPort,
            final ConsultarSesionInputPort consultarSesionInputPort,
            final CerrarSesionInputPort cerrarSesionInputPort,
            final ActualizarSesionInputPort actualizarSesionInputPort,
            final GenerarSesionesGrupoInputPort generarSesionesGrupoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearSesionInputPort = Objects.requireNonNull(crearSesionInputPort, "CrearSesionInputPort es obligatorio.");
        this.consultarSesionInputPort = Objects.requireNonNull(consultarSesionInputPort, "ConsultarSesionInputPort es obligatorio.");
        this.cerrarSesionInputPort = Objects.requireNonNull(cerrarSesionInputPort, "CerrarSesionInputPort es obligatorio.");
        this.actualizarSesionInputPort = Objects.requireNonNull(actualizarSesionInputPort, "ActualizarSesionInputPort es obligatorio.");
        this.generarSesionesGrupoInputPort = Objects.requireNonNull(generarSesionesGrupoInputPort, "GenerarSesionesGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "CREAR_SESION", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<ApiMessageResponse> crearSesion(@RequestBody final CrearSesionRequest request) {
        RequestValidationGuard.validate(CREATE_SESSION_VALIDATOR.validate(request));
        final CrearSesionDTO dto = SesionHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId());
        crearSesionInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesion creada correctamente."));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<ApiListResponse<Void>> consultarSesionesPorGrupo(@PathVariable final UUID grupoId) {
        throw new co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException(
                "La base publica expone consulta de sesion por id, pero no una lectura agregada por grupo lista para esta respuesta."
        );
    }

    @PostMapping("/grupo/{grupoId}/generacion")
    @AuditableOperation(action = "GENERAR_SESIONES_GRUPO", resourceType = "GRUPO", resourceIdPathVariable = "grupoId")
    public ResponseEntity<ApiMessageResponse> generarSesionesGrupo(@PathVariable final UUID grupoId) {
        generarSesionesGrupoInputPort.execute(new GenerarSesionesGrupoDTO(grupoId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesiones de grupo generadas correctamente."));
    }

    @GetMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesion(@PathVariable final UUID sesionId) {
        final ConsultarSesionRequest request = new ConsultarSesionRequest();
        request.setSesion(sesionId);
        return executeConsultarSesion(request);
    }

    @Deprecated(forRemoval = false)
    @PostMapping("/consultas")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesionLegacy(
            @RequestBody final ConsultarSesionRequest request
    ) {
        return executeConsultarSesion(request);
    }

    @PostMapping("/cierres")
    @AuditableOperation(action = "CERRAR_SESION", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> cerrarSesion(@RequestBody final CerrarSesionRequest request) {
        RequestValidationGuard.validate(CLOSE_SESSION_VALIDATOR.validate(request));
        final CerrarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request, authenticatedUserResolver.requireAuthenticatedUserId());
        cerrarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiMessageResponse(true, "Sesion cerrada correctamente."));
    }

    @PutMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<Void>> actualizarSesion(
            @PathVariable final UUID sesionId,
            @RequestBody final ActualizarSesionRequest request
    ) {
        final ActualizarSesionDTO dto = SesionHttpMapper.toApplicationDTO(
                sesionId,
                request,
                authenticatedUserResolver.requireAuthenticatedUserId()
        );
        actualizarSesionInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    private ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> executeConsultarSesion(
            final ConsultarSesionRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_SESSION_VALIDATOR.validate(request));
        final ConsultarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        final SesionConsultadaDTO sesion = consultarSesionInputPort.execute(dto);
        return ResponseEntity.ok(new ApiDataResponse<>(true, sesion));
    }

}
