package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.SolicitudIngresoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.SolicitudesIngresoDocentePaginaDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.ConsultarSolicitudesIngresoDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.ConsultarSolicitudesIngresoDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** HU053/HU055: bandeja de solicitudes de un grupo del docente titular (la titularidad la filtra el SP). */
public final class ConsultarSolicitudesIngresoDocenteUseCaseImpl implements ConsultarSolicitudesIngresoDocenteUseCase {

    private static final Map<String, String> ESTADO_LEGIBLE = Map.of(
            "P", "PENDIENTE", "A", "ACEPTADA", "R", "RECHAZADA", "E", "EN_ESPERA", "X", "RETIRADA");

    private final InstitutionalScopePort scopePort;
    private final SolicitudIngresoGrupoPort port;

    public ConsultarSolicitudesIngresoDocenteUseCaseImpl(final InstitutionalScopePort scopePort, final SolicitudIngresoGrupoPort port) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "SolicitudIngresoGrupoPort es obligatorio.");
    }

    @Override
    public SolicitudesIngresoDocentePaginaDomain execute(final ConsultarSolicitudesIngresoDocenteDomain domain) {
        scopePort.findDocenteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));

        final List<SolicitudIngresoDocenteProjection> filas = port.consultarSolicitudesGrupo(
                domain.grupoId(), domain.codigoEstado(), domain.pagina(), domain.tamano(), domain.actorUsuarioId());
        final int total = filas.isEmpty() ? 0 : filas.getFirst().totalRegistros();
        return new SolicitudesIngresoDocentePaginaDomain(filas.stream().map(fila -> new SolicitudIngresoDocenteDomain(
                fila.idSolicitud(), fila.idEstudiante(), fila.nombreEstudiante(), fila.numeroIdentificacion(),
                ESTADO_LEGIBLE.getOrDefault(fila.codigoEstado(), fila.codigoEstado()), fila.fechaSolicitudUtc(),
                fila.fechaResolucionUtc(), fila.motivoRechazo(), fila.posicionEspera(), fila.capacidadGrupo(),
                fila.cuposDisponibles())).toList(), total);
    }
}
