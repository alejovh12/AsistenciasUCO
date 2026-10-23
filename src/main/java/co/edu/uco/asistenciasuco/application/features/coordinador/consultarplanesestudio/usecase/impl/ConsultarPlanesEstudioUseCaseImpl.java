package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.ConsultarPlanesEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.mapper.ConsultarPlanesEstudioRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.Optional; import java.util.UUID;

public final class ConsultarPlanesEstudioUseCaseImpl implements ConsultarPlanesEstudioUseCase {
    private final InstitutionalScopePort scopePort; private final PlanEstudioQueryPort queryPort;
    public ConsultarPlanesEstudioUseCaseImpl(final InstitutionalScopePort scopePort, final PlanEstudioQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<PlanEstudioDomain> execute(final UUID actorUsuarioId) {
        // Coordinador: planes de su programa. Decano (sin programa): planes de los programas de su facultad.
        final Optional<UUID> idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId);
        final List<PlanEstudioProjection> planes;
        if (idPrograma.isPresent()) {
            planes = queryPort.consultarPlanesPorPrograma(idPrograma.get());
        } else {
            final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                    .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa ni la facultad del usuario autenticado."));
            planes = queryPort.consultarPlanesPorFacultad(idFacultad);
        }
        return planes.stream().map(ConsultarPlanesEstudioRepositoryMapper::toDomain).toList();
    }
}
