package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.mapper.ConsultarCoordinadoresRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarCoordinadoresUseCaseImpl implements ConsultarCoordinadoresUseCase {
    private final InstitutionalScopePort scopePort;
    private final CoordinadorQueryPort coordinadorQueryPort;
    public ConsultarCoordinadoresUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorQueryPort coordinadorQueryPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.coordinadorQueryPort = Objects.requireNonNull(coordinadorQueryPort);
    }
    @Override public List<CoordinadorDomain> execute(final UUID actorUsuarioId) {
        final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));
        return coordinadorQueryPort.consultarCoordinadoresPorFacultad(idFacultad).stream()
                .map(ConsultarCoordinadoresRepositoryMapper::toDomain)
                .toList();
    }
}
