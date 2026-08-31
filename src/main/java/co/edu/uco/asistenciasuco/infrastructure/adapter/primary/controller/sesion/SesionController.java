package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.dto.CerrarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.mapper.SesionHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CerrarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.ConsultarSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.request.CrearSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CerrarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.ConsultarSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.validation.CrearSesionRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para operaciones relacionadas con sesiones.
 */
@RestController
@Profile("mock")
@RequestMapping("/api/v1/sesiones")
public final class SesionController {

    private static final CrearSesionRequestValidator CREATE_SESSION_VALIDATOR = new CrearSesionRequestValidator();
    private static final ConsultarSesionRequestValidator CONSULT_SESSION_VALIDATOR = new ConsultarSesionRequestValidator();
    private static final CerrarSesionRequestValidator CLOSE_SESSION_VALIDATOR = new CerrarSesionRequestValidator();

    private final CrearSesionInputPort crearSesionInputPort;
    private final ConsultarSesionInputPort consultarSesionInputPort;
    private final CerrarSesionInputPort cerrarSesionInputPort;

    public SesionController(
            final CrearSesionInputPort crearSesionInputPort,
            final ConsultarSesionInputPort consultarSesionInputPort,
            final CerrarSesionInputPort cerrarSesionInputPort
    ) {

        this.crearSesionInputPort = Objects.requireNonNull(crearSesionInputPort, "El puerto de entrada CrearSesionInputPort es obligatorio.");
        this.consultarSesionInputPort = Objects.requireNonNull(consultarSesionInputPort, "El puerto de entrada ConsultarSesionInputPort es obligatorio.");
        this.cerrarSesionInputPort = Objects.requireNonNull(cerrarSesionInputPort, "El puerto de entrada CerrarSesionInputPort es obligatorio.");
    }

    @PostMapping
    @AuditableOperation(action = "CREAR_SESION", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<ApiMessageResponse> crearSesion(@RequestBody final CrearSesionRequest request) {
        RequestValidationGuard.validate(CREATE_SESSION_VALIDATOR.validate(request));
        final CrearSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        crearSesionInputPort.execute(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessageResponse(true, "Sesion creada correctamente."));
    }

    @GetMapping("/{sesionId}")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesion(
            @PathVariable final UUID sesionId
    ) {
        final ConsultarSesionRequest request = new ConsultarSesionRequest();
        request.setSesion(sesionId);
        return executeConsultarSesion(request);
    }

    /**
     * @deprecated Compatibilidad temporal.
     * Consumir GET /api/v1/sesiones/{sesionId}.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/consultas")
    public ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> consultarSesionLegacy(
            @RequestBody final ConsultarSesionRequest request
    ) {
        return executeConsultarSesion(request);
    }

    private ResponseEntity<ApiDataResponse<SesionConsultadaDTO>> executeConsultarSesion(
            final ConsultarSesionRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_SESSION_VALIDATOR.validate(request));
        final ConsultarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        final SesionConsultadaDTO sesion = consultarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiDataResponse<>(true, sesion));
    }

    @PostMapping("/cierres")
    @AuditableOperation(action = "CERRAR_SESION", resourceType = "SESION", resourceIdRequestField = "sesion")
    public ResponseEntity<ApiMessageResponse> cerrarSesion(@RequestBody final CerrarSesionRequest request) {
        RequestValidationGuard.validate(CLOSE_SESSION_VALIDATOR.validate(request));
        final CerrarSesionDTO dto = SesionHttpMapper.toApplicationDTO(request);
        cerrarSesionInputPort.execute(dto);

        return ResponseEntity.ok(new ApiMessageResponse(true, "Sesion cerrada correctamente."));
    }
}
