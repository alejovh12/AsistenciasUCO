package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper.CrearSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso crear sesion.
 */
public final class CrearSesionUseCaseImpl implements CrearSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public CrearSesionUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort,
                                  final InstitutionalScopePort institutionalScopePort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final CrearSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }
        // Solo chequeo de rol docente: el docente resuelto no se persiste ni se propaga.
        institutionalScopePort.findDocenteIdByUsuario(domain.getUsuarioEjecutor())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        sesionRepositoryPort.crearSesion(CrearSesionRepositoryMapper.toRepositoryDTO(domain));
    }
}
