package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.RegistrarDocenteDesdeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper.DocenteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.AsignarDocenteAGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarAsignacionesAcademicasDocenteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarDocentePorIdRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.RegistrarDocenteDesdeUsuarioRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.RegistrarDocenteDesdeUsuarioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.AsignarDocenteAGrupoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.ConsultarAsignacionesAcademicasDocenteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.ConsultarDocentePorIdRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.RegistrarDocenteDesdeUsuarioRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.http.HttpStatus;
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
 * Adaptador primario REST para operaciones relacionadas con docentes.
 */
@RestController
@RequestMapping("/api/v1/docentes")
public final class DocenteController {

    private static final ConsultarDocentePorIdRequestValidator CONSULT_BY_ID_VALIDATOR =
            new ConsultarDocentePorIdRequestValidator();
    private static final ConsultarAsignacionesAcademicasDocenteRequestValidator CONSULT_ASSIGNMENTS_VALIDATOR =
            new ConsultarAsignacionesAcademicasDocenteRequestValidator();
    private static final RegistrarDocenteDesdeUsuarioRequestValidator REGISTER_TEACHER_VALIDATOR =
            new RegistrarDocenteDesdeUsuarioRequestValidator();
    private static final AsignarDocenteAGrupoRequestValidator ASSIGN_TEACHER_VALIDATOR =
            new AsignarDocenteAGrupoRequestValidator();

    private final ConsultarDocentesInputPort consultarDocentesInputPort;
    private final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort;
    private final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort;
    private final RegistrarDocenteDesdeUsuarioInputPort registrarDocenteDesdeUsuarioInputPort;
    private final AsignarDocenteAGrupoInputPort asignarDocenteAGrupoInputPort;

    public DocenteController(
            final ConsultarDocentesInputPort consultarDocentesInputPort,
            final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort,
            final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort,
            final RegistrarDocenteDesdeUsuarioInputPort registrarDocenteDesdeUsuarioInputPort,
            final AsignarDocenteAGrupoInputPort asignarDocenteAGrupoInputPort
    ) {

        this.consultarDocentesInputPort = Objects.requireNonNull(consultarDocentesInputPort, "El puerto de entrada ConsultarDocentesInputPort es obligatorio.");
        this.consultarDocentePorIdInputPort = Objects.requireNonNull(consultarDocentePorIdInputPort, "El puerto de entrada ConsultarDocentePorIdInputPort es obligatorio.");
        this.consultarAsignacionesAcademicasDocenteInputPort = Objects.requireNonNull(consultarAsignacionesAcademicasDocenteInputPort, "El puerto de entrada ConsultarAsignacionesAcademicasDocenteInputPort es obligatorio.");
        this.registrarDocenteDesdeUsuarioInputPort = Objects.requireNonNull(registrarDocenteDesdeUsuarioInputPort, "El puerto de entrada RegistrarDocenteDesdeUsuarioInputPort es obligatorio.");
        this.asignarDocenteAGrupoInputPort = Objects.requireNonNull(asignarDocenteAGrupoInputPort, "El puerto de entrada AsignarDocenteAGrupoInputPort es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<List<DocenteIdentidadDTO>> consultarDocentes() {
        return ResponseEntity.ok(consultarDocentesInputPort.execute());
    }

    @GetMapping("/{docenteId}")
    public ResponseEntity<ApiDataResponse<DocenteIdentidadDTO>> consultarDocentePorId(
            @PathVariable final UUID docenteId
    ) {
        final ConsultarDocentePorIdRequest request = new ConsultarDocentePorIdRequest();
        request.setDocente(docenteId);
        return executeConsultarDocente(request);
    }

    /**
     * @deprecated Compatibilidad temporal.
     * Consumir GET /api/v1/docentes/{docenteId}.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/consultas/id")
    public ResponseEntity<ApiDataResponse<DocenteIdentidadDTO>> consultarDocentePorIdLegacy(
            @RequestBody final ConsultarDocentePorIdRequest request
    ) {
        return executeConsultarDocente(request);
    }

    private ResponseEntity<ApiDataResponse<DocenteIdentidadDTO>> executeConsultarDocente(
            final ConsultarDocentePorIdRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_BY_ID_VALIDATOR.validate(request));
        final ConsultarDocentePorIdDTO dto = DocenteHttpMapper.toApplicationDTO(request);
        final DocenteIdentidadDTO docente = consultarDocentePorIdInputPort.execute(dto);

        return ResponseEntity.ok(new ApiDataResponse<>(true, docente));
    }

    @GetMapping("/{docenteId}/asignaciones")
    public ResponseEntity<ApiListResponse<DocenteAsignacionAcademicaDTO>> consultarAsignacionesAcademicas(
            @PathVariable final UUID docenteId
    ) {
        final ConsultarAsignacionesAcademicasDocenteRequest request =
                new ConsultarAsignacionesAcademicasDocenteRequest();
        request.setDocente(docenteId);
        return executeConsultarAsignacionesAcademicas(request);
    }

    /**
     * @deprecated Compatibilidad temporal.
     * Consumir GET /api/v1/docentes/{docenteId}/asignaciones.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/consultas/asignaciones")
    public ResponseEntity<ApiListResponse<DocenteAsignacionAcademicaDTO>> consultarAsignacionesAcademicasLegacy(
            @RequestBody final ConsultarAsignacionesAcademicasDocenteRequest request
    ) {
        return executeConsultarAsignacionesAcademicas(request);
    }

    private ResponseEntity<ApiListResponse<DocenteAsignacionAcademicaDTO>> executeConsultarAsignacionesAcademicas(
            final ConsultarAsignacionesAcademicasDocenteRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_ASSIGNMENTS_VALIDATOR.validate(request));
        final ConsultarAsignacionesAcademicasDocenteDTO dto = DocenteHttpMapper.toApplicationDTO(request);
        final List<DocenteAsignacionAcademicaDTO> asignaciones =
                consultarAsignacionesAcademicasDocenteInputPort.execute(dto);

        return ResponseEntity.ok(new ApiListResponse<>(true, asignaciones, asignaciones.size()));
    }

    @PostMapping
    @AuditableOperation(
            action = "REGISTRAR_DOCENTE_DESDE_USUARIO",
            resourceType = "DOCENTE"
    )
    public ResponseEntity<RegistrarDocenteDesdeUsuarioResponse> registrarDocenteDesdeUsuario(
            @RequestBody final RegistrarDocenteDesdeUsuarioRequest request
    ) {
        RequestValidationGuard.validate(REGISTER_TEACHER_VALIDATOR.validate(request));
        final RegistrarDocenteDesdeUsuarioDTO dto = DocenteHttpMapper.toApplicationDTO(request);
        final RegistrarDocenteDesdeUsuarioResultadoDTO resultado = registrarDocenteDesdeUsuarioInputPort.execute(dto);
        if (!ObjectHelper.isNull(resultado.getDocenteId())) {
            AuditRequestAttributes.storeResourceId(resultado.getDocenteId().toString());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistrarDocenteDesdeUsuarioResponse.from(resultado));
    }

    @PostMapping("/asignaciones/grupo")
    @AuditableOperation(action = "ASIGNAR_DOCENTE_A_GRUPO", resourceType = "GRUPO", resourceIdRequestField = "grupo")
    public ResponseEntity<AsignarDocenteAGrupoResultadoDTO> asignarDocenteAGrupo(
            @RequestBody final AsignarDocenteAGrupoRequest request
    ) {
        RequestValidationGuard.validate(ASSIGN_TEACHER_VALIDATOR.validate(request));
        final AsignarDocenteAGrupoDTO dto = DocenteHttpMapper.toApplicationDTO(request);
        return ResponseEntity.ok(asignarDocenteAGrupoInputPort.execute(dto));
    }
}
