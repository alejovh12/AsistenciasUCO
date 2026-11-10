package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.ConsultarPlanEstudioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** HU018: estructura curricular por semestres de los planes de estudio activos del estudiante autenticado. */
@RestController
@RequestMapping("/api/v1/estudiante/plan-estudio")
public final class EstudiantePlanEstudioController {

    private final ConsultarPlanEstudioEstudianteInputPort consultarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudiantePlanEstudioController(
            final ConsultarPlanEstudioEstudianteInputPort consultarInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort, "ConsultarPlanEstudioEstudianteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<PlanEstudioEstudianteDTO>> consultar() {
        final List<PlanEstudioEstudianteDTO> planes =
                consultarInputPort.execute(authenticatedUserResolver.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, planes, planes.size()));
    }
}
