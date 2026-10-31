package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.domain.GenerarReporteAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.mapper.ReporteAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import java.util.List; import java.util.Objects; import java.util.UUID;
import java.util.Set;

public final class GenerarReporteAsistenciaUseCaseImpl implements GenerarReporteAsistenciaUseCase {
    private final ReporteAsistenciaQueryPort queryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public GenerarReporteAsistenciaUseCaseImpl(
            final ReporteAsistenciaQueryPort queryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.queryPort = Objects.requireNonNull(queryPort, "ReporteAsistenciaQueryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override public List<ReporteAsistenciaEntity> execute(final GenerarReporteAsistenciaDomain domain) {
        validarAutorizacion(domain);
        return queryPort.consultarReporteAsistenciaGrupo(domain.grupoId()).stream()
                .map(ReporteAsistenciaRepositoryMapper::toEntity)
                .toList();
    }

    private void validarAutorizacion(final GenerarReporteAsistenciaDomain domain) {
        final Set<InstitutionalRole> activeRoles = institutionalScopePort.findRolesActivosByUsuario(domain.usuarioEjecutor());
        if (activeRoles == null || activeRoles.isEmpty()) {
            throw accesoDenegado();
        }

        if (activeRoles.contains(InstitutionalRole.ADMINISTRADOR)
                && institutionalScopePort.isAdministradorActivo(domain.usuarioEjecutor())) {
            return;
        }

        if (activeRoles.contains(InstitutionalRole.COORDINADOR)
                && institutionalScopePort.canCoordinadorAccessGrupo(domain.usuarioEjecutor(), domain.grupoId())) {
            return;
        }

        if (activeRoles.contains(InstitutionalRole.DOCENTE)
                && institutionalScopePort.findDocenteIdByUsuario(domain.usuarioEjecutor()).isPresent()
                && institutionalScopePort.canDocenteAccessGrupo(domain.usuarioEjecutor(), domain.grupoId())) {
            return;
        }

        throw accesoDenegado();
    }

    private static ForbiddenException accesoDenegado() {
        return new ForbiddenException("El usuario autenticado no tiene autorizacion institucional para consultar el reporte del grupo.");
    }
}
