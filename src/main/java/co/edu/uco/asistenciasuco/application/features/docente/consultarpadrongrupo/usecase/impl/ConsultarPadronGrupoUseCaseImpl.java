package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.ConsultarPadronGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.ConsultarPadronGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.PadronGrupoPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.mapper.ConsultarPadronGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.PadronGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.PadronGrupoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/**
 * Padron paginado del grupo del docente titular. Application exige que el usuario sea un docente institucional titular del
 * grupo (defensa adicional, como {@code ConsultarEstudiantesGrupoUseCaseImpl}); el SP valida ademas el ambito antes de leer
 * filas y resuelve busqueda, orden y paginacion.
 */
public final class ConsultarPadronGrupoUseCaseImpl implements ConsultarPadronGrupoUseCase {

    private final InstitutionalScopePort scopePort;
    private final PadronGrupoQueryPort queryPort;

    public ConsultarPadronGrupoUseCaseImpl(final InstitutionalScopePort scopePort, final PadronGrupoQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "PadronGrupoQueryPort es obligatorio.");
    }

    @Override
    public PadronGrupoPaginaDomain execute(final ConsultarPadronGrupoDomain domain) {
        if (scopePort.findDocenteIdByUsuario(domain.usuarioEjecutor()).isEmpty()
                || !scopePort.canDocenteAccessGrupo(domain.usuarioEjecutor(), domain.grupoId())) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_001);
        }
        final PadronGrupoPaginaProjection pagina = queryPort.consultarPadronPagina(
                domain.grupoId(), domain.usuarioEjecutor(), domain.pagina(), domain.tamano(), domain.busqueda());
        return new PadronGrupoPaginaDomain(
                pagina.estudiantes().stream().map(ConsultarPadronGrupoRepositoryMapper::toDomain).toList(),
                domain.pagina(), domain.tamano(), pagina.totalRegistros());
    }
}
