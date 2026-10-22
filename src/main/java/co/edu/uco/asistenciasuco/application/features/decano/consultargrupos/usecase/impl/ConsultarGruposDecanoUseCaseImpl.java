package co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.ConsultarGruposDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.mapper.ConsultarGruposRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoFacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Grupos de la facultad del decano autenticado. El ambito se fuerza aqui porque el backend no consulta las vistas
 * {@code uv_auth_*}: el decano solo ve los grupos de los programas de su facultad.
 */
public final class ConsultarGruposDecanoUseCaseImpl implements ConsultarGruposDecanoUseCase {

    private final InstitutionalScopePort scopePort;
    private final GrupoFacultadQueryPort queryPort;

    public ConsultarGruposDecanoUseCaseImpl(final InstitutionalScopePort scopePort, final GrupoFacultadQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "GrupoFacultadQueryPort es obligatorio.");
    }

    @Override
    public List<GrupoEntity> execute(final UUID actorUsuarioId) {
        if (ObjectHelper.isNull(actorUsuarioId)) {
            throw new CrosscuttingException("El usuario autenticado es obligatorio para consultar los grupos del decano.");
        }
        final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));
        return ConsultarGruposRepositoryMapper.toUseCaseEntities(queryPort.consultarGruposPorFacultad(idFacultad));
    }
}
