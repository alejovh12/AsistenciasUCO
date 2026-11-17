package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.primaryports.ConsultarResumenGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * CIERRE-W3I: totales de los grupos habilitados del docente (grupos, estudiantes activos, cupo y ocupacion), calculados por la base.
 * Ruta nueva bajo {@code /api/v1/docente/**}: el Golden Path no cambia. El perfil DOCENTE lo valida el SP (otro rol: 403 {@code SEC_001}).
 */
@RestController
@RequestMapping("/api/v1/docente/resumen-grupos")
public final class DocenteResumenGruposController {

    private final ConsultarResumenGruposDocenteInputPort consultarResumen;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteResumenGruposController(
            final ConsultarResumenGruposDocenteInputPort consultarResumen,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarResumen = Objects.requireNonNull(consultarResumen, "ConsultarResumenGruposDocenteInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiDataResponse<ResumenGruposDocenteDTO>> consultarResumen() {
        return ResponseEntity.ok(new ApiDataResponse<>(true, consultarResumen.execute(
                new ConsultarResumenGruposDocenteInputPort.Consulta(authenticatedUserResolver.requireAuthenticatedUserId()))));
    }
}
