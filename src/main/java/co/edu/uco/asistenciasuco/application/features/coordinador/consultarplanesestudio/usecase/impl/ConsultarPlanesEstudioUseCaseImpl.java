package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.ConsultarPlanesEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.mapper.ConsultarPlanesEstudioRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarPlanesEstudioUseCaseImpl implements ConsultarPlanesEstudioUseCase {
    private final InstitutionalScopePort scopePort; private final PlanEstudioQueryPort queryPort;
    public ConsultarPlanesEstudioUseCaseImpl(final InstitutionalScopePort scopePort, final PlanEstudioQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<PlanEstudioDomain> execute(final UUID actorUsuarioId) {
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        return queryPort.consultarPlanesPorPrograma(idPrograma).stream().map(ConsultarPlanesEstudioRepositoryMapper::toDomain).toList();
    }
}
