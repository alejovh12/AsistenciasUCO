package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.mapper.ConsultarHorariosDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarHorariosDocenteUseCaseImpl implements ConsultarHorariosDocenteUseCase {
    private final InstitutionalScopePort scopePort; private final HorarioDocenteQueryPort queryPort;
    public ConsultarHorariosDocenteUseCaseImpl(final InstitutionalScopePort scopePort, final HorarioDocenteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<HorarioDocenteDomain> execute(final UUID actorUsuarioId) {
        final UUID idDocente = scopePort.findDocenteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        return queryPort.consultarHorarioDocente(idDocente).stream()
                .map(ConsultarHorariosDocenteRepositoryMapper::toDomain)
                .toList();
    }
}
