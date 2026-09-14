package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverReclamoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.AuthenticatedUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/docente")
public final class DocentePortalController {

    private final ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort;
    private final ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort;
    private final ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public DocentePortalController(
            final ConsultarHorariosDocenteInputPort consultarHorariosDocenteInputPort,
            final ConsultarAsignaturasDocenteInputPort consultarAsignaturasDocenteInputPort,
            final ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort,
            final AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.consultarHorariosDocenteInputPort = consultarHorariosDocenteInputPort;
        this.consultarAsignaturasDocenteInputPort = consultarAsignaturasDocenteInputPort;
        this.resolverSolicitudRevisionAsistenciaInputPort = resolverSolicitudRevisionAsistenciaInputPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/reclamos")
    public ResponseEntity<ApiListResponse<Void>> consultarReclamos() {
        throw unavailable("consultar reclamos");
    }

    @PatchMapping("/reclamos/{id}")
    public ResponseEntity<ApiDataResponse<Void>> resolverReclamo(
            @PathVariable final UUID id,
            @RequestBody final ResolverReclamoRequest request
    ) {
        resolverSolicitudRevisionAsistenciaInputPort.execute(new ResolverSolicitudRevisionAsistenciaDTO(
                id,
                request == null ? null : request.getAccion(),
                request == null ? null : request.getRespuestaDocente(),
                authenticatedUserProvider.requireAuthenticatedUserId()
        ));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    @GetMapping("/horarios")
    public ResponseEntity<ApiListResponse<HorarioDocenteDTO>> consultarHorarios() {
        final var data = consultarHorariosDocenteInputPort.execute(authenticatedUserProvider.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    @PatchMapping("/sesiones/{sesionId}/cancelar")
    public ResponseEntity<ApiDataResponse<Void>> cancelarSesion(@PathVariable final UUID sesionId) {
        throw unavailable("cancelar sesion");
    }

    @GetMapping("/asignaturas")
    public ResponseEntity<ApiListResponse<AsignaturaDocenteDTO>> consultarAsignaturas() {
        final var data = consultarAsignaturasDocenteInputPort.execute(authenticatedUserProvider.requireAuthenticatedUserId());
        return ResponseEntity.ok(new ApiListResponse<>(true, data, data.size()));
    }

    private FeatureUnavailableException unavailable(final String operation) {
        return new FeatureUnavailableException("La operacion docente no esta disponible: " + operation + ".");
    }
}
