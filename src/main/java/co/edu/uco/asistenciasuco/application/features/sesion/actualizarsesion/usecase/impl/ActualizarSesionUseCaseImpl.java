package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.ActualizarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.mapper.ActualizarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

public final class ActualizarSesionUseCaseImpl implements ActualizarSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ActualizarSesionUseCaseImpl(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final ActualizarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para actualizar sesion es obligatorio.");
        }

        final var docenteId = institutionalScopePort.findDocenteIdByUsuario(domain.getDocente())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        final var scopedDomain = new ActualizarSesionDomain(
                domain.getSesion(),
                domain.getNombre(),
                domain.getFechaHoraInicio(),
                domain.getFechaHoraFin(),
                domain.getAula(),
                domain.getDescripcion(),
                docenteId
        );
        sesionRepositoryPort.actualizarSesion(ActualizarSesionRepositoryMapper.toRepositoryDTO(scopedDomain));
    }
}
