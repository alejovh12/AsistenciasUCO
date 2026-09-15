package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.mapper.ResolverSolicitudRevisionAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

public final class ResolverSolicitudRevisionAsistenciaUseCaseImpl implements ResolverSolicitudRevisionAsistenciaUseCase {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ResolverSolicitudRevisionAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final ResolverSolicitudRevisionAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para resolver solicitud de revision es obligatorio.");
        }
        final var docenteId = institutionalScopePort.findDocenteIdByUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        asistenciaRepositoryPort.resolverSolicitudRevisionAsistencia(
                ResolverSolicitudRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain, docenteId)
        );
    }
}
