package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.CerrarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.mapper.CerrarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso cerrar sesion.
 */
public final class CerrarSesionUseCaseImpl implements CerrarSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public CerrarSesionUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort,
                                   final InstitutionalScopePort institutionalScopePort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final CerrarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }
        final var docenteId = institutionalScopePort.findDocenteIdByUsuario(domain.getDocente())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        final var scopedDomain = new CerrarSesionDomain(domain.getSesion(), docenteId, domain.getObservacionCierre());
        sesionRepositoryPort.cerrarSesion(CerrarSesionRepositoryMapper.toRepositoryDTO(scopedDomain));
    }
}
