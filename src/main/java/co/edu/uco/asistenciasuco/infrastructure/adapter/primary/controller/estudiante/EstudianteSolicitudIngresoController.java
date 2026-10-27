package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.RetirarSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto.RetirarSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.SolicitarIngresoGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto.SolicitarIngresoGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request.SolicitarIngresoGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response.SolicitudIngresoCreadaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response.SolicitudIngresoEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * HU053/HU055: el estudiante solicita el ingreso a un grupo, consulta sus solicitudes y retira las abiertas. El
 * ownership lo revalida la DB; una solicitud ajena responde 404.
 */
@RestController
@RequestMapping("/api/v1/estudiante/solicitudes-matricula")
public final class EstudianteSolicitudIngresoController {

    private final SolicitarIngresoGrupoInputPort solicitarInputPort;
    private final ConsultarSolicitudesIngresoEstudianteInputPort consultarInputPort;
    private final RetirarSolicitudIngresoInputPort retirarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteSolicitudIngresoController(
            final SolicitarIngresoGrupoInputPort solicitarInputPort,
            final ConsultarSolicitudesIngresoEstudianteInputPort consultarInputPort,
            final RetirarSolicitudIngresoInputPort retirarInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.solicitarInputPort = Objects.requireNonNull(solicitarInputPort, "SolicitarIngresoGrupoInputPort es obligatorio.");
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort, "ConsultarSolicitudesIngresoEstudianteInputPort es obligatorio.");
        this.retirarInputPort = Objects.requireNonNull(retirarInputPort, "RetirarSolicitudIngresoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<SolicitudIngresoCreadaResponse>> solicitar(
            @RequestBody(required = false) final SolicitarIngresoGrupoRequest request
    ) {
        final UUID id = solicitarInputPort.execute(new SolicitarIngresoGrupoDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), request == null ? null : request.getGrupoId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, new SolicitudIngresoCreadaResponse(id)));
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<SolicitudIngresoEstudianteResponse>> consultar(
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final SolicitudesIngresoEstudiantePaginaDTO resultado = consultarInputPort.execute(new ConsultarSolicitudesIngresoEstudianteDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), estado, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(
                true,
                resultado.solicitudes().stream().map(EstudianteSolicitudIngresoController::toResponse).toList(),
                resultado.totalRegistros()));
    }

    @PostMapping("/{id}/retiro")
    public ResponseEntity<ApiDataResponse<Void>> retirar(@PathVariable final UUID id) {
        retirarInputPort.execute(new RetirarSolicitudIngresoDTO(authenticatedUserResolver.requireAuthenticatedUserId(), id));
        return ResponseEntity.ok(new ApiDataResponse<>(true, null));
    }

    private static SolicitudIngresoEstudianteResponse toResponse(final SolicitudIngresoEstudianteDTO dto) {
        return new SolicitudIngresoEstudianteResponse(dto.id(), dto.grupoId(), dto.grupoCodigo(), dto.grupoNombre(),
                dto.asignaturaId(), dto.asignaturaNombre(), dto.docenteNombre(), dto.estado(), utc(dto.fechaSolicitudUtc()),
                utc(dto.fechaResolucionUtc()), dto.motivoRechazo());
    }

    private static String utc(final Instant instant) {
        return instant == null
                ? null
                : HttpUtcInstantCodec.fromUtcDatabaseDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
