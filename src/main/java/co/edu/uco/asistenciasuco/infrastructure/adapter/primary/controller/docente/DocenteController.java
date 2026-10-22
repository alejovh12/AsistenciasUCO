package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.ConsultarAsignacionesAcademicasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.ConsultarDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.ConsultarDocentePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.primaryports.dto.ConsultarDocentePorIdDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper.DocenteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarAsignacionesAcademicasDocenteRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ConsultarDocentePorIdRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.ConsultarAsignacionesAcademicasDocenteRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.validation.ConsultarDocentePorIdRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
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

    private final ConsultarDocentesInputPort consultarDocentesInputPort;
    private final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort;
    private final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort;

    public DocenteController(
            final ConsultarDocentesInputPort consultarDocentesInputPort,
            final ConsultarDocentePorIdInputPort consultarDocentePorIdInputPort,
            final ConsultarAsignacionesAcademicasDocenteInputPort consultarAsignacionesAcademicasDocenteInputPort
    ) {
        this.consultarDocentesInputPort = Objects.requireNonNull(consultarDocentesInputPort, "El puerto de entrada ConsultarDocentesInputPort es obligatorio.");
        this.consultarDocentePorIdInputPort = Objects.requireNonNull(consultarDocentePorIdInputPort, "El puerto de entrada ConsultarDocentePorIdInputPort es obligatorio.");
        this.consultarAsignacionesAcademicasDocenteInputPort = Objects.requireNonNull(consultarAsignacionesAcademicasDocenteInputPort, "El puerto de entrada ConsultarAsignacionesAcademicasDocenteInputPort es obligatorio.");
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
}
