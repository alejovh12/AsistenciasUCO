package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.ResolverSolicitudIngresoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResolverSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResultadoSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverSolicitudIngresoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.SolicitudIngresoDocenteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * HU053/HU054/HU055: el docente titular consulta y resuelve las solicitudes de ingreso a sus grupos. La titularidad la
 * revalida la DB; una solicitud de otro grupo responde 404.
 */
@RestController
@RequestMapping("/api/v1/docente")
public final class DocenteSolicitudIngresoController {

    private final ConsultarSolicitudesIngresoDocenteInputPort consultarInputPort;
    private final ResolverSolicitudIngresoInputPort resolverInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteSolicitudIngresoController(
            final ConsultarSolicitudesIngresoDocenteInputPort consultarInputPort,
            final ResolverSolicitudIngresoInputPort resolverInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort, "ConsultarSolicitudesIngresoDocenteInputPort es obligatorio.");
        this.resolverInputPort = Objects.requireNonNull(resolverInputPort, "ResolverSolicitudIngresoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    /** Bandeja del grupo en orden de llegada, con cupo disponible y posicion en la lista de espera. */
    @GetMapping("/grupos/{grupoId}/solicitudes-ingreso")
    public ResponseEntity<ApiListResponse<SolicitudIngresoDocenteResponse>> consultarSolicitudes(
            @PathVariable final UUID grupoId,
            @RequestParam(name = "estado", required = false) final String estado,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final SolicitudesIngresoDocentePaginaDTO resultado = consultarInputPort.execute(new ConsultarSolicitudesIngresoDocenteDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(), grupoId, estado, pagina, tamano));
        return ResponseEntity.ok(new ApiListResponse<>(
                true,
                resultado.solicitudes().stream().map(DocenteSolicitudIngresoController::toResponse).toList(),
                resultado.totalRegistros()));
    }

    /** ACEPTAR (con cupo matricula; sin cupo queda EN_ESPERA) o RECHAZAR con motivo obligatorio. */
    @PatchMapping("/solicitudes-ingreso/{id}")
    public ResponseEntity<ApiDataResponse<ResultadoSolicitudIngresoDTO>> resolverSolicitud(
            @PathVariable final UUID id,
            @RequestBody(required = false) final ResolverSolicitudIngresoRequest request
    ) {
        final ResultadoSolicitudIngresoDTO resultado = resolverInputPort.execute(new ResolverSolicitudIngresoDTO(
                authenticatedUserResolver.requireAuthenticatedUserId(),
                id,
                request == null ? null : request.getAccion(),
                request == null ? null : request.getMotivo()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, resultado));
    }

    private static SolicitudIngresoDocenteResponse toResponse(final SolicitudIngresoDocenteDTO dto) {
        return new SolicitudIngresoDocenteResponse(dto.id(), dto.estudianteId(), dto.estudianteNombre(),
                dto.numeroIdentificacion(), dto.estado(), utc(dto.fechaSolicitudUtc()), utc(dto.fechaResolucionUtc()),
                dto.motivoRechazo(), dto.posicionEspera(), dto.capacidadGrupo(), dto.cuposDisponibles());
    }

    /** Instant UTC -> {@code yyyy-MM-ddTHH:mm:ss[.fffffff]Z} sin pasar por la zona de la JVM. */
    static String utc(final Instant instant) {
        return instant == null
                ? null
                : HttpUtcInstantCodec.fromUtcDatabaseDateTime(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
