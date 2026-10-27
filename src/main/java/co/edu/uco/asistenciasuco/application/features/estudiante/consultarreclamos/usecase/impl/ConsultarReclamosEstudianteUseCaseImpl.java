package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.ConsultarReclamosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ConsultarReclamosEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamosEstudiantePaginaDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.mapper.ConsultarReclamosEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;

/**
 * Lista las solicitudes de revision del estudiante autenticado (HU010/HU011). El rol ESTUDIANTE no basta:
 * Application exige un estudiante institucional y SQL filtra ademas por ownership ({@code Estudiante.usuario}),
 * de modo que nadie recibe solicitudes ajenas.
 */
public final class ConsultarReclamosEstudianteUseCaseImpl implements ConsultarReclamosEstudianteUseCase {

    private final InstitutionalScopePort scopePort;
    private final SolicitudRevisionEstudianteQueryPort queryPort;

    public ConsultarReclamosEstudianteUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionEstudianteQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "SolicitudRevisionEstudianteQueryPort es obligatorio.");
    }

    @Override
    public ReclamosEstudiantePaginaDomain execute(final ConsultarReclamosEstudianteDomain domain) {
        scopePort.findEstudianteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        final List<SolicitudRevisionEstudianteProjection> filas = queryPort.consultarSolicitudesEstudiante(
                domain.actorUsuarioId(), domain.codigoEstado(), domain.pagina(), domain.tamano());
        final int total = filas.isEmpty() ? 0 : filas.getFirst().totalRegistros();
        return new ReclamosEstudiantePaginaDomain(
                filas.stream().map(ConsultarReclamosEstudianteRepositoryMapper::toDomain).toList(), total);
    }
}
