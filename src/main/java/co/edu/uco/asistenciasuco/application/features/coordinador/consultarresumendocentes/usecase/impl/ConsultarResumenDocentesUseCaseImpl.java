package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.ConsultarResumenDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenDocentesProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;
import java.util.UUID;

/**
 * HU095: consolidado del directorio docente del programa del coordinador. La DB cuenta activos e inactivos (mismo criterio
 * que el directorio); MEJORAS-SERGIO-2026-11-18 retiro el desglose por tipo de vinculacion.
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
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_006));
        final ResumenDocentesProgramaProjection resumen = Objects.requireNonNullElseGet(
                queryPort.consultarResumen(actorUsuarioId), ResumenDocentesProgramaProjection::vacio);
        return new ResumenDocentesProgramaDomain(resumen.totalActivos() + resumen.totalInactivos(),
                resumen.totalActivos(), resumen.totalInactivos());
    }
}
