package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper.ConsultarSesionesMateriaEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarSesionesMateriaEstudianteUseCaseImpl implements ConsultarSesionesMateriaEstudianteUseCase {
    private final InstitutionalScopePort scopePort; private final SesionMateriaEstudianteQueryPort queryPort;
    public ConsultarSesionesMateriaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final SesionMateriaEstudianteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<SesionMateriaEstudianteDomain> execute(final UUID actorUsuarioId, final UUID idAsignatura) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        return queryPort.consultarSesionesMateria(idEstudiante, idAsignatura).stream()
                .map(ConsultarSesionesMateriaEstudianteRepositoryMapper::toDomain)
                .toList();
    }
}
