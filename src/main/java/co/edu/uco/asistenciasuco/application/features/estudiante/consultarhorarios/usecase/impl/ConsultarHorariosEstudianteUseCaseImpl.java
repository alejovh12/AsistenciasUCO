package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.mapper.ConsultarHorariosEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarHorariosEstudianteUseCaseImpl implements ConsultarHorariosEstudianteUseCase {
    private final InstitutionalScopePort scopePort; private final HorarioEstudianteQueryPort queryPort;
    public ConsultarHorariosEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final HorarioEstudianteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<HorarioEstudianteDomain> execute(final UUID actorUsuarioId) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        return queryPort.consultarHorarioEstudiante(idEstudiante).stream()
                .map(ConsultarHorariosEstudianteRepositoryMapper::toDomain)
                .toList();
    }
}
