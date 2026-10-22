package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.ConsultarAsignaturasUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.usecase.mapper.ConsultarAsignaturasRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.Optional; import java.util.UUID;

public final class ConsultarAsignaturasUseCaseImpl implements ConsultarAsignaturasUseCase {
    private final InstitutionalScopePort scopePort; private final AsignaturaQueryPort queryPort;
    public ConsultarAsignaturasUseCaseImpl(final InstitutionalScopePort scopePort, final AsignaturaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<AsignaturaDomain> execute(final UUID actorUsuarioId) {
        // Coordinador: asignaturas de su programa. Decano (sin programa): las de los programas de su facultad.
        final Optional<UUID> idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId);
        final List<AsignaturaProjection> asignaturas;
        if (idPrograma.isPresent()) {
            asignaturas = queryPort.consultarAsignaturasPorPrograma(idPrograma.get());
        } else {
            final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                    .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa ni la facultad del usuario autenticado."));
            asignaturas = queryPort.consultarAsignaturasPorFacultad(idFacultad);
        }
        return asignaturas.stream().map(ConsultarAsignaturasRepositoryMapper::toDomain).toList();
    }
}
