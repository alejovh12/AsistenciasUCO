package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.domain.DocenteCandidatoDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.ConsultarCandidatosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.mapper.ConsultarCandidatosDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarCandidatosDocenteUseCaseImpl implements ConsultarCandidatosDocenteUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaQueryPort queryPort;

    public ConsultarCandidatosDocenteUseCaseImpl(final InstitutionalScopePort scopePort, final DocenteProgramaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
    }

    @Override
    public List<DocenteCandidatoDomain> execute(final UUID actorUsuarioId, final String busqueda) {
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        return queryPort.consultarCandidatos(actorUsuarioId, idPrograma, busqueda).stream()
                .map(ConsultarCandidatosDocenteRepositoryMapper::toDomain).toList();
    }
}
