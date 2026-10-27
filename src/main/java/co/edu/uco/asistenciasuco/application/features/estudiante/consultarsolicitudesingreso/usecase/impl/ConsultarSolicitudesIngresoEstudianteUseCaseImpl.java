package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.ConsultarSolicitudesIngresoEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain.ConsultarSolicitudesIngresoEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain.SolicitudIngresoEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain.SolicitudesIngresoEstudiantePaginaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** HU053/HU055: solicitudes de ingreso propias del estudiante autenticado (el ownership lo filtra el SP). */
public final class ConsultarSolicitudesIngresoEstudianteUseCaseImpl implements ConsultarSolicitudesIngresoEstudianteUseCase {

    private static final Map<String, String> ESTADO_LEGIBLE = Map.of(
            "P", "PENDIENTE", "A", "ACEPTADA", "R", "RECHAZADA", "E", "EN_ESPERA", "X", "RETIRADA");

    private final InstitutionalScopePort scopePort;
    private final SolicitudIngresoGrupoPort port;

    public ConsultarSolicitudesIngresoEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final SolicitudIngresoGrupoPort port) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.port = Objects.requireNonNull(port, "SolicitudIngresoGrupoPort es obligatorio.");
    }

    @Override
    public SolicitudesIngresoEstudiantePaginaDomain execute(final ConsultarSolicitudesIngresoEstudianteDomain domain) {
        scopePort.findEstudianteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        final List<SolicitudIngresoEstudianteProjection> filas = port.consultarSolicitudesEstudiante(
                domain.codigoEstado(), domain.pagina(), domain.tamano(), domain.actorUsuarioId());
        final int total = filas.isEmpty() ? 0 : filas.getFirst().totalRegistros();
        return new SolicitudesIngresoEstudiantePaginaDomain(filas.stream().map(fila -> new SolicitudIngresoEstudianteDomain(
                fila.idSolicitud(), fila.idGrupo(), fila.codigoGrupo(), fila.nombreGrupo(), fila.idAsignatura(),
                fila.nombreAsignatura(), fila.nombreDocente(),
                ESTADO_LEGIBLE.getOrDefault(fila.codigoEstado(), fila.codigoEstado()), fila.fechaSolicitudUtc(),
                fila.fechaResolucionUtc(), fila.motivoRechazo())).toList(), total);
    }
}
