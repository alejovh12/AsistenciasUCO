package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain.AsignaturaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.mapper.ConsultarAsignaturasDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarAsignaturasDocenteUseCaseImpl implements ConsultarAsignaturasDocenteUseCase {
    private final InstitutionalScopePort scopePort; private final AsignaturaDocenteQueryPort queryPort;
    public ConsultarAsignaturasDocenteUseCaseImpl(final InstitutionalScopePort scopePort, final AsignaturaDocenteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<AsignaturaDocenteDomain> execute(final UUID actorUsuarioId) {
        final UUID idDocente = scopePort.findDocenteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        return queryPort.consultarAsignaturasDocente(idDocente).stream()
                .map(ConsultarAsignaturasDocenteRepositoryMapper::toDomain)
                .toList();
    }
}
