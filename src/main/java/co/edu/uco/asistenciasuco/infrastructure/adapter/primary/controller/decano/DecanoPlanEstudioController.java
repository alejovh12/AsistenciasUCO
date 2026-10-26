package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU108 (parte de planes): malla de un plan de estudio de la facultad del decano. Solo lectura. Reutiliza el caso de
 * uso de asignaturas del plan, que ya admite al decano y rechaza (403) planes de programas fuera de su facultad. Vive
 * bajo {@code /api/v1/decano/**} porque la ruta del coordinador no admite al rol DECANO; {@code SecurityConfig} no cambia.
 */
@RestController
@RequestMapping("/api/v1/decano/planes-estudio")
public final class DecanoPlanEstudioController {

    private final ConsultarAsignaturasPlanInputPort consultarAsignaturasPlanInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DecanoPlanEstudioController(final ConsultarAsignaturasPlanInputPort consultarAsignaturasPlanInputPort,
                                       final AuthenticatedUserResolver authenticatedUserResolver) {
        this.consultarAsignaturasPlanInputPort = Objects.requireNonNull(consultarAsignaturasPlanInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @GetMapping("/{planId}/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDTO>> consultarAsignaturasPlan(@PathVariable("planId") final UUID planId) {
        final List<AsignaturaDTO> asignaturas = consultarAsignaturasPlanInputPort.execute(
                authenticatedUserResolver.requireAuthenticatedUserId(), planId);
        return ResponseEntity.ok(new ApiListResponse<>(true, asignaturas, asignaturas.size()));
    }
}
