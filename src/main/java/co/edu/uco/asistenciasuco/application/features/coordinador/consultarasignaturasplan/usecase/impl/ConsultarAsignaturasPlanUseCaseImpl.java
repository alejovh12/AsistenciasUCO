package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.ConsultarAsignaturasPlanUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.usecase.mapper.ConsultarAsignaturasPlanRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.Optional; import java.util.UUID;

public final class ConsultarAsignaturasPlanUseCaseImpl implements ConsultarAsignaturasPlanUseCase {
    private final InstitutionalScopePort scopePort; private final AsignaturaQueryPort queryPort;
    public ConsultarAsignaturasPlanUseCaseImpl(final InstitutionalScopePort scopePort, final AsignaturaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<AsignaturaDomain> execute(final UUID actorUsuarioId, final UUID idPlanEstudio) {
        // Coordinador: el plan debe ser de su programa. Decano (sin programa): de un programa de su facultad.
        final Optional<UUID> idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId);
        if (idPrograma.isEmpty()) {
            scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                    .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa ni la facultad del usuario autenticado."));
        }
        final List<AsignaturaDomain> asignaturas = queryPort.consultarAsignaturasPorPlan(idPlanEstudio).stream()
                .map(ConsultarAsignaturasPlanRepositoryMapper::toDomain).toList();
        final boolean fueraDeAmbito = idPrograma
                .map(programa -> asignaturas.stream().anyMatch(a -> !programa.equals(a.idPrograma())))
                .orElseGet(() -> asignaturas.stream()
                        .anyMatch(a -> !scopePort.canDecanoAccessPrograma(actorUsuarioId, a.idPrograma())));
        if (fueraDeAmbito) {
            throw new ForbiddenException("El plan de estudio solicitado no pertenece al ambito del usuario autenticado.");
        }
        return asignaturas;
    }
}
