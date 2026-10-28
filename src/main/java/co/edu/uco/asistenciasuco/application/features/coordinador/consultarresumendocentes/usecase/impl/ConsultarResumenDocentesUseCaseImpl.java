package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.ConsultarResumenDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenTipoVinculacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU095: consolidado del directorio docente del programa del coordinador. La DB cuenta activos e inactivos por tipo
 * de vinculacion (mismo criterio que el directorio); aqui solo se agregan los totales.
 */
public final class ConsultarResumenDocentesUseCaseImpl implements ConsultarResumenDocentesUseCase {

    private final InstitutionalScopePort scopePort;
    private final ResumenDocentesProgramaQueryPort queryPort;

    public ConsultarResumenDocentesUseCaseImpl(final InstitutionalScopePort scopePort, final ResumenDocentesProgramaQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ResumenDocentesProgramaQueryPort es obligatorio.");
    }

    @Override
    public ResumenDocentesProgramaDomain execute(final UUID actorUsuarioId) {
        scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        final List<ResumenTipoVinculacionDomain> porTipo = queryPort.consultarResumen(actorUsuarioId).stream()
                .map(r -> new ResumenTipoVinculacionDomain(r.idTipoVinculacion(), r.codigoTipoVinculacion(),
                        r.nombreTipoVinculacion(), r.totalActivos(), r.totalInactivos()))
                .toList();
        final int activos = porTipo.stream().mapToInt(ResumenTipoVinculacionDomain::totalActivos).sum();
        final int inactivos = porTipo.stream().mapToInt(ResumenTipoVinculacionDomain::totalInactivos).sum();
        return new ResumenDocentesProgramaDomain(activos + inactivos, activos, inactivos, porTipo);
    }
}
