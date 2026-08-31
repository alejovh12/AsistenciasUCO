package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper.AsistenciaHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.validation.ConsultarAsistenciasPorGrupoRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para consultas de asistencias.
 */
@RestController
@Profile("mock")
public final class AsistenciaQueryController {

    private static final ConsultarAsistenciasPorGrupoRequestValidator CONSULT_BY_GROUP_VALIDATOR =
            new ConsultarAsistenciasPorGrupoRequestValidator();

    private final ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort;

    public AsistenciaQueryController(
            final ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort
    ) {

        this.consultarAsistenciasPorGrupoInputPort = Objects.requireNonNull(consultarAsistenciasPorGrupoInputPort, "El puerto de entrada ConsultarAsistenciasPorGrupoInputPort es obligatorio.");
    }

    @GetMapping("/api/v1/grupos/{grupoId}/asistencias")
    public ResponseEntity<ApiListResponse<AsistenciaConsultadaDTO>> consultarAsistenciasPorGrupo(
            @PathVariable final UUID grupoId,
            @RequestParam(required = false) final UUID sesionId
    ) {
        final ConsultarAsistenciasPorGrupoRequest request = new ConsultarAsistenciasPorGrupoRequest();
        request.setGrupo(grupoId);
        request.setSesion(sesionId);
        return executeConsultarAsistenciasPorGrupo(request);
    }

    /**
     * @deprecated Compatibilidad temporal.
     * Consumir GET /api/v1/grupos/{grupoId}/asistencias.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/api/v1/asistencias/consultas/grupo")
    public ResponseEntity<ApiListResponse<AsistenciaConsultadaDTO>> consultarAsistenciasPorGrupoLegacy(
            @RequestBody final ConsultarAsistenciasPorGrupoRequest request
    ) {
        return executeConsultarAsistenciasPorGrupo(request);
    }

    private ResponseEntity<ApiListResponse<AsistenciaConsultadaDTO>> executeConsultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRequest request
    ) {
        RequestValidationGuard.validate(CONSULT_BY_GROUP_VALIDATOR.validate(request));
        final ConsultarAsistenciasPorGrupoDTO dto = AsistenciaHttpMapper.toApplicationDTO(request);
        final List<AsistenciaConsultadaDTO> asistencias = consultarAsistenciasPorGrupoInputPort.execute(dto);

        return ResponseEntity.ok(new ApiListResponse<>(true, asistencias, asistencias.size()));
    }
}
